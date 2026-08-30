package com.example.masterproject.config;

import com.example.masterproject.logging.AppLog;
import com.example.masterproject.model.entity.User;
import com.example.masterproject.model.enums.UserRole;
import com.example.masterproject.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;
    private final String demoUserEmail;
    private final String demoUserPassword;
    private final AppLog appLog;

    public DataInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AppLog appLog,
            @Value("${app.seed.admin-email}") String adminEmail,
            @Value("${app.seed.admin-password}") String adminPassword,
            @Value("${app.seed.demo-user-email}") String demoUserEmail,
            @Value("${app.seed.demo-user-password}") String demoUserPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.appLog = appLog;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.demoUserEmail = demoUserEmail;
        this.demoUserPassword = demoUserPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedUserIfMissing(adminEmail, adminPassword, UserRole.ADMIN);
        seedUserIfMissing(demoUserEmail, demoUserPassword, UserRole.USER);
    }

    private void seedUserIfMissing(String email, String rawPassword, UserRole role) {
        if (userRepository.findByEmail(email).isPresent()) {
            return;
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        userRepository.save(user);
        appLog.info("APP", "Seeded " + role + " account " + email + ".");
    }
}
