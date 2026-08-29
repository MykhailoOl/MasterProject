package com.example.masterproject.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SimplifyTextRequest {

    @NotBlank
    @Size(min = 8, max = 4000)
    private String selectedText;

    public String getSelectedText() {
        return selectedText;
    }

    public void setSelectedText(String selectedText) {
        this.selectedText = selectedText == null ? null : selectedText.trim();
    }
}
