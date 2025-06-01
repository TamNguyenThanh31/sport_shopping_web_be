package com.runner.shopping.entity;

import com.runner.shopping.enums.SupportSessionStatus;
import com.runner.shopping.enums.converter.PaymentStatusConverter;
import com.runner.shopping.enums.converter.SupportSessionStatusConverter;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "SupportSessions")
@Data
public class SupportSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Khóa ngoại tới bảng Users
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "staff_id", nullable = true)
    private Long staffId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "last_message", length = 500)
    private String lastMessage;

    @Column(name = "last_activity")
    private LocalDateTime lastActivity;
}
