package com.runner.shopping.controller;


import com.runner.shopping.entity.User;
import com.runner.shopping.mapper.UserMapper;
import com.runner.shopping.model.dto.UserDTO;
import com.runner.shopping.model.request.LoginRequest;
import com.runner.shopping.model.response.LoginResponse;
import com.runner.shopping.security.JwtUtil;
import com.runner.shopping.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(@RequestBody User user) {
        User registeredUser = userService.registerUser(user);
        UserDTO userDTO = userMapper.toDTO(registeredUser);
        return ResponseEntity.ok(userDTO);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        User user = userService.loginUser(loginRequest.getIdentifier(), loginRequest.getPassword());
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        UserDTO userDTO = userMapper.toDTO(user);
        return ResponseEntity.ok(new LoginResponse(token, userDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        User user = userService.findById(id);
        UserDTO userDTO = userMapper.toDTO(user);
        return ResponseEntity.ok(userDTO);
    }

    @PutMapping("/update-customer")
    public ResponseEntity<UserDTO> updateProfile(@RequestBody User updatedUser, Authentication authentication) {
        String currentUsername = authentication.getName();
        User currentUser = userService.findByUsername(currentUsername);

        // Không cần kiểm tra id, vì thông tin đã lấy từ token
        currentUser.setUsername(updatedUser.getUsername());
        currentUser.setEmail(updatedUser.getEmail());
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isBlank()) {
            currentUser.setPassword(updatedUser.getPassword());
        }

        User savedUser = userService.updateUser(currentUser);
        return ResponseEntity.ok(userMapper.toDTO(savedUser));
    }
}
