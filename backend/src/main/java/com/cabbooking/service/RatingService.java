package com.cabbooking.service;

import com.cabbooking.dto.MiscDtos.RatingRequest;
import com.cabbooking.dto.MiscDtos.RatingResponse;
import com.cabbooking.entity.Driver;
import com.cabbooking.entity.Payment;
import com.cabbooking.entity.Rating;
import com.cabbooking.entity.Ride;
import com.cabbooking.repository.DriverRepository;
import com.cabbooking.repository.PaymentRepository;
import com.cabbooking.repository.RatingRepository;
import com.cabbooking.repository.RideRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** One rating per completed + paid ride; keeps the driver's running average. */
@Service
public class RatingService {

    private final RatingRepository ratings;
    private final RideRepository rides;
    private final PaymentRepository payments;
    private final DriverRepository drivers;

    public RatingService(RatingRepository ratings, RideRepository rides,
                         PaymentRepository payments, DriverRepository drivers) {
        this.ratings = ratings;
        this.rides = rides;
        this.payments = payments;
        this.drivers = drivers;
    }

    @Transactional
    public Rating rate(Long riderId, Long rideId, RatingRequest req) {
        Ride ride = rides.findById(rideId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));
        if (!ride.getRider().getId().equals(riderId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your ride");
        }
        if (ride.getStatus() != Ride.Status.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only completed rides can be rated");
        }
        Payment payment = payments.findByRideId(rideId).orElse(null);
        if (payment == null || payment.getStatus() != Payment.Status.SUCCEEDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ride must be paid before rating");
        }
        if (ratings.findByRideId(rideId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ride already rated");
        }
        Rating r = new Rating();
        r.setRide(ride);
        r.setDriver(ride.getDriver());
        r.setRider(ride.getRider());
        r.setStars(req.stars());
        r.setComment(req.comment());
        ratings.save(r);

        // Update the driver's running average.
        Driver d = drivers.findByUserId(ride.getDriver().getId()).orElseThrow();
        double total = d.getRatingAvg() * d.getRatingCount() + req.stars();
        d.setRatingCount(d.getRatingCount() + 1);
        d.setRatingAvg(Math.round(total / d.getRatingCount() * 100.0) / 100.0);
        drivers.save(d);
        return r;
    }

    @Transactional(readOnly = true)
    public List<RatingResponse> forDriver(Long driverUserId) {
        return ratings.findByDriverIdOrderByCreatedAtDesc(driverUserId).stream()
                .map(r -> new RatingResponse(r.getId(), r.getStars(), r.getComment(),
                        r.getRider().getName(), r.getCreatedAt().toString()))
                .toList();
    }
}
