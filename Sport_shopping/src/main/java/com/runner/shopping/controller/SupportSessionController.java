package com.runner.shopping.controller;

import com.runner.shopping.entity.User;
import com.runner.shopping.model.dto.MessageDTO;
import com.runner.shopping.model.dto.SupportSessionDTO;
import com.runner.shopping.service.MessageService;
import com.runner.shopping.service.SupportSessionService;
import com.runner.shopping.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
public class SupportSessionController {

    private final SupportSessionService sessionService;
    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    public SupportSessionController(
            SupportSessionService sessionService,
            MessageService messageService,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.sessionService = sessionService;
        this.messageService = messageService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 1. Customer mở chat (hoặc lấy session active nếu đã có)
     * POST /api/sessions/open
     */
    @PostMapping("/open")
    public ResponseEntity<SupportSessionDTO> openSession(Authentication authentication) {
        // Lấy username từ authentication, sau đó lấy userId qua userService
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        Long customerId = user.getId();

        SupportSessionDTO dto = sessionService.openSessionForCustomer(customerId);
        // Sau khi open session, nếu cần trả thông báo "chưa có staff online", có thể đưa vào dto hoặc qua WebSocket
        return ResponseEntity.ok(dto);
    }

    /**
     * 2. Staff lấy danh sách session chờ (chưa assign)
     * GET /api/sessions/available
     */
    @GetMapping("/available")
    public ResponseEntity<List<SupportSessionDTO>> getWaitingSessions(Authentication authentication) {
        // SecurityConfig đã bảo đảm chỉ ROLE_STAFF có thể gọi endpoint này
        List<SupportSessionDTO> list = sessionService.getWaitingSessions();
        return ResponseEntity.ok(list);
    }

    /**
     * 3. User lấy danh sách session active của họ
     * GET /api/sessions/active
     */
    @GetMapping("/active")
    public ResponseEntity<List<SupportSessionDTO>> getActiveSessions(Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        Long userId = user.getId();

        List<SupportSessionDTO> list = sessionService.getActiveSessionsForUser(userId);
        return ResponseEntity.ok(list);
    }

    /**
     * 4. Staff assign session
     * POST /api/sessions/{sessionId}/assign
     */
    @PostMapping("/{sessionId}/assign")
    public ResponseEntity<SupportSessionDTO> assignSession(
            @PathVariable Long sessionId,
            Authentication authentication
    ) {
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        Long staffId = user.getId();

        SupportSessionDTO dto = sessionService.assignSessionToStaff(sessionId, staffId);

        // Sau khi assign thành công, gửi notification cho customer qua WebSocket
        Long customerId = dto.getCustomerId();
        String msg = "Nhân viên [" + staffId + "] đã nhận cuộc trò chuyện của bạn.";
        messagingTemplate.convertAndSendToUser(
                customerId.toString(),
                "/queue/notifications",
                msg
        );

        return ResponseEntity.ok(dto);
    }

    /**
     * 5. Đóng session (customer hoặc staff gọi)
     * POST /api/sessions/{sessionId}/close
     */
    @PostMapping("/{sessionId}/close")
    public ResponseEntity<Void> closeSession(
            @PathVariable Long sessionId,
            Authentication authentication
    ) {
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        Long userId = user.getId();

        sessionService.closeSession(sessionId, userId);

        // Notify cả hai bên (customer + staff) qua WebSocket nếu cần
        messagingTemplate.convertAndSend(
                "/topic/session." + sessionId + ".closed",
                "closed"
        );
        return ResponseEntity.ok().build();
    }

    /**
     * 6. Lấy lịch sử message của session (unlimited)
     * GET /api/sessions/{sessionId}/messages
     */
    @GetMapping("/{sessionId}/messages")
    public ResponseEntity<List<MessageDTO>> getMessages(
            @PathVariable Long sessionId,
            Authentication authentication
    ) {
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        Long userId = user.getId();

        // Kiểm tra user có phải là customer hoặc staff của session không
        SupportSessionDTO sessionDto = sessionService.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session không tồn tại: " + sessionId));
        if (!sessionDto.getCustomerId().equals(userId) &&
                (sessionDto.getStaffId() == null || !sessionDto.getStaffId().equals(userId))) {
            return ResponseEntity.status(403).build();
        }

        List<MessageDTO> list = messageService.getMessagesBySession(sessionId);
        return ResponseEntity.ok(list);
    }
}
