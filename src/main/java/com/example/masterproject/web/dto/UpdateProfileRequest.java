package com.example.masterproject.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {

    @NotBlank(message = "{auth.email.notBlank}")
    @Email(message = "{auth.email.invalid}")
    @Size(max = 255, message = "{auth.email.size}")
    private String email;

    @Size(max = 120, message = "{profile.displayName.size}")
    private String displayName;

    @NotBlank(message = "{profile.currentPassword.notBlank}")
    private String currentPassword;

    @Size(min = 6, max = 100, message = "{auth.password.size}")
    private String newPassword;

    @Size(min = 6, max = 100, message = "{auth.password.size}")
    private String confirmNewPassword;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim().toLowerCase();
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName == null ? null : displayName.trim();
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
