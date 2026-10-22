package com.cabbooking.controller;

import com.cabbooking.dto.AuthDtos.*;
import com.cabbooking.entity.User;
import com.cabbooking.repository.UserRepository;
import com.cabbooking.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/** Public: register + login. Authenticated: who-am-I. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final UserRepository users;

    public AuthController(AuthService auth, UserRepository users) {
        this.auth = auth;
        this.users = users;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auth.register(req));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        Long uid = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User u = users.findById(uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return Map.of("id", u.getId(), "name", u.getName(), "email", u.getEmail(), "role", u.getRole().name());
    }
}
