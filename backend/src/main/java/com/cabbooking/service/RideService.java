package com.cabbooking.service;

import com.cabbooking.dto.RideDtos.*;
import com.cabbooking.entity.Driver;
import com.cabbooking.entity.PromoCode;
import com.cabbooking.entity.Ride;
import com.cabbooking.entity.User;
import com.cabbooking.repository.DriverRepository;
import com.cabbooking.repository.PromoCodeRepository;
import com.cabbooking.repository.RideRepository;
import com.cabbooking.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * The booking state machine. All transitions are validated here so the
 * database can never hold an illegal ride state or a double assignment.
 */
@Service
public class RideService {

    private final RideRepository rides;
    private final UserRepository users;
    private final DriverRepository drivers;
    private final PromoCodeRepository promos;
    private final FareService fares;
    private final RideEventPublisher events;
    private final PaymentService payments;
    private final ApplicationEventPublisher applicationEvents;

    /** Allowed next states for each current state. */
    private static final Map<Ride.Status, List<Ride.Status>> TRANSITIONS = Map.of(
            Ride.Status.REQUESTED, List.of(Ride.Status.ACCEPTED, Ride.Status.CANCELLED),
            Ride.Status.ACCEPTED, List.of(Ride.Status.DRIVER_ARRIVING, Ride.Status.CANCELLED),
            Ride.Status.DRIVER_ARRIVING, List.of(Ride.Status.IN_PROGRESS, Ride.Status.CANCELLED),
            Ride.Status.IN_PROGRESS, List.of(Ride.Status.COMPLETED),
            Ride.Status.COMPLETED, List.of(),
            Ride.Status.CANCELLED, List.of());

    public RideService(RideRepository rides, UserRepository users, DriverRepository drivers,
                       PromoCodeRepository promos, FareService fares,
                       RideEventPublisher events, PaymentService payments,
                       ApplicationEventPublisher applicationEvents) {
        this.rides = rides;
        this.users = users;
        this.drivers = drivers;
        this.promos = promos;
        this.fares = fares;
        this.events = events;
        this.payments = payments;
        this.applicationEvents = applicationEvents;
    }

    /**
     * Queue a WebSocket push to be sent after the current transaction commits,
     * so clients never refetch before the change is saved.
     */
    private void pushAfterCommit(Long rideId, String type, Map<String, Object> data) {
        applicationEvents.publishEvent(new RideWsEvent(rideId, type, data));
    }

    // ------------------------------ rider ops ------------------------------

    public FareEstimateResponse estimate(CreateRideRequest req) {
        double km = fares.haversineKm(req.pickupLat(), req.pickupLng(), req.dropoffLat(), req.dropoffLng());
        return new FareEstimateResponse(round2(km), fares.estimate(km), fares.etaMinutes(km));
    }

    @Transactional
    public RideResponse createRide(Long riderId, CreateRideRequest req) {
        validateCoords(req);
        // One active ride per rider at a time.
        boolean hasActive = rides.findByRiderIdOrderByCreatedAtDesc(riderId).stream()
                .anyMatch(r -> isActive(r.getStatus()));
        if (hasActive) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have an active ride");
        }
        User rider = users.findById(riderId).orElseThrow();
        double km = fares.haversineKm(req.pickupLat(), req.pickupLng(), req.dropoffLat(), req.dropoffLng());
        double fare = fares.estimate(km);

        String promoCode = null;
        if (req.promoCode() != null && !req.promoCode().isBlank()) {
            PromoCode promo = validPromo(req.promoCode());
            promoCode = promo.getCode();
            fare = round2(fare - fares.discountFor(fare, promo.getPercentOff(), promo.getMaxDiscount()));
        }

        Ride r = new Ride();
        r.setRider(rider);
        r.setPickupLat(req.pickupLat()); r.setPickupLng(req.pickupLng());
        r.setDropoffLat(req.dropoffLat()); r.setDropoffLng(req.dropoffLng());
        r.setPickupLabel(req.pickupLabel()); r.setDropoffLabel(req.dropoffLabel());
        r.setDistanceKm(round2(km));
        r.setEstimatedFare(fare);
        r.setPromoCode(promoCode);
        r.setStatus(Ride.Status.REQUESTED);
        rides.save(r);

