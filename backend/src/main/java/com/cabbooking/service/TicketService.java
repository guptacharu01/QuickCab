package com.cabbooking.service;

import com.cabbooking.dto.MiscDtos.*;
import com.cabbooking.entity.SupportTicket;
import com.cabbooking.entity.User;
import com.cabbooking.repository.SupportTicketRepository;
import com.cabbooking.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Help-desk tickets: users raise them, admins triage them. */
@Service
public class TicketService {

    private final SupportTicketRepository tickets;
    private final UserRepository users;

    public TicketService(SupportTicketRepository tickets, UserRepository users) {
        this.tickets = tickets;
        this.users = users;
    }

    @Transactional
    public TicketResponse create(Long userId, TicketRequest req) {
        User u = users.findById(userId).orElseThrow();
        SupportTicket t = new SupportTicket();
        t.setUser(u);
        t.setSubject(req.subject());
        t.setMessage(req.message());
        tickets.save(t);
        return toResponse(t);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> mine(Long userId) {
        return tickets.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> all() {
        return tickets.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public TicketResponse setStatus(Long id, String status) {
        SupportTicket t = tickets.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        try {
            t.setStatus(SupportTicket.Status.valueOf(status));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown status: " + status);
        }
        return toResponse(t);
    }

    private TicketResponse toResponse(SupportTicket t) {
        return new TicketResponse(t.getId(), t.getSubject(), t.getMessage(), t.getStatus().name(),
                t.getCreatedAt().toString(), t.getUser().getName());
    }
}
