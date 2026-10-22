package com.cabbooking.controller;

import com.cabbooking.dto.MiscDtos.ActiveStatusRequest;
import com.cabbooking.dto.MiscDtos.TicketResponse;
import com.cabbooking.dto.MiscDtos.TicketStatusRequest;
import com.cabbooking.dto.RideDtos.RideResponse;
import com.cabbooking.entity.User;
import com.cabbooking.service.AdminService;
import com.cabbooking.service.TicketService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Admin-only operations. Every route here requires ROLE_ADMIN (see SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService admin;
    private final TicketService tickets;

    public AdminController(AdminService admin, TicketService tickets) {
        this.admin = admin;
        this.tickets = tickets;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return admin.stats();
    }

    @GetMapping("/users")
    public List<Map<String, Object>> users() {
        return admin.allUsers().stream().map(this::userView).toList();
    }

    @PutMapping("/users/{id}/active")
    public Map<String, Object> setActive(@PathVariable("id") Long id, @RequestBody ActiveStatusRequest req) {
        return userView(admin.setActive(id, req.active()));
    }

    @GetMapping("/rides")
    public List<RideResponse> allRides() {
        return admin.allRideResponses();
    }

    @GetMapping("/payments")
    public List<Map<String, Object>> payments() {
        return admin.allPayments().stream().map(p -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("rideId", p.getRide().getId());
            m.put("amount", p.getAmount());
            m.put("status", p.getStatus().name());
            m.put("createdAt", p.getCreatedAt().toString());
            return m;
        }).toList();
    }

    @GetMapping("/tickets")
    public List<TicketResponse> tickets() {
        return tickets.all();
    }

    @PutMapping("/tickets/{id}")
    public TicketResponse ticketStatus(@PathVariable("id") Long id, @RequestBody TicketStatusRequest req) {
        return tickets.setStatus(id, req.status());
    }

    private Map<String, Object> userView(User u) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("name", u.getName());
        m.put("email", u.getEmail());
        m.put("phone", u.getPhone());
        m.put("role", u.getRole().name());
        m.put("active", u.isActive());
        m.put("createdAt", u.getCreatedAt().toString());
        return m;
    }
}
