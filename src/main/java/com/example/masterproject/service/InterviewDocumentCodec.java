package com.example.masterproject.service;

import com.example.masterproject.model.interview.InterviewDocument;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class InterviewDocumentCodec {
    private final ObjectMapper mapper;

    public InterviewDocumentCodec(ObjectMapper mapper) { this.mapper = mapper; }

    public InterviewDocument read(String json) {
        if (json == null || json.isBlank()) return InterviewDocument.empty();
        try {
            InterviewDocument result = mapper.readValue(json, InterviewDocument.class);
            if (result != null && "ideaspec-interview-2".equals(result.protocolVersion())) {
                return new InterviewDocument(InterviewDocument.VERSION, result.capabilities(), result.entries(),
                        java.util.List.of(), java.util.List.of("This older record requires a new evidence check."), result.summary());
            }
            if (result == null || !InterviewDocument.VERSION.equals(result.protocolVersion())) {
                throw new IllegalStateException("Unsupported interview version");
            }
            return result;
        } catch (Exception ex) {
            throw new IllegalStateException("The saved interview could not be read. No data was replaced.", ex);
        }
    }

    public String write(Object value) { return mapper.writeValueAsString(value); }
}
