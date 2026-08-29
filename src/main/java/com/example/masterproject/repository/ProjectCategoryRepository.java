package com.example.masterproject.repository;

import com.example.masterproject.model.entity.Project;
import com.example.masterproject.model.entity.ProjectCategory;
import com.example.masterproject.model.enums.RequirementCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectCategoryRepository extends JpaRepository<ProjectCategory, Long> {

    List<ProjectCategory> findByProjectOrderByIdAsc(Project project);

    Optional<ProjectCategory> findByProjectAndCategory(Project project, RequirementCategory category);
}
