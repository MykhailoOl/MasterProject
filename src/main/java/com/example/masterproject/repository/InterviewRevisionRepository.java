package com.example.masterproject.repository;

import com.example.masterproject.model.entity.InterviewRevision;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewRevisionRepository extends JpaRepository<InterviewRevision, Long> {
    List<InterviewRevision> findByProjectIdOrderByRevisionAsc(Long projectId);
}

