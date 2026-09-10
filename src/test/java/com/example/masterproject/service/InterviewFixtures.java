package com.example.masterproject.service;

import com.example.masterproject.model.enums.RequirementCategory;
import com.example.masterproject.model.interview.InterviewDocument;
import com.example.masterproject.model.interview.InterviewDocument.*;
import java.util.List;

final class InterviewFixtures {
    static final List<Evidence> EVIDENCE = List.of(new Evidence("IDEA", "save and find notes"));
    static final List<Capability> CAPABILITIES = List.of(new Capability("C1", "Save notes", EVIDENCE),
            new Capability("C2", "Find notes", EVIDENCE));
    static Entry fact(String id, RequirementCategory category, String cap, String criterion, String text, Resolution resolution) {
        return new Entry(id, category, cap, criterion, text, resolution, EVIDENCE);
    }
    static InterviewDocument document(List<Capability> capabilities, Entry... entries) {
        return new InterviewDocument(InterviewDocument.VERSION, capabilities, List.of(entries), List.of("IDEA"), List.of(), "",
                List.of(new SourceCheck("IDEA", "EXTRACTED", "Fixture source checked", true)));
    }
    private InterviewFixtures() {}
}
