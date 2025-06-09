package com.runner.shopping.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Tắt CSRF, chuyển sang STATELESS vì dùng JWT
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // 1. Public endpoints
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/products",
                                "/api/products/**",
                                "/api/categories",
                                "/api/categories/**",
                                "/api/vnpay/return"
                        ).permitAll()
                        .requestMatchers(
                                "/api/users/register",
                                "/api/users/login",
                                "/uploads/**",
                                "/error",
                                "/ws-support/**",
                                "/ws/**"
                        ).permitAll()
                        // 3. Customer‐only endpoints
                        .requestMatchers(HttpMethod.POST,
                                "/api/orders",
                                "/api/orders/*/cancel",
                                "/api/orders/*/vnpay",
                                "/api/orders/vnpay/return",
                                "/api/addresses",
                                "/api/sessions/open",
                                "/api/sessions/{sessionId}/close"
                        ).hasAuthority("ROLE_CUSTOMER")
                        .requestMatchers(HttpMethod.GET,
                                "/api/orders",
                                "/api/orders/**",
                                "/api/addresses",
                                "/api/addresses/**"
                        ).hasAuthority("ROLE_CUSTOMER")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/addresses/**"
                        ).hasAuthority("ROLE_CUSTOMER")
                        .requestMatchers(HttpMethod.PATCH,
                                "/api/addresses/*/set-default"
                        ).hasAuthority("ROLE_CUSTOMER")
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/addresses/**"
                        ).hasAuthority("ROLE_CUSTOMER")
                        .requestMatchers(HttpMethod.POST,
                                "/api/cart",
                                "/api/payments"
                        ).hasAuthority("ROLE_CUSTOMER")
                        .requestMatchers(HttpMethod.GET,
                                "/api/cart",
                                "/api/cart/**"
                        ).hasAuthority("ROLE_CUSTOMER")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/cart/**"
                        ).hasAuthority("ROLE_CUSTOMER")
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/cart/**"
                        ).hasAuthority("ROLE_CUSTOMER")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/users/update-customer"
                        ).hasAnyAuthority("ROLE_CUSTOMER", "ROLE_ADMIN")

                        // Strava – chỉ CUSTOMER
                        .requestMatchers(HttpMethod.GET,
                                "/api/strava/callback", "/api/strava/status", "/api/strava/coupons"
                        ).hasAuthority("ROLE_CUSTOMER")
                        .requestMatchers(HttpMethod.POST,
                                "/api/strava/redeem-coupon"
                        ).hasAuthority("ROLE_CUSTOMER")

                        // 5. Staff‐only endpoints
                        .requestMatchers(HttpMethod.POST,
                                "/api/promotions",
                                "/api/sessions/{sessionId}/assign"
                        ).hasAuthority("ROLE_STAFF")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/promotions/**"
                        ).hasAuthority("ROLE_STAFF")
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/promotions/**"
                        ).hasAuthority("ROLE_STAFF")

                        .requestMatchers(HttpMethod.GET,
                                "/api/promotions",
                                "/api/promotions/**")
                        .hasAnyAuthority("ROLE_STAFF","ROLE_CUSTOMER")

                        // 5. Strava coupons riêng
                        .requestMatchers(HttpMethod.GET,
                                "/api/strava/coupons")
                        .hasAuthority("ROLE_CUSTOMER")

                        // Session-available vẫn cho customer & staff
                                .requestMatchers(HttpMethod.GET,
                                        "/api/sessions/available"
                                ).hasAnyAuthority("ROLE_STAFF", "ROLE_CUSTOMER")

                        // 6. “sessions/active” cho Customer hoặc Staff
                        .requestMatchers(HttpMethod.GET,
                                "/api/sessions/active",
                                "/api/sessions/{sessionId}/messages"
                        ).hasAnyAuthority("ROLE_CUSTOMER", "ROLE_STAFF")

                        // 7. Admin‐only endpoints
                        .requestMatchers("/api/admin/**")
                        .hasAuthority("ROLE_ADMIN")

                        // 8. WebSocket/SockJS STOMP endpoints
                        .requestMatchers("/ws-support/**", "/ws/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/topic/**", "/queue/**"
                        ).authenticated()

                        // 9. Các request còn lại yêu cầu auth
                        .anyRequest().authenticated()
                )
                // Thêm filter JWT trước UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
