package com.runner.shopping.config;

import com.runner.shopping.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.config.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * Cấu hình STOMP over WebSocket, thay đổi để dùng setAllowedOriginPatterns("*")
 * thay vì setAllowedOrigins("*") nhằm tránh lỗi CORS khi allowCredentials = true.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtChannelInterceptor jwtChannelInterceptor;

    @Autowired
    public WebSocketConfig(JwtChannelInterceptor jwtChannelInterceptor) {
        this.jwtChannelInterceptor = jwtChannelInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Đăng ký STOMP endpoint tại /ws/support, cho phép SockJS
        // Sử dụng setAllowedOriginPatterns("*") thay cho setAllowedOrigins("*")
        // để không gặp lỗi "When allowCredentials is true, allowedOrigins cannot contain '*'"
        registry.addEndpoint("/ws/support")
                .setAllowedOriginPatterns("*")  // cho phép mọi origin, bao gồm credentials
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Cấu hình broker in‐memory
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Thêm interceptor JWT để parse token khi client CONNECT
        registration.interceptors(jwtChannelInterceptor);
    }
}

/**
 * Interceptor đọc JWT trong STOMP CONNECT, gắn Authentication vào Principal.
 */
@Component
class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Autowired
    public JwtChannelInterceptor(JwtUtil jwtUtil,
                                 UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            List<String> authHeader = accessor.getNativeHeader("Authorization");
            if (authHeader != null && !authHeader.isEmpty()) {
                String bearerToken = authHeader.get(0);
                if (bearerToken.startsWith("Bearer ")) {
                    String token = bearerToken.substring(7);
                    String username = null;
                    try {
                        username = jwtUtil.extractUsername(token);
                    } catch (Exception ignored) {}

                    if (username != null
                            && SecurityContextHolder.getContext().getAuthentication() == null
                            && jwtUtil.validateToken(token, username)) {

                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        String role = jwtUtil.extractClaim(token, claims -> claims.get("role", String.class));
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        Collections.singletonList(new SimpleGrantedAuthority(role))
                                );
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        accessor.setUser(authentication);
                    }
                }
            }
        }
        return message;
    }
}
