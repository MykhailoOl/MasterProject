package com.example.masterproject.repository;

import com.example.masterproject.model.entity.Project;
import com.example.masterproject.model.entity.User;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    @EntityGraph(attributePaths = "owner")
    List<Project> findByOwnerOrderByUpdatedAtDesc(User owner);

    @EntityGraph(attributePaths = "owner")
    List<Project> findAllByOrderByUpdatedAtDesc();

    @EntityGraph(attributePaths = "owner")
    List<Project> findAllByOrderByIdAsc();
}
