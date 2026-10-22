package com.cabbooking.config;

import com.cabbooking.entity.User;
import com.cabbooking.repository.UserRepository;
import com.cabbooking.security.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

/** Stateless JWT security: public auth/webhook/websocket endpoints, role checks everywhere else. */
@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Loads users for the login check; deactivated accounts cannot log in. */
    @Bean
    public UserDetailsService userDetailsService(UserRepository users) {
        return email -> {
            User u = users.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            return org.springframework.security.core.userdetails.User
                    .withUsername(u.getEmail())
                    .password(u.getPasswordHash())
                    .authorities("ROLE_" + u.getRole().name())
                    .disabled(!u.isActive())
                    .build();
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                            @Value("${app.cors-origins}") String corsOrigins) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // stateless JWT API
            .cors(c -> c.configurationSource(req -> {
                var cfg = new CorsConfiguration();
                cfg.setAllowedOrigins(List.of(corsOrigins.split(",")));
                cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                cfg.setAllowedHeaders(List.of("*"));
                cfg.setAllowCredentials(true);
                return cfg;
            }))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                // /me needs a valid token; login/register stay public (order matters)
                .requestMatchers("/api/auth/me").authenticated()
                .requestMatchers("/api/auth/**", "/api/payments/webhook", "/ws/**", "/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/promos/validate").hasAnyRole("RIDER", "ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/driver/**").hasRole("DRIVER")
                .requestMatchers("/api/rides/**", "/api/payments/**", "/api/tickets/**").hasAnyRole("RIDER", "ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
