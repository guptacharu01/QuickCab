package com.cabbooking.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

/** Help-desk tickets raised by riders/drivers and handled by admins. */
@Entity
@Table(name = "support_tickets")
@Getter @Setter @NoArgsConstructor
public class SupportTicket {

    public enum Status { OPEN, IN_PROGRESS, CLOSED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, length = 2000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.OPEN;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
