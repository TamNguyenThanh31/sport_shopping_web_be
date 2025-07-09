package com.runner.shopping.service;

import com.runner.shopping.model.dto.MessageDTO;

import java.util.List;

public interface MessageService {
    /**
     * Lưu một message mới:
     * - Gọi khi có tin nhắn mới từ client (WebSocket).
     * - Phải:
     *   1. Insert message vào DB
     *   2. Cập nhật session.lastMessage + lastActivity qua SupportSessionService.updateSessionActivity
     *   3. Trả về MessageDTO vừa lưu (để broadcast lại cho client)
     */
    MessageDTO saveMessage(MessageDTO messageDto);

    /**
     * Lấy toàn bộ message của một session (dùng cho REST khi khách mới open hoặc staff mới assign muốn xem lịch sử)
     */
    List<MessageDTO> getMessagesBySession(Long sessionId);

}
