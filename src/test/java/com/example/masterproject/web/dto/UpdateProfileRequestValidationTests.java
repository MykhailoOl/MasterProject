package com.example.masterproject.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class UpdateProfileRequestValidationTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void newPasswordRequiresAtLeastEightCharacters() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setEmail("user@example.com");
        request.setDisplayName("User");
        request.setCurrentPassword("current-password");
        request.setNewPassword("1234567");
        request.setConfirmNewPassword("1234567");

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("newPassword", "confirmNewPassword");
    }

    @Test
    void eightCharacterPasswordIsAccepted() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setEmail("user@example.com");
        request.setDisplayName("User");
        request.setCurrentPassword("current-password");
        request.setNewPassword("12345678");
        request.setConfirmNewPassword("12345678");

        assertThat(validator.validate(request)).isEmpty();
    }
}
