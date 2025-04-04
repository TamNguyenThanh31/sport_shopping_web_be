package com.runner.shopping.service.impl;

import com.runner.shopping.entity.User;
import com.runner.shopping.enums.AuthProvider;
import com.runner.shopping.enums.UserRole;
import com.runner.shopping.repository.UserRepository;
import com.runner.shopping.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User registerUser(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (userRepository.findByPhoneNumber(user.getPhoneNumber()).isPresent()) {
            throw new IllegalArgumentException("Phone number already exists");
        }

        if (user.getProvider() == AuthProvider.LOCAL) {
            if (user.getPassword() == null || user.getPassword().isBlank()) {
                throw new IllegalArgumentException("Password is required for local registration");
            }
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(null);
        }

        return userRepository.save(user);
    }

    @Override
    public User authenticate(String identifier, String password) {
        // Tìm user theo username hoặc phoneNumber
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByPhoneNumber(identifier))
                .orElseThrow(() -> new RuntimeException("Invalid username or phone number"));

        // Kiểm tra mật khẩu
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        return user;
    }

    @Override
    public User loginUser(String identifier, String password) {
        // Tìm user theo username hoặc phoneNumber
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByPhoneNumber(identifier))
                .orElseThrow(() -> new IllegalArgumentException("Invalid username/phone number or password"));

        // Chỉ cho phép đăng nhập nếu provider là LOCAL
        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new IllegalArgumentException("Please use Google OAuth2 to login");
        }

        // Kiểm tra mật khẩu
        if (password == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid username/phone number or password");
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

    @Override
    public List<User> findAllByRole(UserRole role) {
        return userRepository.findAllByRole(role);
    }

    @Override
    public User updateUser(User user) {
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }

    @Override
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
