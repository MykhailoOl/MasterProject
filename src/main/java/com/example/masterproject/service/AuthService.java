package com.example.masterproject.service;

import com.example.masterproject.logging.AppLog;
import com.example.masterproject.model.entity.User;
import com.example.masterproject.model.enums.UserRole;
import com.example.masterproject.repository.UserRepository;
import com.example.masterproject.web.dto.RegisterRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppLog appLog;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AppLog appLog) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.appLog = appLog;
    }

    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    @Transactional(readOnly = true)
    public boolean usernameExists(String username) {
        return userRepository.existsByUsernameIgnoreCase(username);
    }

    @Transactional
    public User register(RegisterRequest request) {
        if (emailExists(request.getEmail())) {
            throw new EmailAlreadyUsedException(request.getEmail());
        }
        if (usernameExists(request.getUsername())) {
            throw new UsernameAlreadyUsedException(request.getUsername());
        }
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.USER);
        User saved = userRepository.save(user);
        appLog.info("AUTH", "New account registered: " + saved.getUsername() + ".");
        return saved;
    }
}
