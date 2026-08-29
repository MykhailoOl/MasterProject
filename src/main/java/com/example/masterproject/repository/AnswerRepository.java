package com.example.masterproject.repository;

import com.example.masterproject.model.entity.Answer;
import com.example.masterproject.model.entity.Question;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    Optional<Answer> findByQuestion(Question question);

    boolean existsByQuestion(Question question);
}
