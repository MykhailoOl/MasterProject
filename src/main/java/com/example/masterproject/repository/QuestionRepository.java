package com.example.masterproject.repository;

import com.example.masterproject.model.entity.ElicitationSession;
import com.example.masterproject.model.entity.Question;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findBySessionOrderByQuestionOrderAsc(ElicitationSession session);

    Optional<Question> findFirstBySessionAndId(ElicitationSession session, Long id);

    long countBySession(ElicitationSession session);
}