        pushAfterCommit(0L, "NEW_REQUEST", Map.of("rideId", r.getId(), "pickupLabel", nz(req.pickupLabel()),
                "dropoffLabel", nz(req.dropoffLabel()), "fare", fare));
        return toResponse(r);
    }

    @Transactional
    public RideResponse cancelRide(Long riderId, Long rideId) {
        Ride r = getRiderRide(riderId, rideId);
        if (r.getStatus() != Ride.Status.REQUESTED && r.getStatus() != Ride.Status.ACCEPTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ride can no longer be cancelled");
        }
        r.setStatus(Ride.Status.CANCELLED);
        pushAfterCommit(rideId, "STATUS", Map.of("status", "CANCELLED"));
        return toResponse(r);
    }

    @Transactional
    public RideResponse sos(Long riderId, Long rideId, String note) {
        Ride r = getRiderRide(riderId, rideId);
        if (!isActive(r.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "SOS is only available during an active ride");
        }
        r.setSosNote(note == null ? "SOS alert" : note);
        pushAfterCommit(rideId, "SOS", Map.of("note", r.getSosNote(), "at", Instant.now().toString()));
        return toResponse(r);
    }

    @Transactional(readOnly = true)
    public List<RideResponse> myRides(Long riderId) {
        return rides.findByRiderIdOrderByCreatedAtDesc(riderId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RideResponse getRide(Long riderId, Long rideId) {
        return toResponse(getRiderRide(riderId, rideId));
    }

    // ------------------------------ driver ops ------------------------------

    /** Rides waiting for a driver, newest first. */
    @Transactional(readOnly = true)
    public List<RideResponse> pendingRequests() {
        return rides.findByStatusOrderByCreatedAtDesc(Ride.Status.REQUESTED).stream()
                .map(this::toResponse).toList();
    }

    /**
     * Atomically assigns the ride to one driver. The pessimistic lock plus the
     * status check guarantees two drivers can never accept the same ride.
     */
    @Transactional
    public RideResponse acceptRide(Long driverUserId, Long rideId) {
        Driver driver = drivers.findByUserId(driverUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Driver profile missing"));
        if (!driver.isOnline()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Go online before accepting rides");
        }
        if (rides.findActiveByDriverId(driverUserId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Finish your current ride first");
        }
        Ride r = rides.findByIdForUpdate(rideId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));
        if (r.getStatus() != Ride.Status.REQUESTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ride is no longer available");
        }
        User driverUser = users.findById(driverUserId).orElseThrow();
        r.setDriver(driverUser);
        r.setStatus(Ride.Status.ACCEPTED);
        pushAfterCommit(rideId, "STATUS", Map.of("status", "ACCEPTED", "driver", driverPublic(driver)));
        return toResponse(r);
    }

    /** A rejection just notifies the rider; the ride stays open for other drivers. */
    public void rejectRide(Long driverUserId, Long rideId) {
        Ride r = rides.findById(rideId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));
        if (r.getStatus() != Ride.Status.REQUESTED) return;
        // No transaction and no database write here, so this can go out immediately.
        events.broadcast(rideId, "DRIVER_DECLINED", Map.of("at", Instant.now().toString()));
    }

    @Transactional
    public RideResponse updateStatus(Long driverUserId, Long rideId, String newStatus) {
        Ride r = rides.findById(rideId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));
        if (r.getDriver() == null || !r.getDriver().getId().equals(driverUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This ride is not assigned to you");
        }
        Ride.Status next;
        try {
            next = Ride.Status.valueOf(newStatus);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown status: " + newStatus);
        }
        if (!TRANSITIONS.get(r.getStatus()).contains(next)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot move ride from " + r.getStatus() + " to " + next);
        }
        r.setStatus(next);
        if (next == Ride.Status.COMPLETED) {
            r.setFinalFare(r.getEstimatedFare());
            r.setCompletedAt(Instant.now());
            payments.createPending(r); // creates the Stripe PaymentIntent record
        }
        pushAfterCommit(rideId, "STATUS", Map.of("status", next.name()));
        return toResponse(r);
    }

    @Transactional
    public void updateLocation(Long driverUserId, double lat, double lng) {
        Driver d = drivers.findByUserId(driverUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Driver profile missing"));
        d.setCurrentLat(lat);
        d.setCurrentLng(lng);
        d.setLocationUpdatedAt(Instant.now());
        // Push the live position to whoever is watching this driver's active ride.
        rides.findActiveByDriverId(driverUserId).ifPresent(r ->
                pushAfterCommit(r.getId(), "LOCATION", Map.of("lat", lat, "lng", lng)));
    }

    @Transactional
    public void setOnline(Long driverUserId, boolean online) {
        Driver d = drivers.findByUserId(driverUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Driver profile missing"));
        d.setOnline(online);
    }

    @Transactional(readOnly = true)
    public List<RideResponse> driverHistory(Long driverUserId) {
        return rides.findByDriverIdOrderByCreatedAtDesc(driverUserId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DriverProfileResponse driverProfile(Long driverUserId) {
        Driver d = drivers.findByUserId(driverUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Driver profile missing"));
        User u = d.getUser();
        return new DriverProfileResponse(u.getName(), u.getEmail(), u.getPhone(),
                d.getVehicleModel(), d.getVehiclePlate(), d.isOnline(),
                d.getRatingAvg(), d.getRatingCount(), d.getTotalEarnings(),
                d.getCurrentLat(), d.getCurrentLng());
    }

    @Transactional
    public DriverProfileResponse updateDriverProfile(Long driverUserId, String vehicleModel,
                                                     String vehiclePlate, String phone) {
        Driver d = drivers.findByUserId(driverUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Driver profile missing"));
        if (vehicleModel != null && !vehicleModel.isBlank()) d.setVehicleModel(vehicleModel);
        if (vehiclePlate != null && !vehiclePlate.isBlank()) d.setVehiclePlate(vehiclePlate);
        if (phone != null && !phone.isBlank()) d.getUser().setPhone(phone);
        return driverProfile(driverUserId);
    }

    // ------------------------------ helpers ------------------------------

    public PromoCode validPromo(String code) {
        PromoCode p = promos.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid promo code"));
        if (!p.isActive() || (p.getExpiresAt() != null && p.getExpiresAt().isBefore(Instant.now()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Promo code expired or inactive");
        }
        return p;
    }

    private Ride getRiderRide(Long riderId, Long rideId) {
        Ride r = rides.findById(rideId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));
        if (!r.getRider().getId().equals(riderId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your ride");
        }
        return r;
    }

    private void validateCoords(CreateRideRequest req) {
        for (double v : new double[]{req.pickupLat(), req.dropoffLat()}) {
            if (v < -90 || v > 90) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid latitude");
        }
        for (double v : new double[]{req.pickupLng(), req.dropoffLng()}) {
            if (v < -180 || v > 180) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid longitude");
        }
        if (fares.haversineKm(req.pickupLat(), req.pickupLng(), req.dropoffLat(), req.dropoffLng()) < 0.1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pickup and destination are too close");
        }
    }

    private boolean isActive(Ride.Status s) {
        return s == Ride.Status.REQUESTED || s == Ride.Status.ACCEPTED
                || s == Ride.Status.DRIVER_ARRIVING || s == Ride.Status.IN_PROGRESS;
    }

    private String nz(String s) { return s == null ? "" : s; }
    private double round2(double v) { return Math.round(v * 100.0) / 100.0; }

    /** Public driver details for the rider's "your driver" card. */
    private DriverInfo driverPublic(Driver d) {
        User u = d.getUser();
        return new DriverInfo(u.getName(), u.getPhone(), d.getVehicleModel(),
                d.getVehiclePlate(), d.getRatingAvg(), d.getCurrentLat(), d.getCurrentLng());
    }

    public RideResponse toResponse(Ride r) {
        DriverInfo info = null;
        if (r.getDriver() != null) {
            Driver d = drivers.findByUserId(r.getDriver().getId()).orElse(null);
            if (d != null) info = driverPublic(d);
        }
        return new RideResponse(
                r.getId(), r.getPickupLabel(), r.getDropoffLabel(),
                r.getPickupLat(), r.getPickupLng(), r.getDropoffLat(), r.getDropoffLng(),
                r.getDistanceKm(), r.getEstimatedFare(), r.getFinalFare(),
                r.getPromoCode(), r.getStatus().name(),
                r.getCreatedAt().toString(), info);
    }
}
