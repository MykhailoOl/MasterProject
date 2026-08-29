package com.example.masterproject.repository;

import com.example.masterproject.model.entity.ElicitationSession;
import com.example.masterproject.model.entity.Project;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ElicitationSessionRepository extends JpaRepository<ElicitationSession, Long> {

    List<ElicitationSession> findByProjectOrderByStartedAtDesc(Project project);

    Optional<ElicitationSession> findFirstByProjectOrderByStartedAtDesc(Project project);
}
