package com.example.masterproject.service;

import static org.assertj.core.api.Assertions.*;
import java.sql.DriverManager;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "INTERVIEW_TEST_DATABASE_URL", matches = ".+")
class PostgresMigrationTests {
    @Test
    void protocolTwoDocumentsAndGeneratedLegacyAnswersRemainExcludedAfterVersionNineUpgrade() throws Exception {
        String url = System.getenv("INTERVIEW_TEST_DATABASE_URL");
        String schema = "protocol_two_" + UUID.randomUUID().toString().replace("-", "");
        Flyway.configure().dataSource(url, "postgres", "").schemas(schema).defaultSchema(schema)
                .locations("classpath:db/migration").target("9").load().migrate();
        try (var connection = DriverManager.getConnection(url, "postgres", ""); var sql = connection.createStatement()) {
            sql.execute("SET search_path TO " + schema);
            sql.execute("INSERT INTO users (id,email,username,password_hash,role,study_condition) VALUES (202,'protocol2@example.com','protocol2','old-hash','USER','BASELINE')");
            sql.execute("INSERT INTO projects (id,user_id,title,initial_idea,status,interview_document,interview_revision,reviewed_revision) VALUES (202,202,'Old plan','A saved old idea','COMPLETED','{\"protocolVersion\":\"ideaspec-interview-2\",\"summary\":\"Previously confirmed\"}',2,2)");
            sql.execute("INSERT INTO elicitation_sessions (id,project_id,condition_tag) VALUES (202,202,'BASELINE')");
            sql.execute("INSERT INTO questions (id,session_id,category,question_text,question_order,answer_example) VALUES (202,202,'GOAL','Who needs it?',1,'Generated administrator example')");
            sql.execute("INSERT INTO answers (id,question_id,answer_text) VALUES (202,202,'Generated administrator example')");
            sql.execute("INSERT INTO export_artifacts (id,project_id,export_type,content,source_revision) VALUES (202,202,'SPEC_MD','Previously reviewed spec',2)");
        }
        Flyway.configure().dataSource(url, "postgres", "").schemas(schema).defaultSchema(schema)
                .locations("classpath:db/migration").load().migrate();
        try (var connection = DriverManager.getConnection(url, "postgres", ""); var sql = connection.createStatement()) {
            sql.execute("SET search_path TO " + schema);
            try (var rows = sql.executeQuery("SELECT collection_protocol,interview_document,reviewed_revision FROM projects WHERE id=202")) {
                assertThat(rows.next()).isTrue(); assertThat(rows.getString(1)).isEqualTo("LEGACY_UNVERIFIED");
                assertThat(rows.getString(2)).contains("ideaspec-interview-2", "Previously confirmed");
                assertThat(rows.getLong(3)).isEqualTo(2);
            }
            try (var rows = sql.executeQuery("SELECT provenance,answer_text FROM answers WHERE id=202")) {
                assertThat(rows.next()).isTrue(); assertThat(rows.getString(1)).isEqualTo("LEGACY_UNKNOWN");
                assertThat(rows.getString(2)).isEqualTo("Generated administrator example");
            }
            try (var rows = sql.executeQuery("SELECT study_enrolled,condition_tag FROM elicitation_sessions WHERE id=202")) {
                assertThat(rows.next()).isTrue(); assertThat(rows.getBoolean(1)).isFalse(); assertThat(rows.getString(2)).isEqualTo("BASELINE");
            }
            try (var rows = sql.executeQuery("SELECT content,source_revision FROM export_artifacts WHERE id=202")) {
                assertThat(rows.next()).isTrue(); assertThat(rows.getString(1)).isEqualTo("Previously reviewed spec");
                assertThat(rows.getLong(2)).isEqualTo(2);
            }
        }
    }

    @Test
    void upgradesExistingVersionEightDataWithoutLosingAnswersOrPriorExports() throws Exception {
        String url = System.getenv("INTERVIEW_TEST_DATABASE_URL");
        String schema = "legacy_" + UUID.randomUUID().toString().replace("-", "");
        Flyway.configure().dataSource(url, "postgres", "").schemas(schema).defaultSchema(schema)
                .locations("classpath:db/migration").target("8").load().migrate();
        try (var connection = DriverManager.getConnection(url, "postgres", ""); var sql = connection.createStatement()) {
            sql.execute("SET search_path TO " + schema);
            sql.execute("INSERT INTO users (id,email,username,password_hash,role) VALUES (101,'legacy@example.com','legacyuser','old-hash','USER')");
            sql.execute("INSERT INTO projects (id,user_id,title,initial_idea,status) VALUES (101,101,'Legacy notes','Remember important notes','COMPLETED')");
            sql.execute("INSERT INTO elicitation_sessions (id,project_id) VALUES (101,101)");
            sql.execute("INSERT INTO questions (id,session_id,category,question_text,question_order) VALUES (101,101,'GOAL','Who needs it?',1)");
            sql.execute("INSERT INTO answers (id,question_id,answer_text) VALUES (101,101,'Students need private notes')");
            sql.execute("INSERT INTO export_artifacts (id,project_id,export_type,content) VALUES (101,101,'SPEC_MD','Original spec bytes')");
        }
        var result = Flyway.configure().dataSource(url, "postgres", "").schemas(schema).defaultSchema(schema)
                .locations("classpath:db/migration").load().migrate();
        assertThat(result.success).isTrue();
        try (var connection = DriverManager.getConnection(url, "postgres", ""); var sql = connection.createStatement()) {
            sql.execute("SET search_path TO " + schema);
            try (var rows = sql.executeQuery("SELECT interview_document,interview_revision,reviewed_revision,collection_protocol FROM projects WHERE id=101")) {
                assertThat(rows.next()).isTrue(); assertThat(rows.getString(1)).isNull();
                assertThat(rows.getLong(2)).isZero(); assertThat(rows.getLong(3)).isEqualTo(-1);
                assertThat(rows.getString(4)).isEqualTo("LEGACY_UNVERIFIED");
            }
            try (var rows = sql.executeQuery("SELECT answer_text,question_kind,provenance,generation_origin FROM answers JOIN questions ON questions.id=question_id WHERE answers.id=101")) {
                assertThat(rows.next()).isTrue(); assertThat(rows.getString(1)).isEqualTo("Students need private notes");
                assertThat(rows.getString(2)).isEqualTo("INTERVIEW");
                assertThat(rows.getString(3)).isEqualTo("LEGACY_UNKNOWN");
                assertThat(rows.getString(4)).isEqualTo("LEGACY_UNKNOWN");
            }
            try (var rows = sql.executeQuery("SELECT study_enrolled,assignment_method FROM elicitation_sessions WHERE id=101")) {
                assertThat(rows.next()).isTrue(); assertThat(rows.getBoolean(1)).isFalse();
                assertThat(rows.getString(2)).isEqualTo("LEGACY_UNKNOWN");
            }
            try (var rows = sql.executeQuery("SELECT content,source_revision FROM export_artifacts WHERE id=101")) {
                assertThat(rows.next()).isTrue(); assertThat(rows.getString(1)).isEqualTo("Original spec bytes");
                assertThat(rows.getObject(2)).isNull();
            }
        }
    }
}
