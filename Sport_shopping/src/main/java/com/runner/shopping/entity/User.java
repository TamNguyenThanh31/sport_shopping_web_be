package com.runner.shopping.entity;

import com.runner.shopping.enums.AuthProvider;
import com.runner.shopping.enums.UserRole;
import com.runner.shopping.enums.converter.AuthProviderConverter;
import com.runner.shopping.enums.converter.UserRoleConverter;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(length = 255)
    private String password;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Convert(converter = UserRoleConverter.class)
    @Column(nullable = false)
    private UserRole role = UserRole.CUSTOMER;

    @Convert(converter = AuthProviderConverter.class)
    @Column(nullable = false)
    private AuthProvider provider = AuthProvider.LOCAL;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
