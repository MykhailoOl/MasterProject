package com.example.masterproject.web.dto;

import jakarta.validation.constraints.Size;

public class AnswerQuestionRequest {

    @Size(max = 5000)
    private String answerText;

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText == null ? null : answerText.trim();
    }
}
