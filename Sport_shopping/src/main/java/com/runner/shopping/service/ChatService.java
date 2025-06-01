package com.runner.shopping.service;

import com.runner.shopping.entity.User;
import com.runner.shopping.model.dto.MessageDTO;

import java.util.List;

public interface ChatService {
    MessageDTO saveAndSendMessage(MessageDTO dto);
    List<MessageDTO> getMessagesBySession(Long sessionId, Long viewerId);
}

