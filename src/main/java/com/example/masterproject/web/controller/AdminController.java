package com.example.masterproject.web.controller;

import com.example.masterproject.service.AdminDataService;
import com.example.masterproject.service.StudyAssignmentService;
import com.example.masterproject.model.enums.StudyCondition;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final DateTimeFormatter FILE_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

    private final AdminDataService adminDataService;
    private final StudyAssignmentService assignments;

    public AdminController(AdminDataService adminDataService, StudyAssignmentService assignments) {
        this.adminDataService = adminDataService;
        this.assignments = assignments;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("dashboard", adminDataService.dashboard());
        return "admin/dashboard";
    }

    @GetMapping("/users/{id}")
    public String userDetail(@PathVariable Long id, Model model) {
        model.addAttribute("userDetail", adminDataService.userDetail(id));
        model.addAttribute("studyCondition", assignments.assignment(id));
        return "admin/user-detail";
    }

    @PostMapping("/users/{id}/study-condition")
    public String assignCondition(@PathVariable Long id, @RequestParam StudyCondition condition) {
        assignments.assign(id, condition);
        return "redirect:/admin/users/" + id;
    }
    @PostMapping("/users/{id}/enroll")
    public String enroll(@PathVariable Long id) {
        assignments.randomize(id);
        return "redirect:/admin/users/" + id;
    }

    @GetMapping("/projects/{id}")
    public String projectDetail(@PathVariable Long id, Model model) {
        model.addAttribute("projectDetail", adminDataService.projectDetail(id));
        return "admin/project-detail";
    }

    @GetMapping("/exports/study-data.json")
    @ResponseBody
    public ResponseEntity<byte[]> downloadJson(@RequestParam(defaultValue = "false") boolean includeExcluded) {
        return download(
                adminDataService.jsonExport(includeExcluded),
                MediaType.APPLICATION_JSON,
                (includeExcluded ? "diagnostic-all-data-" : "study-data-") + FILE_TIME.format(Instant.now()) + ".json");
    }

    @GetMapping("/exports/study-data-csv.zip")
    @ResponseBody
    public ResponseEntity<byte[]> downloadCsvArchive(@RequestParam(defaultValue = "false") boolean includeExcluded) {
        return download(
                adminDataService.csvArchive(includeExcluded),
                MediaType.parseMediaType("application/zip"),
                (includeExcluded ? "diagnostic-all-data-" : "study-data-") + FILE_TIME.format(Instant.now()) + "-csv.zip");
    }

    private ResponseEntity<byte[]> download(byte[] content, MediaType mediaType, String fileName) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(mediaType)
                .contentLength(content.length)
                .body(content);
    }
}
