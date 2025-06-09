package com.runner.shopping.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity @Table(name="StravaTokens")
@Data
public class StravaToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "access_token", nullable = false)
    private String accessToken;

    @Column(name = "refresh_token", nullable = false)
    private String refreshToken;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    // thêm createdAt
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // (nếu bạn muốn track lastSync)
    @Column(name = "last_sync_timestamp")
    private LocalDateTime lastSyncTimestamp;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
