package com.runner.shopping.service;

import com.runner.shopping.entity.User;

import java.util.Optional;

public interface UserService {
    User registerUser(User user);
    User loginUser(String username, String password);
    User findById(Long id);
    User findByUsername(String username);
    Optional<User> findByEmail(String email); // Hỗ trợ OAuth2 sau này
}
