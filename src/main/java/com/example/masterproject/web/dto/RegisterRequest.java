package com.example.masterproject.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "Choose a username")
    @Size(min = 3, max = 24, message = "Username must be 3 to 24 characters")
    @Pattern(
            regexp = "^[A-Za-z][A-Za-z0-9_]{2,23}$",
            message = "Username must start with a letter and use only letters, numbers, or underscores")
    private String username;

    @NotBlank(message = "{auth.email.notBlank}")
    @Email(message = "{auth.email.invalid}")
    @Size(max = 255, message = "{auth.email.size}")
    private String email;

    @NotBlank(message = "{auth.password.notBlank}")
    @Size(min = 8, max = 100, message = "{auth.password.size}")
    private String password;

    @NotBlank(message = "{auth.confirmPassword.notBlank}")
    private String confirmPassword;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username == null ? null : username.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim().toLowerCase();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
