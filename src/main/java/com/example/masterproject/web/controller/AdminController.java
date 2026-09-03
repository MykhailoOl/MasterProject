package com.example.masterproject.web.controller;

import com.example.masterproject.service.AdminDataService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final DateTimeFormatter FILE_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

    private final AdminDataService adminDataService;

    public AdminController(AdminDataService adminDataService) {
        this.adminDataService = adminDataService;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("dashboard", adminDataService.dashboard());
        return "admin/dashboard";
    }

    @GetMapping("/exports/study-data.json")
    @ResponseBody
    public ResponseEntity<byte[]> downloadJson() {
        return download(
                adminDataService.jsonExport(),
                MediaType.APPLICATION_JSON,
                "study-data-" + FILE_TIME.format(Instant.now()) + ".json");
    }

    @GetMapping("/exports/study-data-csv.zip")
    @ResponseBody
    public ResponseEntity<byte[]> downloadCsvArchive() {
        return download(
                adminDataService.csvArchive(),
                MediaType.parseMediaType("application/zip"),
                "study-data-" + FILE_TIME.format(Instant.now()) + "-csv.zip");
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
