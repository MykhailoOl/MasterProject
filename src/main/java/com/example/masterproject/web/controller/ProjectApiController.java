package com.example.masterproject.web.controller;

import com.example.masterproject.service.ProjectService;
import com.example.masterproject.web.dto.CreateProjectRequest;
import com.example.masterproject.web.dto.ProjectSummaryResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectApiController {

    private final ProjectService projectService;

    public ProjectApiController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public List<ProjectSummaryResponse> listProjects() {
        return projectService.listProjectsForCurrentUser();
    }

    @GetMapping("/{id}")
    public ProjectSummaryResponse getProject(@PathVariable Long id) {
        return projectService.getProjectSummaryForCurrentUser(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectSummaryResponse createProject(@Valid @RequestBody CreateProjectRequest request) {
        Long projectId = projectService.createProject(request).getId();
        return projectService.getProjectSummaryForCurrentUser(projectId);
    }
}
