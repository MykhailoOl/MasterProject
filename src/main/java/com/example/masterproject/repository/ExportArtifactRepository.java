package com.example.masterproject.repository;

import com.example.masterproject.model.entity.ExportArtifact;
import com.example.masterproject.model.entity.Project;
import com.example.masterproject.model.enums.ExportType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExportArtifactRepository extends JpaRepository<ExportArtifact, Long> {

    List<ExportArtifact> findByProjectOrderByGeneratedAtDesc(Project project);

    Optional<ExportArtifact> findFirstByProjectAndExportTypeOrderByGeneratedAtDesc(
            Project project, ExportType exportType);
}
