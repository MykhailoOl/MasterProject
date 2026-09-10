package com.example.masterproject.repository;

import com.example.masterproject.model.entity.Answer;
import com.example.masterproject.model.entity.Question;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"question", "question.session"})
    java.util.List<Answer> findByQuestionSessionOrderByQuestionQuestionOrderAsc(
            com.example.masterproject.model.entity.ElicitationSession session);

    Optional<Answer> findByQuestion(Question question);

    boolean existsByQuestion(Question question);
}
