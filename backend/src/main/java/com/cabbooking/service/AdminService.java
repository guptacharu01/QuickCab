package com.cabbooking.service;

import com.cabbooking.entity.Payment;
import com.cabbooking.entity.Ride;
import com.cabbooking.entity.User;
import com.cabbooking.dto.RideDtos.RideResponse;
import com.cabbooking.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Read-only dashboards + account management for admins. */
@Service
public class AdminService {

    private final UserRepository users;
    private final DriverRepository drivers;
    private final RideRepository rides;
    private final PaymentRepository payments;
    private final RatingRepository ratings;
    private final RideService rideService;

    public AdminService(UserRepository users, DriverRepository drivers, RideRepository rides,
                        PaymentRepository payments, RatingRepository ratings, RideService rideService) {
        this.users = users;
        this.drivers = drivers;
        this.rides = rides;
        this.payments = payments;
        this.ratings = ratings;
        this.rideService = rideService;
    }

    public Map<String, Object> stats() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("totalUsers", users.count());
        s.put("totalRiders", users.countByRole(User.Role.RIDER));
        s.put("totalDrivers", users.countByRole(User.Role.DRIVER));
        s.put("onlineDrivers", drivers.findByOnlineTrue().size());
        s.put("totalRides", rides.count());
        s.put("completedRides", rides.countByStatus(Ride.Status.COMPLETED));
        s.put("cancelledRides", rides.countByStatus(Ride.Status.CANCELLED));
        s.put("activeRides", rides.count() - rides.countByStatus(Ride.Status.COMPLETED)
                - rides.countByStatus(Ride.Status.CANCELLED));
        s.put("totalRevenue", payments.findAll().stream()
                .filter(p -> p.getStatus() == Payment.Status.SUCCEEDED)
                .mapToDouble(Payment::getAmount).sum());
        s.put("totalRatings", ratings.count());
        return s;
    }

    public List<User> allUsers() {
        return users.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public List<Ride> allRides() {
        return rides.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    /** Ride DTOs mapped inside a transaction (lazy driver data loads safely). */
    @Transactional(readOnly = true)
    public List<RideResponse> allRideResponses() {
        return rides.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(rideService::toResponse).toList();
    }

    public List<Payment> allPayments() {
        return payments.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional
    public User setActive(Long id, boolean active) {
        User u = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (u.getRole() == User.Role.ADMIN && !active) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admins cannot be deactivated here");
        }
        u.setActive(active);
        return u;
    }
}
