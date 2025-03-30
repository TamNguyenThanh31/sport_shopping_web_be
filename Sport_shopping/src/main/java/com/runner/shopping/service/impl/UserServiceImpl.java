package com.runner.shopping.service.impl;

import com.runner.shopping.entity.User;
import com.runner.shopping.enums.AuthProvider;
import com.runner.shopping.repository.UserRepository;
import com.runner.shopping.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@AllArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User registerUser(User user) {
        // Kiểm tra username đã tồn tại chưa
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Kiểm tra email đã tồn tại chưa
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Mã hóa mật khẩu nếu có (cho đăng ký local)
        if (user.getProvider() == AuthProvider.LOCAL) {
            if (user.getPassword() == null || user.getPassword().isBlank()) {
                throw new IllegalArgumentException("Password is required for local registration");
            }
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            // Nếu là Google (OAuth2), không cần mật khẩu
            user.setPassword(null);
        }

        // Lưu user vào database
        return userRepository.save(user);
    }

    @Override
    public User loginUser(String username, String password) {
        // Tìm user theo username
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        // Kiểm tra provider
        if (user.getProvider() == AuthProvider.LOCAL) {
            // Đăng nhập local: kiểm tra mật khẩu
            if (password == null || !passwordEncoder.matches(password, user.getPassword())) {
                throw new IllegalArgumentException("Invalid username or password");
            }
        } else {
            // Nếu là Google, không cho phép đăng nhập bằng username/password
            throw new IllegalArgumentException("Please use Google OAuth2 to login");
        }

        return user;
    }

    @Override
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
    }

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
