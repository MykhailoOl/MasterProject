package com.example.masterproject.repository;

import com.example.masterproject.model.entity.Project;
import com.example.masterproject.model.entity.User;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Project p join fetch p.owner where p.id = :id")
    java.util.Optional<Project> findForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

    @EntityGraph(attributePaths = "owner")
    List<Project> findByOwnerOrderByUpdatedAtDesc(User owner);

    @EntityGraph(attributePaths = "owner")
    List<Project> findAllByOrderByUpdatedAtDesc();

    @EntityGraph(attributePaths = "owner")
    List<Project> findAllByOrderByIdAsc();
}
