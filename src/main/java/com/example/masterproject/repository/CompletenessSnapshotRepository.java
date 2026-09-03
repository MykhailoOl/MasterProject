package com.example.masterproject.repository;

import com.example.masterproject.model.entity.CompletenessSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompletenessSnapshotRepository extends JpaRepository<CompletenessSnapshot, Long> {
}
