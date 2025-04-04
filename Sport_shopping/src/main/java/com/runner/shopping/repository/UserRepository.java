package com.runner.shopping.repository;

import com.runner.shopping.entity.User;
import com.runner.shopping.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findAllByRole(UserRole role);
    Optional<User> findByPhoneNumber(String phoneNumber);
}
