package com.example.masterproject.repository;

import com.example.masterproject.model.entity.LlmCallAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LlmCallAuditRepository extends JpaRepository<LlmCallAudit, String> {}
