package com.example.masterproject.service;

import com.example.masterproject.model.entity.User;
import com.example.masterproject.repository.UserRepository;
import com.example.masterproject.web.dto.UpdateProfileRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final UserContextService userContextService;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(
            UserRepository userRepository,
            UserContextService userContextService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userContextService = userContextService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public User getCurrentProfile() {
        return userContextService.getCurrentUser();
    }

    @Transactional
    public User updateProfile(UpdateProfileRequest request) {
        User user = userContextService.getCurrentUser();
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidProfileUpdateException("Current password is incorrect.");
        }

        String newEmail = request.getEmail();
        if (!newEmail.equalsIgnoreCase(user.getEmail())
                && userRepository.findByEmail(newEmail).isPresent()) {
            throw new EmailAlreadyUsedException(newEmail);
        }

        boolean changingPassword = request.getNewPassword() != null && !request.getNewPassword().isBlank();
        if (changingPassword) {
            if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
                throw new InvalidProfileUpdateException("New passwords do not match.");
            }
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        }

        user.setEmail(newEmail);
        user.setDisplayName(
                request.getDisplayName() == null || request.getDisplayName().isBlank()
                        ? null
                        : request.getDisplayName());
        return userRepository.save(user);
    }
}
