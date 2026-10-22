package com.cabbooking.controller;

import com.cabbooking.dto.MiscDtos.TicketRequest;
import com.cabbooking.dto.MiscDtos.TicketResponse;
import com.cabbooking.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Riders and drivers raise and track their own support tickets. */
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService tickets;

    public TicketController(TicketService tickets) {
        this.tickets = tickets;
    }

    private Long uid() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @PostMapping
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody TicketRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tickets.create(uid(), req));
    }

    @GetMapping
    public List<TicketResponse> mine() {
        return tickets.mine(uid());
    }
}
