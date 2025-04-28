package com.runner.shopping.service;

import com.runner.shopping.entity.User;
import com.runner.shopping.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserService {
    User registerUser(User user);
    User authenticate(String identifier, String password);
    User loginUser(String username, String password);
    User findById(Long id);
    User findByUsername(String username);
    Optional<User> findByEmail(String email);
    Page<User> findAllByRole(UserRole role, Pageable pageable);
    Page<User> findAll(Pageable pageable);
    Page<User> searchUsers(String keyword, Pageable pageable);
    User updateUser(User user);
    void deleteUser(Long id);
    Long getUserIdByUsername(String username);
}
