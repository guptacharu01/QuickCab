package com.cabbooking.controller;

import com.cabbooking.dto.MiscDtos.PromoResponse;
import com.cabbooking.dto.MiscDtos.RatingRequest;
import com.cabbooking.dto.RideDtos.*;
import com.cabbooking.entity.PromoCode;
import com.cabbooking.entity.Rating;
import com.cabbooking.service.RatingService;
import com.cabbooking.service.RideService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Rider ride flow: estimate, book, track, cancel, SOS, rate, promos. */
@RestController
@RequestMapping("/api/rides")
public class RideController {

    private final RideService rides;
    private final RatingService ratings;

    public RideController(RideService rides, RatingService ratings) {
        this.rides = rides;
        this.ratings = ratings;
    }

    private Long uid() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @PostMapping("/estimate")
    public FareEstimateResponse estimate(@Valid @RequestBody CreateRideRequest req) {
        return rides.estimate(req);
    }

    @PostMapping
    public ResponseEntity<RideResponse> create(@Valid @RequestBody CreateRideRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rides.createRide(uid(), req));
    }

    @GetMapping("/mine")
    public List<RideResponse> mine() {
        return rides.myRides(uid());
    }

    @GetMapping("/{id}")
    public RideResponse one(@PathVariable("id") Long id) {
        return rides.getRide(uid(), id);
    }

    @PostMapping("/{id}/cancel")
    public RideResponse cancel(@PathVariable("id") Long id) {
        return rides.cancelRide(uid(), id);
    }

    @PostMapping("/{id}/sos")
    public RideResponse sos(@PathVariable("id") Long id, @RequestBody(required = false) SosRequest req) {
        return rides.sos(uid(), id, req == null ? null : req.note());
    }

    @PostMapping("/{id}/rate")
    public ResponseEntity<Map<String, Object>> rate(@PathVariable("id") Long id,
                                                   @Valid @RequestBody RatingRequest req) {
        Rating r = ratings.rate(uid(), id, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", r.getId(), "stars", r.getStars()));
    }
}

/** Promo code validation for the booking form. */
@RestController
@RequestMapping("/api/promos")
class PromoController {

    private final RideService rides;

    PromoController(RideService rides) {
        this.rides = rides;
    }

    @GetMapping("/validate")
    public PromoResponse validate(@RequestParam("code") String code) {
        PromoCode p = rides.validPromo(code);
        return new PromoResponse(p.getCode(), p.getPercentOff(), p.getMaxDiscount(), true);
    }
}
