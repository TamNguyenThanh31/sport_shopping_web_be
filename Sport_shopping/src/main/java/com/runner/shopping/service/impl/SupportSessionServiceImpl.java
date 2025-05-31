package com.runner.shopping.service.impl;

import com.runner.shopping.entity.SupportSession;
import com.runner.shopping.mapper.SupportSessionMapper;
import com.runner.shopping.model.dto.SupportSessionDTO;
import com.runner.shopping.repository.SupportSessionRepository;
import com.runner.shopping.service.SupportSessionService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class SupportSessionServiceImpl implements SupportSessionService {

    private final SupportSessionRepository sessionRepository;
    private final SupportSessionMapper sessionMapper;


    @Override
    @Transactional
    public SupportSessionDTO openSessionForCustomer(Long customerId) {
        // 1. Kiểm tra xem đã có session active cho customer hay chưa
        Optional<SupportSession> existing = sessionRepository.findByCustomerIdAndEndedAtIsNull(customerId);
        if (existing.isPresent()) {
            return sessionMapper.toDto(existing.get());
        }
        // 2. Nếu chưa có, tạo mới
        SupportSession newSession = new SupportSession();
        newSession.setCustomerId(customerId);
        newSession.setStartedAt(LocalDateTime.now());
        // staffId, endedAt, lastMessage, lastActivity để null
        SupportSession saved = sessionRepository.save(newSession);
        return sessionMapper.toDto(saved);
    }

    @Override
    public List<SupportSessionDTO> getWaitingSessions() {
        List<SupportSession> list = sessionRepository.findByStaffIdIsNullAndEndedAtIsNullOrderByLastActivityDesc();
        return list.stream()
                .map(sessionMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<SupportSessionDTO> getAssignedSessions(Long staffId) {
        List<SupportSession> list = sessionRepository.findByStaffIdAndEndedAtIsNullOrderByLastActivityDesc(staffId);
        return list.stream()
                .map(sessionMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SupportSessionDTO assignSessionToStaff(Long sessionId, Long staffId) {
        SupportSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session không tồn tại: " + sessionId));
        if (session.getStaffId() != null) {
            throw new IllegalStateException("Session đã được nhân viên khác nhận trước đó");
        }
        session.setStaffId(staffId);
        // Nếu session chưa có lastActivity thì có thể set lastActivity = now(), nhưng ko bắt buộc
        if (session.getLastActivity() == null) {
            session.setLastActivity(LocalDateTime.now());
        }
        SupportSession updated = sessionRepository.save(session);
        return sessionMapper.toDto(updated);
    }

    @Override
    public List<SupportSessionDTO> getActiveSessionsForUser(Long userId) {
        List<SupportSession> list = sessionRepository.findByCustomerIdOrStaffIdAndEndedAtIsNullOrderByLastActivityDesc(userId, userId);
        return list.stream().map(sessionMapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void closeSession(Long sessionId, Long userId) {
        SupportSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session không tồn tại: " + sessionId));

        // Chỉ cho phép staff hoặc chủ session (customer) có quyền đóng
        if (!session.getCustomerId().equals(userId) && (session.getStaffId() == null || !session.getStaffId().equals(userId))) {
            throw new IllegalStateException("Bạn không có quyền đóng session này");
        }
        session.setEndedAt(LocalDateTime.now());
        sessionRepository.save(session);
    }

    @Override
    @Transactional
    public void updateSessionActivity(Long sessionId, String lastMessageContent) {
        SupportSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session không tồn tại: " + sessionId));
        session.setLastMessage(lastMessageContent);
        session.setLastActivity(LocalDateTime.now());
        sessionRepository.save(session);
    }

    @Override
    public Optional<SupportSessionDTO> findById(Long sessionId) {
        Optional<SupportSession> opt = sessionRepository.findById(sessionId);
        return opt.map(sessionMapper::toDto);
    }
}
