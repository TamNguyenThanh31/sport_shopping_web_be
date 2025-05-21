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
                // Vô hiệu hóa CSRF vì sử dụng JWT
                .csrf(csrf -> csrf.disable())
                // Sử dụng session stateless cho JWT
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Cấu hình phân quyền
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints (không cần xác thực)
                        .requestMatchers(HttpMethod.GET,
                                "/api/products",
                                "/api/products/**",
                                "/api/categories",
                                "/api/categories/**").permitAll()
                        .requestMatchers(
                                "/api/users/register",
                                "/api/users/login",
                                "/uploads/**",
                                "/error").permitAll()
                        // Customer endpoints
                        .requestMatchers(HttpMethod.POST,
                                "/api/orders",
//                                "/api/orders/**/cancel",
                                "/api/addresses").hasAuthority("ROLE_CUSTOMER")
                        .requestMatchers(HttpMethod.GET,
                                "/api/orders",
                                "/api/orders/**",
                                "/api/addresses",
                                "/api/addresses/**").hasAuthority("ROLE_CUSTOMER")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/addresses/**").hasAuthority("ROLE_CUSTOMER")
                        .requestMatchers(HttpMethod.PATCH,
                                "/api/addresses/*/set-default").hasAuthority("ROLE_CUSTOMER")
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/addresses/**").hasAuthority("ROLE_CUSTOMER")
                        .requestMatchers(HttpMethod.POST,
                                "/api/cart",
                                "/api/payments").hasAuthority("ROLE_CUSTOMER")
                        .requestMatchers(HttpMethod.GET,
                                "/api/cart",
                                "/api/cart/**").hasAuthority("ROLE_CUSTOMER")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/cart/**").hasAuthority("ROLE_CUSTOMER")
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/cart/**").hasAuthority("ROLE_CUSTOMER")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/users/update-customer").hasAnyAuthority("ROLE_CUSTOMER", "ROLE_ADMIN")
                        // Staff endpoints
                        .requestMatchers(HttpMethod.POST,
                                "/api/promotions").hasAuthority("ROLE_STAFF")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/promotions/**").hasAuthority("ROLE_STAFF")
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/promotions/**").hasAuthority("ROLE_STAFF")
                        .requestMatchers(HttpMethod.GET,
                                "/api/promotions",
                                "/api/promotions/**").hasAuthority("ROLE_STAFF")
                        // Staff và Admin endpoints
                        .requestMatchers(HttpMethod.PUT,
                                "/api/orders/**/status").hasAnyAuthority("ROLE_STAFF", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.POST,
                                "/api/products",
                                "/api/categories").hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/products/**",
                                "/api/categories/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/products/**",
                                "/api/categories/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")
                        // Admin endpoints
                        .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                        // Tất cả các request khác yêu cầu xác thực
                        .anyRequest().authenticated()
                )
                // Thêm JWT filter trước UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}