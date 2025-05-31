package com.runner.shopping.service.impl;

import com.runner.shopping.entity.Message;
import com.runner.shopping.mapper.MessageMapper;
import com.runner.shopping.model.dto.MessageDTO;
import com.runner.shopping.repository.MessageRepository;
import com.runner.shopping.repository.SupportSessionRepository;
import com.runner.shopping.service.MessageService;
import com.runner.shopping.service.SupportSessionService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;
    private final SupportSessionService supportSessionService; // để cập nhật session lên
    private final SupportSessionRepository sessionRepository;   // để validate session tồn tại

    @Override
    @Transactional
    public MessageDTO saveMessage(MessageDTO messageDto) {
        // 1. Validate session tồn tại & active
        Long sessionId = messageDto.getSessionId();
        var optSession = sessionRepository.findById(sessionId);
        if (optSession.isEmpty() || optSession.get().getEndedAt() != null) {
            throw new IllegalStateException("Session không tồn tại hoặc đã đóng: " + sessionId);
        }
        // 2. Tạo entity và lưu
        Message message = new Message();
        message.setSessionId(messageDto.getSessionId());
        message.setSenderId(messageDto.getSenderId());
        message.setReceiverId(messageDto.getReceiverId());
        message.setContent(messageDto.getContent());
        message.setTimestamp(LocalDateTime.now());
        message.setReadStatus(false);

        Message saved = messageRepository.save(message);

        // 3. Cập nhật session.lastMessage & lastActivity
        supportSessionService.updateSessionActivity(sessionId, messageDto.getContent());

        // 4. Chuyển thành DTO để return
        return messageMapper.toDto(saved);
    }

    @Override
    public List<MessageDTO> getMessagesBySession(Long sessionId) {
        List<Message> list = messageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
        return list.stream()
                .map(messageMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markMessageRead(Long messageId, Long userId) {
        Message msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message không tồn tại: " + messageId));
        // Chỉ cho phép receiver (người nhận) đánh dấu đọc
        if (!msg.getReceiverId().equals(userId)) {
            throw new IllegalStateException("Bạn không có quyền đánh dấu tin nhắn này");
        }
        msg.setReadStatus(true);
        messageRepository.save(msg);
    }

    @Override
    public List<MessageDTO> getUnreadMessages(Long sessionId, Long userId) {
        List<Message> list = messageRepository.findBySessionIdAndReceiverIdAndReadStatusFalse(sessionId, userId);
        return list.stream().map(messageMapper::toDto).collect(Collectors.toList());
    }
}
