package com.example.masterproject.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.masterproject.model.entity.Answer;
import com.example.masterproject.model.entity.CompletenessSnapshot;
import com.example.masterproject.model.entity.ElicitationSession;
import com.example.masterproject.model.entity.Project;
import com.example.masterproject.model.entity.Question;
import com.example.masterproject.model.entity.RequirementSlot;
import com.example.masterproject.model.enums.RequirementCategory;
import com.example.masterproject.repository.CompletenessSnapshotRepository;
import com.example.masterproject.repository.RequirementSlotRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class CompletenessSnapshotServiceTests {

    private final RequirementSlotRepository slotRepository = mock(RequirementSlotRepository.class);
    private final CompletenessSnapshotRepository snapshotRepository =
            mock(CompletenessSnapshotRepository.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CompletenessSnapshotService service =
            new CompletenessSnapshotService(slotRepository, snapshotRepository, objectMapper);

    @Test
    void capturesAllCategoryScoresAndAveragesOnlySpecificationBody() throws Exception {
        Project project = new Project();
        project.setId(10L);

        ElicitationSession session = new ElicitationSession();
        session.setId(20L);
        session.setProject(project);

        Question question = new Question();
        question.setId(30L);
        question.setSession(session);
        question.setCategory(RequirementCategory.CORE_FEATURES);
        question.setQuestionOrder(3);

        Answer answer = new Answer();
        answer.setId(40L);
        answer.setQuestion(question);

        RequirementSlot goal = slot(project, RequirementCategory.GOAL, 0.75);
        RequirementSlot features = slot(project, RequirementCategory.CORE_FEATURES, 0.5);
        RequirementSlot title = slot(project, RequirementCategory.PROJECT_TITLE, 1.0);
        when(slotRepository.findByProjectOrderByCategoryAsc(project))
                .thenReturn(List.of(features, goal, title));
        when(snapshotRepository.save(any(CompletenessSnapshot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CompletenessSnapshot snapshot = service.captureAfterAnswer(project, session, answer);

        JsonNode scores = objectMapper.readTree(snapshot.getScoresJson());
        assertThat(scores.get("GOAL").asDouble()).isEqualTo(0.75);
        assertThat(scores.get("CORE_FEATURES").asDouble()).isEqualTo(0.5);
        assertThat(scores.get("PROJECT_TITLE").asDouble()).isEqualTo(1.0);
        assertThat(snapshot.getTotalScore()).isEqualTo(0.625);
        assertThat(snapshot.getProject()).isSameAs(project);
        assertThat(snapshot.getSession()).isSameAs(session);
        assertThat(snapshot.getAnswer()).isSameAs(answer);
        assertThat(snapshot.getAnsweredCategory()).isEqualTo(RequirementCategory.CORE_FEATURES);
        assertThat(snapshot.getSequenceNumber()).isEqualTo(3);
    }

    private RequirementSlot slot(Project project, RequirementCategory category, double completeness) {
        RequirementSlot slot = new RequirementSlot();
        slot.setProject(project);
        slot.setCategory(category);
        slot.setCompleteness(completeness);
        return slot;
    }
}
