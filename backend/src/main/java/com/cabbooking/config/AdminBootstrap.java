package com.cabbooking.config;

import com.cabbooking.entity.PromoCode;
import com.cabbooking.entity.User;
import com.cabbooking.repository.PromoCodeRepository;
import com.cabbooking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Bootstraps the first admin account from environment variables
 * (ADMIN_EMAIL / ADMIN_PASSWORD) - there is no public admin registration.
 * Also seeds the demo promo codes when the table is empty.
 */
@Component
public class AdminBootstrap implements CommandLineRunner {

    private final UserRepository users;
    private final PromoCodeRepository promos;
    private final PasswordEncoder encoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminBootstrap(UserRepository users, PromoCodeRepository promos, PasswordEncoder encoder,
                          @Value("${admin.email:}") String adminEmail,
                          @Value("${admin.password:}") String adminPassword) {
        this.users = users;
        this.promos = promos;
        this.encoder = encoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (adminEmail != null && !adminEmail.isBlank()
                && adminPassword != null && !adminPassword.isBlank()
                && !users.existsByEmail(adminEmail.toLowerCase())) {
            User admin = new User();
            admin.setName("Admin");
            admin.setEmail(adminEmail.toLowerCase());
            admin.setPhone("0000000000");
            admin.setPasswordHash(encoder.encode(adminPassword));
            admin.setRole(User.Role.ADMIN);
            users.save(admin);
            System.out.println("[bootstrap] Admin account created for " + adminEmail);
        }
        if (promos.count() == 0) {
            PromoCode w = new PromoCode();
            w.setCode("WELCOME50"); w.setPercentOff(50); w.setMaxDiscount(100);
            promos.save(w);
            PromoCode f = new PromoCode();
            f.setCode("FLAT30"); f.setPercentOff(0); f.setMaxDiscount(30);
            promos.save(f);
            System.out.println("[bootstrap] Seeded promo codes WELCOME50, FLAT30");
        }
    }
}
