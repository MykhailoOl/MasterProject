package com.example.masterproject.repository;

import com.example.masterproject.model.entity.Project;
import com.example.masterproject.model.entity.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByOwnerOrderByUpdatedAtDesc(User owner);

    List<Project> findAllByOrderByUpdatedAtDesc();
}
