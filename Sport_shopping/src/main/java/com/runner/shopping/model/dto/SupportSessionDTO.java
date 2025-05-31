package com.runner.shopping.model.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SupportSessionDTO {
    private Long id;
    private Long customerId;
    private Long staffId;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private String lastMessage;
    private LocalDateTime lastActivity;
}
