package com.runner.shopping.model.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MessageDTO {
    private Long id;
    private Long sessionId;
    private Long senderId;
    private Long receiverId;
    private String content;
    private LocalDateTime timestamp;
}
