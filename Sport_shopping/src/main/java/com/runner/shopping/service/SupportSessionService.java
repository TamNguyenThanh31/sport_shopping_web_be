package com.runner.shopping.service;

import com.runner.shopping.model.dto.SupportSessionDTO;

import java.util.List;
import java.util.Optional;

public interface SupportSessionService {
    /**
     * Customer mở chat: Tìm hoặc tạo session active
     * - Nếu đã có session active (customerId + endedAt is null) thì trả về nó
     * - Ngược lại tạo mới session và trả về
     */
    SupportSessionDTO openSessionForCustomer(Long customerId);

    /**
     * Staff lấy danh sách session chờ (chưa được assign, staffId IS NULL, endedAt IS NULL)
     */
    List<SupportSessionDTO> getWaitingSessions();

    /**
     * Staff lấy danh sách session mà họ đang phụ trách (staffId = currentStaffId, endedAt IS NULL)
     */
    List<SupportSessionDTO> getAssignedSessions(Long staffId);

    /**
     * Staff assign session: cập nhật staffId cho session (nếu session.staffId = NULL)
     * - Trả về SupportSessionDTO đã được cập nhật
     * - Nếu session.staffId != NULL, ném exception để báo lỗi “đã được assign”
     */
    SupportSessionDTO assignSessionToStaff(Long sessionId, Long staffId);

    /**
     * Lấy tất cả session active của một user (có thể là customer hoặc staff)
     */
    List<SupportSessionDTO> getActiveSessionsForUser(Long userId);

    /**
     * Kết thúc session (có thể do staff hoặc customer gọi)
     * - Cập nhật endedAt = now()
     */
    void closeSession(Long sessionId, Long userId);

    /**
     * Cập nhật lastMessage + lastActivity mỗi khi có message mới
     */
    void updateSessionActivity(Long sessionId, String lastMessageContent);

    /**
     * Tìm session theo id
     */
    Optional<SupportSessionDTO> findById(Long sessionId);
}
