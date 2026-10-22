package com.cabbooking.repository;

import com.cabbooking.entity.Ride;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface RideRepository extends JpaRepository<Ride, Long> {

    /** Pessimistic lock: two drivers can never accept the same ride. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Ride r where r.id = :id")
    Optional<Ride> findByIdForUpdate(@Param("id") Long id);

    List<Ride> findByRiderIdOrderByCreatedAtDesc(Long riderId);
    List<Ride> findByDriverIdOrderByCreatedAtDesc(Long driverId);
    List<Ride> findByStatusOrderByCreatedAtDesc(Ride.Status status);
    long countByStatus(Ride.Status status);

    /** Active ride currently assigned to a driver (at most one). */
    @Query("select r from Ride r where r.driver.id = :driverId and r.status in ('ACCEPTED','DRIVER_ARRIVING','IN_PROGRESS')")
    Optional<Ride> findActiveByDriverId(@Param("driverId") Long driverId);
}
