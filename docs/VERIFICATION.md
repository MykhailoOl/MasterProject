# Verification — defect repair, 2026-09-10

| Check | Result |
| --- | --- |
| `gradlew test bootJar` | Passed; executable application JAR produced |
| Standard test suite | 123 discovered: 121 passed, 0 failed, 0 errors; 2 PostgreSQL-only migration cases skipped |
| PostgreSQL verification | 15 passed, 0 failed, 0 errors, 0 skipped |
| PostgreSQL web/service integration | 13 integration cases rerun with real Flyway migrations and Hibernate schema validation |
| Existing-data upgrade | V8 → V10 and V9 → V10; old answers, confirmed protocol-2 record and stored artifacts preserved with legacy provenance |
| Patch whitespace check | `git diff --check` passed |
| Obsolete runtime references | No `fast-finish-form`, `/elicit/fast-finish` or `optionalCategories` references remain in `src` |
| Temporary database | Isolated PostgreSQL cluster stopped; `data/postmaster.pid` absent |

The PostgreSQL total includes reruns of 13 standard integration cases plus the 2 migration cases. It is not 15 additional unique scenarios. No application database or live provider credentials were used.

## Saved results

- Standard HTML report: `build/defect-verification-final/standard-report/index.html`
- Standard JUnit XML: `build/defect-verification-final/standard-test-results/`
- Standard result summary: `build/defect-verification-final/standard-summary.json`
- Standard command completion log: `build/defect-verification-final/standard-run.log`
- Preserved standard audit log: `build/defect-verification-final/standard-audit.log`
- PostgreSQL HTML report: `build/postgres-verification-15c3608b565a4c06a8cc69cd71a6f54c/report/index.html`
- PostgreSQL JUnit XML: `build/postgres-verification-15c3608b565a4c06a8cc69cd71a6f54c/test-results/`
- PostgreSQL execution log: `build/postgres-verification-15c3608b565a4c06a8cc69cd71a6f54c/verification.log`
- PostgreSQL server log: `build/postgres-verification-15c3608b565a4c06a8cc69cd71a6f54c/postgres.log`
- PostgreSQL result summary: `build/defect-verification-final/postgres-summary.json`
- Current test audit/application logs: `build/test-logs/app-steps.log`, `build/test-logs/application.log`
- Built application: `build/libs/MasterProject-0.0.1-SNAPSHOT.jar`
- JAR SHA-256: `047E35D8A80312B73FDA78788CC231A1ECCE8306AE28A68B4AC2A7F33BE00B1E`

Build outputs and test data are ignored by Git. Preserved report copies keep the standard results separate from the subsequent PostgreSQL-only run.

An initial composite verification command was blocked before tests when the sandbox denied the Gradle wrapper cache lock. Its folder, `build/defect-verification-cf341445df184bda86fc92cfe9c49a2c`, is explicitly labeled `NOT_A_TEST_RUN.txt`; its copied XML is stale and must not be counted. The final command succeeded using the already-authorized Gradle execution outside that sandbox. Earlier development failures included a Gemini trace-variable compile error, an obsolete logging assertion, a test assertion with a wildcard map type, and a nested-JSON ID assertion; these were repaired before the final runs.

## What was checked

The integration suite uses actual repositories, transactions, locking, security filters, controller bindings, Thymeleaf templates and export services. Only the credential/provider service is mocked there. Adapter and audit tests also exercise real request construction against mock HTTP servers, including supported versus omitted temperature, model fallback traces, token-limit termination and persistent call-audit capture.

New regressions cover empty and incomplete extraction, explicit no-change accounting, missing/negative verifier verdicts, misleading quote fragments, full-quote semantic rejection, legacy-source blocking, success-snapshot gating, baseline early-stop rejection, common budget stopping, guided planner exhaustion, enrollment immutability, guided override of baseline, retention of randomized quota failures in study data, legacy/ordinary exclusions, draft recovery, exact frozen SPEC summary bytes and correction/reconfirmation exports. Existing concurrency, ownership, CSRF, no-generated-answer, scope, deferral, conflict and rendering tests still pass.

Logging tests verify useful diagnostic content and cause stacks while checking that HTTP bodies, credentials and recognizable secrets do not enter those diagnostics. PostgreSQL tests verify the additive schema change and ensure older reviewed data cannot become new-protocol collection evidence through a format migration.

## Limits

There were no live model quality tests, visual browser inspection or sessions with novice participants. Verifier responses in tests are controlled fixtures: they prove rejection/acceptance gates and persistence behavior, not the LLM's ability to judge semantic support. The separate checker adds latency and cost and may share the extractor's mistakes. The research hypothesis, model choice, error rates and beginner usability still require an observed pilot and independent blinded assessment.

See [Defect review](DEFECT_REVIEW.md) for the nine-point repair map and [Interview design](INTERVIEW_DESIGN.md) for the current collection and analysis rules. Previous protocol-2 verification remains archived under `build/interview-verification-final`; it is superseded by the results above.
