package com.cabbooking.controller;

import com.cabbooking.dto.MiscDtos.RatingResponse;
import com.cabbooking.dto.RideDtos.*;
import com.cabbooking.service.RatingService;
import com.cabbooking.service.RideService;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Driver workspace: availability, requests, trip control, earnings, reviews. */
@RestController
@RequestMapping("/api/driver")
public class DriverController {

    private final RideService rides;
    private final RatingService ratings;

    public DriverController(RideService rides, RatingService ratings) {
        this.rides = rides;
        this.ratings = ratings;
    }

    private Long uid() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @GetMapping("/profile")
    public DriverProfileResponse profile() {
        return rides.driverProfile(uid());
    }

    @PutMapping("/profile")
    public DriverProfileResponse updateProfile(@Valid @RequestBody UpdateDriverProfileRequest req) {
        return rides.updateDriverProfile(uid(), req.vehicleModel(), req.vehiclePlate(), req.phone());
    }

    @PutMapping("/availability")
    public Map<String, Object> availability(@RequestBody AvailabilityRequest req) {
        rides.setOnline(uid(), req.online());
        return Map.of("online", req.online());
    }

    @GetMapping("/requests")
    public List<RideResponse> requests() {
        return rides.pendingRequests();
    }

    @PostMapping("/rides/{id}/accept")
    public RideResponse accept(@PathVariable("id") Long id) {
        return rides.acceptRide(uid(), id);
    }

    @PostMapping("/rides/{id}/reject")
    public Map<String, String> reject(@PathVariable("id") Long id) {
        rides.rejectRide(uid(), id);
        return Map.of("status", "rejected");
    }

    @PostMapping("/rides/{id}/status")
    public RideResponse status(@PathVariable("id") Long id, @Valid @RequestBody StatusUpdateRequest req) {
        return rides.updateStatus(uid(), id, req.status());
    }

    @PostMapping("/location")
    public Map<String, String> location(@Valid @RequestBody LocationUpdateRequest req) {
        rides.updateLocation(uid(), req.lat(), req.lng());
        return Map.of("status", "updated");
    }

    @GetMapping("/rides/history")
    public List<RideResponse> history() {
        return rides.driverHistory(uid());
    }

    @GetMapping("/ratings")
    public List<RatingResponse> myRatings() {
        return ratings.forDriver(uid());
    }
}
