package com.cabbooking.repository;

import com.cabbooking.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    Optional<Rating> findByRideId(Long rideId);
    List<Rating> findByDriverIdOrderByCreatedAtDesc(Long driverId);
}
