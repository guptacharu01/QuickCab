package com.cabbooking.repository;

import com.cabbooking.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByRideId(Long rideId);
    Optional<Payment> findByStripePaymentIntentId(String intentId);
}
