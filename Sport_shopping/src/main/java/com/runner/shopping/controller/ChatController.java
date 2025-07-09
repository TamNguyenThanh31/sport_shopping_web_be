package com.runner.shopping.controller;

import com.runner.shopping.entity.User;
import com.runner.shopping.model.dto.MessageDTO;
import com.runner.shopping.model.dto.SupportSessionDTO;
import com.runner.shopping.service.MessageService;
import com.runner.shopping.service.SupportSessionService;
import com.runner.shopping.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;


@Controller
public class ChatController {

    private final MessageService messageService;
    private final SupportSessionService sessionService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public ChatController(MessageService messageService,
                          SupportSessionService sessionService,
                          UserService userService,
                          SimpMessagingTemplate messagingTemplate) {
        this.messageService = messageService;
        this.sessionService = sessionService;
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 1) Khi client gửi message qua STOMP destination /app/chat.sendMessage,
     *    payload: MessageDTO chỉ chứa sessionId và content.
     *    Chúng ta sẽ bổ sung senderId, receiverId dựa vào Principal + thông tin session.
     */
    @MessageMapping("/chat.sendMessage")
    public void processMessage(@Payload MessageDTO payload, Principal principal) {
        // --- 1. Lấy thông tin senderId từ Principal ---
        // principal.getName() trả về username, không phải userId.
        String username = principal.getName();
        User senderUser = userService.findByUsername(username);
        Long senderId = senderUser.getId();

        // --- 2. Lấy thông tin session dựa trên payload.getSessionId() ---
        Long sessionId = payload.getSessionId();
        SupportSessionDTO sessionDto = sessionService.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session không tồn tại: " + sessionId));

        // Nếu session đã đóng, không cho gửi
        if (sessionDto.getEndedAt() != null) {
            throw new IllegalStateException("Session đã đóng, không thể gửi tin nhắn");
        }

        // --- 3. Xác định receiverId dựa vào sender (customer hay staff) ---
        Long receiverId;
        if (sessionDto.getCustomerId().equals(senderId)) {
            // Nếu sender là customer:
            Long staffId = sessionDto.getStaffId();
            if (staffId == null) {
                // Chưa có staff nhận, gán receiverId = 0 (để hiển thị thông báo sau)
                receiverId = 0L;
            } else {
                receiverId = staffId;
            }
        } else if (sessionDto.getStaffId() != null && sessionDto.getStaffId().equals(senderId)) {
            // Nếu sender là staff, receiver = customer
            receiverId = sessionDto.getCustomerId();
        } else {
            throw new IllegalStateException("Bạn không có quyền gửi tin vào session này");
        }

        // --- 4. Tạo đối tượng MessageDTO để lưu vào DB ---
        MessageDTO toSave = new MessageDTO();
        toSave.setSessionId(sessionId);
        toSave.setSenderId(senderId);
        toSave.setReceiverId(receiverId);
        toSave.setContent(payload.getContent());

        // Lưu message (service sẽ gán timestamp, readStatus=false, v.v.)
        MessageDTO saved = messageService.saveMessage(toSave);

        // --- 5. Broadcast message ra topic "/topic/session.{sessionId}" ---
        messagingTemplate.convertAndSend("/topic/session." + sessionId, saved);

        // --- 6. Nếu staff chưa online (receiverId = 0), thông báo cho customer ---
        if (receiverId == 0L) {
            String notice = "Hiện chưa có nhân viên online. Tin nhắn của bạn sẽ được để trong hàng chờ ";
            // Gửi về riêng cho customer qua user-specific queue "/user/{username}/queue/notifications"
            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/notifications",
                    notice
            );
        }
    }

    /**
     * 2) Khi staff assign session thành công (qua REST call), bạn có thể
     *    gọi phương thức này để notify customer qua WebSocket.
     *    (Ví dụ: sau khi thực thi assignSessionToStaff trong SupportSessionController,
     *    gọi chatController.notifyCustomerSessionAssigned(sessionId))
     */
    public void notifyCustomerSessionAssigned(Long sessionId) {
        SupportSessionDTO sessionDto = sessionService.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session không tồn tại: " + sessionId));
        Long customerId = sessionDto.getCustomerId();
        Long staffId = sessionDto.getStaffId();
        String msg = "Bạn đang được hỗ trợ bởi nhân viên: " + staffId;
        // Gửi message qua "/user/{customerUsername}/queue/notifications"
        User customerUser = userService.findById(customerId);
        String customerUsername = customerUser.getUsername();
        messagingTemplate.convertAndSendToUser(
                customerUsername,
                "/queue/notifications",
                msg
        );
    }

    /**
     * 3) Khi client thông báo đã đọc message (markRead), payload chỉ chứa messageId,
     *    chúng ta cũng cần lấy userId từ principal.
     */
//    @MessageMapping("/chat.markRead")
//    public void markRead(@Header("messageId") Long messageId, Principal principal) {
//        String username = principal.getName();
//        User user = userService.findByUsername(username);
//        Long userId = user.getId();
//
//        messageService.markMessageRead(messageId, userId);
//
//        // (Nếu cần, có thể broadcast lại event read receipt, v.v.)
//    }
}
