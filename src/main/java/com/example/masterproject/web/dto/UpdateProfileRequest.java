package com.example.masterproject.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {

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

    @NotBlank(message = "{profile.currentPassword.notBlank}")
    private String currentPassword;

    @Size(min = 8, max = 100, message = "{auth.password.size}")
    private String newPassword;

    @Size(min = 8, max = 100, message = "{auth.password.size}")
    private String confirmNewPassword;

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

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword == null || newPassword.isBlank() ? null : newPassword;
    }

    public String getConfirmNewPassword() {
        return confirmNewPassword;
    }

    public void setConfirmNewPassword(String confirmNewPassword) {
        this.confirmNewPassword =
                confirmNewPassword == null || confirmNewPassword.isBlank() ? null : confirmNewPassword;
    }
}
