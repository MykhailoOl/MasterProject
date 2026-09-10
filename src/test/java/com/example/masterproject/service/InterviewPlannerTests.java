package com.example.masterproject.service;

import static org.assertj.core.api.Assertions.*;
import static com.example.masterproject.service.InterviewFixtures.*;
import static com.example.masterproject.model.enums.RequirementCategory.*;
import static com.example.masterproject.model.interview.InterviewDocument.Resolution.*;

import com.example.masterproject.model.interview.InterviewDocument;
import com.example.masterproject.model.interview.InterviewDocument.*;
import java.util.List;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class InterviewPlannerTests {
    private final InterviewPlanner planner = new InterviewPlanner();

    @Test
    void oneCapabilityWorkflowDoesNotCoverAnother() {
        var doc = document(CAPABILITIES, fact("R1", CORE_FEATURES, "C1", "workflow", "Type and save.", CAPTURED));
        assertThat(planner.gaps(doc)).anyMatch(f -> f.capabilityId().equals("C2") && f.criterion().equals("workflow"))
                .noneMatch(f -> f.capabilityId().equals("C1") && f.criterion().equals("workflow"));
        assertThat(planner.coverage(doc, CORE_FEATURES)).isCloseTo(2.0 / 7, within(0.0001));
    }

    @Test
    void partialCriterionDoesNotBecomeCoveredByOneConfirmedFact() {
        var doc = document(CAPABILITIES,
                fact("R1", CORE_FEATURES, "C1", "workflow", "Type a note.", CAPTURED),
                fact("R2", CORE_FEATURES, "C1", "workflow", "What happens after typing?", OPEN));
        assertThat(planner.coverage(doc, CORE_FEATURES)).isCloseTo(1.0 / 7, within(0.0001));
        assertThat(planner.next(doc, List.of()).orElseThrow().criterion()).isEqualTo("problem");
    }

    @Test
    void uncertaintyStaysOpenWithoutBlockingOtherTopics() {
        var doc = document(List.of(), fact("R1", GOAL, "", "problem", "Owner will decide later.", DEFERRED));
        assertThat(planner.gaps(doc)).anyMatch(f -> f.criterion().equals("problem"));
        assertThat(planner.next(doc, List.of()).orElseThrow().criterion()).isNotEqualTo("problem");
        assertThat(planner.coverage(doc, GOAL)).isZero();
    }

    @Test
    void excludedCapabilityDoesNotLeakIntoPromptsOrCoverageTargets() {
        var doc = document(CAPABILITIES,
                fact("R1", CORE_FEATURES, "C2", "_applicable", "Finding notes is out of scope.", NOT_APPLICABLE),
                fact("R2", CORE_FEATURES, "C2", "workflow", "How should finding work?", OPEN));
        assertThat(planner.gaps(doc)).noneMatch(f -> f.capabilityId().equals("C2"));
        assertThat(planner.coverage(doc, CORE_FEATURES)).isEqualTo(0.25);
    }

    @Test
    void applicabilityIsDiscoveredWithoutTechnicalCategorySelection() {
        var gaps = planner.gaps(InterviewDocument.empty());
        assertThat(gaps).anyMatch(f -> f.category() == AUTHENTICATION && f.criterion().equals("_applicable"));
        assertThat(gaps).noneMatch(f -> f.category() == AUTHENTICATION && f.criterion().equals("session_recovery"));
    }

    @Test
    void conflictsTakePriorityAndQuestionBudgetIsEnforced() {
        var doc = document(List.of(), fact("R1", PLATFORM, "", "access", "Is it for phones or a shared screen?", CONFLICT));
        Focus focus = planner.next(doc, List.of()).orElseThrow();
        assertThat(focus.priority()).isZero();
        assertThat(planner.next(doc, Collections.nCopies(InterviewPlanner.MAX_QUESTIONS, focus))).isEmpty();
        assertThat(planner.next(doc, List.of(focus, focus)).orElseThrow().priority()).isNotZero();
    }

    @Test
    void anApplicabilityDecisionOrLaterGapCannotHideARecordedConflict() {
        var doc = document(List.of(),
                fact("R1", AUTHENTICATION, "", "_applicable", "No private areas are needed.", NOT_APPLICABLE),
                fact("R2", AUTHENTICATION, "", "identity", "Are accounts required or excluded?", CONFLICT),
                fact("R3", AUTHENTICATION, "", "identity", "The sign-in method is undecided.", DEFERRED));
        assertThat(planner.next(doc, List.of()).orElseThrow().question()).isEqualTo("Are accounts required or excluded?");
        assertThat(planner.coverage(doc, AUTHENTICATION)).isZero();
    }
}
