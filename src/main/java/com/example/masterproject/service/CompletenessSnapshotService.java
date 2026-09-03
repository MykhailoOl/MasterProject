package com.example.masterproject.service;

import com.example.masterproject.model.entity.Answer;
import com.example.masterproject.model.entity.CompletenessSnapshot;
import com.example.masterproject.model.entity.ElicitationSession;
import com.example.masterproject.model.entity.Project;
import com.example.masterproject.model.entity.Question;
import com.example.masterproject.model.entity.RequirementSlot;
import com.example.masterproject.model.taxonomy.TaxonomyCatalog;
import com.example.masterproject.repository.CompletenessSnapshotRepository;
import com.example.masterproject.repository.RequirementSlotRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class CompletenessSnapshotService {

    private final RequirementSlotRepository requirementSlotRepository;
    private final CompletenessSnapshotRepository completenessSnapshotRepository;
    private final ObjectMapper objectMapper;

    public CompletenessSnapshotService(
            RequirementSlotRepository requirementSlotRepository,
            CompletenessSnapshotRepository completenessSnapshotRepository,
            ObjectMapper objectMapper) {
        this.requirementSlotRepository = requirementSlotRepository;
        this.completenessSnapshotRepository = completenessSnapshotRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CompletenessSnapshot captureAfterAnswer(Project project, ElicitationSession session, Answer answer) {
        Question question = answer.getQuestion();
        if (!question.getSession().getId().equals(session.getId())
                || !session.getProject().getId().equals(project.getId())) {
            throw new IllegalArgumentException("Answer does not belong to the project session");
        }

        List<RequirementSlot> slots =
                requirementSlotRepository.findByProjectOrderByCategoryAsc(project);
        Map<String, Double> categoryScores = new LinkedHashMap<>();
        for (RequirementSlot slot : slots) {
            categoryScores.put(slot.getCategory().name(), normalized(slot.getCompleteness()));
        }

        double totalScore = slots.stream()
                .filter(slot -> TaxonomyCatalog.require(slot.getCategory()).includeInSpecBody())
                .mapToDouble(RequirementSlot::getCompleteness)
                .average()
                .orElse(0.0);

        CompletenessSnapshot snapshot = new CompletenessSnapshot();
        snapshot.setProject(project);
        snapshot.setSession(session);
        snapshot.setAnswer(answer);
        snapshot.setAnsweredCategory(question.getCategory());
        snapshot.setSequenceNumber(question.getQuestionOrder());
        snapshot.setScoresJson(toJson(categoryScores));
        snapshot.setTotalScore(normalized(totalScore));
        snapshot.setCapturedAt(Instant.now());
        return completenessSnapshotRepository.save(snapshot);
    }

    private String toJson(Map<String, Double> categoryScores) {
        try {
            return objectMapper.writeValueAsString(categoryScores);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not serialize completeness scores", ex);
        }
    }

    private double normalized(double score) {
        double bounded = Math.max(0.0, Math.min(1.0, score));
        return Math.round(bounded * 10000.0) / 10000.0;
    }
}
