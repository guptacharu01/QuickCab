package com.cabbooking.repository;

import com.cabbooking.entity.PromoCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {
    Optional<PromoCode> findByCodeIgnoreCase(String code);
}
