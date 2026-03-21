package com.system.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "disputes")
@Data
public class Dispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(columnDefinition = "NVARCHAR(1000)", nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DisputeStatus status = DisputeStatus.OPEN;

    @Column(name = "hold_applied", nullable = false)
    private boolean holdApplied = false;

    @Column(name = "hold_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal holdAmount = BigDecimal.ZERO;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public enum DisputeStatus {
        OPEN,
        SELLER_ACCEPTED,
        ADMIN_REVIEW,
        RESOLVED_FOR_BUYER,
        RESOLVED_FOR_SELLER
    }
}
