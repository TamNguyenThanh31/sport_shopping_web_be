package com.runner.shopping.config;

import com.runner.shopping.security.JwtAuthenticationFilter;
import com.runner.shopping.security.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;

@Configuration
public class SecurityFilterConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter();
        filter.setJwtUtil(jwtUtil);
        filter.setUserDetailsService(userDetailsService);
        return filter;
    }
}