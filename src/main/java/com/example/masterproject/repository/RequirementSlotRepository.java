package com.example.masterproject.repository;

import com.example.masterproject.model.entity.Project;
import com.example.masterproject.model.entity.RequirementSlot;
import com.example.masterproject.model.enums.RequirementCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequirementSlotRepository extends JpaRepository<RequirementSlot, Long> {

    List<RequirementSlot> findByProjectOrderByCategoryAsc(Project project);

    Optional<RequirementSlot> findByProjectAndCategory(Project project, RequirementCategory category);
}
