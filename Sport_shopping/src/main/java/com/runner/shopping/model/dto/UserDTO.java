package com.runner.shopping.model.dto;

import com.runner.shopping.enums.UserRole;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserDTO {
    Long id;
    String username;
    String email;
    UserRole role;
    String phoneNumber;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
