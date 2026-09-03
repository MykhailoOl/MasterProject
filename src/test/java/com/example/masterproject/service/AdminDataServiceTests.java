package com.example.masterproject.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.masterproject.model.entity.User;
import com.example.masterproject.model.enums.UserRole;
import com.example.masterproject.repository.AnswerRepository;
import com.example.masterproject.repository.CompletenessSnapshotRepository;
import com.example.masterproject.repository.ElicitationSessionRepository;
import com.example.masterproject.repository.ExportArtifactRepository;
import com.example.masterproject.repository.ProjectRepository;
import com.example.masterproject.repository.QuestionRepository;
import com.example.masterproject.repository.RequirementSlotRepository;
import com.example.masterproject.repository.UserRepository;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import tools.jackson.databind.ObjectMapper;

class AdminDataServiceTests {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final ElicitationSessionRepository sessionRepository = mock(ElicitationSessionRepository.class);
    private final QuestionRepository questionRepository = mock(QuestionRepository.class);
    private final AnswerRepository answerRepository = mock(AnswerRepository.class);
    private final RequirementSlotRepository slotRepository = mock(RequirementSlotRepository.class);
    private final CompletenessSnapshotRepository snapshotRepository =
            mock(CompletenessSnapshotRepository.class);
    private final ExportArtifactRepository artifactRepository = mock(ExportArtifactRepository.class);
    private final UserContextService userContextService = mock(UserContextService.class);
    private final AdminDataService service = new AdminDataService(
            userRepository,
            projectRepository,
            sessionRepository,
            questionRepository,
            answerRepository,
            slotRepository,
            snapshotRepository,
            artifactRepository,
            userContextService,
            new ObjectMapper());

    @BeforeEach
    void emptyData() {
        when(userRepository.findAll(any(Sort.class))).thenReturn(List.of());
        when(projectRepository.findAllByOrderByIdAsc()).thenReturn(List.of());
        when(sessionRepository.findAll(any(Sort.class))).thenReturn(List.of());
        when(questionRepository.findAll(any(Sort.class))).thenReturn(List.of());
        when(answerRepository.findAll(any(Sort.class))).thenReturn(List.of());
        when(slotRepository.findAll(any(Sort.class))).thenReturn(List.of());
        when(snapshotRepository.findAll(any(Sort.class))).thenReturn(List.of());
        when(artifactRepository.findAll(any(Sort.class))).thenReturn(List.of());
    }

    @Test
    void jsonExportExcludesPasswordHashes() {
        User user = user("admin@example.com", "Admin", "$2a$secret-hash");
        when(userRepository.findAll(any(Sort.class))).thenReturn(List.of(user));

        String json = new String(service.jsonExport(), StandardCharsets.UTF_8);

        assertThat(json).contains("admin@example.com");
        assertThat(json).doesNotContain("passwordHash", "$2a$secret-hash");
    }

    @Test
    void csvArchiveContainsEveryDatasetAndNeutralizesSpreadsheetFormulas() throws Exception {
        User user = user("admin@example.com", "=HYPERLINK(\"bad\")", "$2a$secret-hash");
        when(userRepository.findAll(any(Sort.class))).thenReturn(List.of(user));

        Map<String, String> files = unzip(service.csvArchive());

        assertThat(files).containsOnlyKeys(
                "users.csv",
                "projects.csv",
                "sessions.csv",
                "questions.csv",
                "answers.csv",
                "slots.csv",
                "snapshots.csv",
                "exports.csv");
        assertThat(files.get("users.csv")).contains("\"'=HYPERLINK(\"\"bad\"\")\"");
        assertThat(files.get("users.csv")).doesNotContain("$2a$secret-hash");
    }

    private User user(String email, String displayName, String passwordHash) {
        User user = new User();
        user.setId(1L);
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setPasswordHash(passwordHash);
        user.setRole(UserRole.ADMIN);
        user.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        return user;
    }

    private Map<String, String> unzip(byte[] archive) throws Exception {
        Map<String, String> files = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                files.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return files;
    }
}
