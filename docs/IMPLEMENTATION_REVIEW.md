# Implementation review — 2026-09-10

This records the initial protocol-2 implementation. Its evidence, stopping, logging and migration limitations were subsequently corrected in [Defect review](DEFECT_REVIEW.md). The current behavior is specified in [Interview design](INTERVIEW_DESIGN.md); the initial verification below is historical.

## Assessment of the project

The existing Spring Boot monolith is a viable research prototype: authentication, owned projects, provider credentials, interview sessions, stored questions and answers, requirement slots, completeness snapshots, and administrative exports already supply most of the infrastructure. PostgreSQL with Flyway and the existing web framework are sufficient for the proposed study; a framework rewrite would not address the main validity risks.

The main risks were in the meaning of the recorded requirements. The previous pipeline could treat generated examples as user answers, lose refinements when a coverage status did not increase, cover a category from only one feature's detail, and add roles during export. A final document generated through a separate synthesis step could also diverge from what the owner thought they had confirmed. These behaviors weaken both the product and the proposed comparison.

The review covered the project/session model, taxonomy, interviewing and assessment prompts, answer persistence, export path, provider adapters and retries, user and admin controllers, templates, credential encryption and configuration, study exports, database migrations, and existing tests.

## Changes and reasons

| Area | Change | Reason |
| --- | --- | --- |
| Novice interaction | Neutral single questions, imagined-situation help, uncertainty control, accessible wording button | Avoid requiring prior software experience or encouraging invented technical choices |
| Question planning | Adaptive gaps, explicit applicability, per-capability behavior, bounded probes | Ask useful missing details without equating one described feature with the whole product |
| Evidence | Atomic records, stable IDs, source excerpts, full-transcript patches | Preserve refinements and make interpretation reviewable |
| Review | Correct individual statements or add general corrections; explicit revision confirmation | Allow the owner to repair misunderstandings before export |
| Export | Pure deterministic rendering from the reviewed revision | Eliminate unsourced enrichment and repeated regeneration differences |
| Research control | Real baseline prompt, ordinary participant assignments, model overrides and strict model selection | Initial comparison support; stopping and assignment defects remained and are addressed in the subsequent review |
| Failure recovery | Commit answers before model calls; short locked writes; revision checks | Preserve work during outages and reject stale concurrent writes |
| Read operations | GET pages and downloads read saved state | Avoid generating or changing a project on refresh or link navigation |
| Auditability | Revision history, source/export IDs in research datasets, rotating correlated call logs | Trace what changed and separate model failures from interview outcomes |
| Local safety | Loopback local binding, disabled passwordless shortcut, local-only demo seeding, explicit production settings | Remove development shortcuts from the default exposed workflow while preserving existing local encrypted keys |
| Test infrastructure | Real web/service tests and PostgreSQL migration/upgrade verification | Exercise persistence and user flows that the earlier startup test did not cover |

The old guided planner, prose slot assessor, and export role-enrichment service were replaced, along with their implementation-specific tests. The existing historical database columns remain for compatibility. No source-code comments were added.

## Defects found while implementing and testing

- H2 treated the historical `value` column as a keyword, so the requirement-slot table could fail to create while the old context-loading test still appeared successful. The H2 test URL now handles that existing column name and schema creation fails fast on errors.
- A stale assessment could overwrite a later answer without a document revision check. Project row locks and expected revisions now guard writes.
- Duplicate answers could race at persistence boundaries. Concurrent identical submissions are now serialized and idempotent.
- An explicitly excluded capability could remain in active requirements or the summary. Active scope is now used consistently during rendering and slot projection.
- Multiple open decisions sharing one criterion could collapse into one export line. The exporter retains every explicit unresolved statement.
- A later deferred item could hide a recorded conflict. The planner now preserves conflict priority.
- A summary based on the starting idea could reintroduce superseded behavior. Summaries now use the active record.
- An uncertainty button could discard a partially typed answer. The text is now retained alongside the uncertainty statement.
- Failure after committing an answer could refill the next question with the old answer. Progression failures now reload saved state without presenting committed text as a new answer draft.
- The first privacy change retained only endpoint/status or exception class. It removed too much diagnostic information; the subsequent defect repair restores sanitized messages and cause stacks with allowlisted provider codes.
- The existing study export omitted the new record history and source revision. JSON and CSV now include these fields and revision records.

## Verification evidence

The standard suite checks extraction and evidence rejection, preservation of previous facts, capability-specific gaps, explicit deferrals and conflicts, deterministic SPEC behavior, scope removal, stable export IDs, provider contracts, fixed-model failure behavior, sanitized logs, and the existing authentication/profile/administration validation tests.

The web/service integration suite uses the actual application context, repositories, security filters, Thymeleaf views, and exporter. Only the credential/provider service is mocked. It covers the beginner flow, owner acknowledgement, partial uncertainty answers, saved-answer recovery, duplicate and concurrent submissions, stale model responses, stale review rejection, correction history, export invalidation, read-only downloads, ownership, CSRF, condition assignment, administrator exports, and legacy review.

The same integration suite can run against PostgreSQL with Flyway and Hibernate schema validation. An additional migration test first installs version 8, inserts an existing completed project with an answer and export, applies version 9, and checks that the old data and required defaults survive. The verification script creates and stops its own isolated cluster and preserves its run reports.

Machine-readable results and HTML reports are under `build/test-results/test` and `build/reports/tests/test`. The PostgreSQL script preserves separate copies under its `build/postgres-verification-<run-id>` directory. See `docs/VERIFICATION.md` for the final run results.

## Remaining validation

No production database, real provider credentials, or live provider calls were used for the tests. Model response quality and participant usability have not been measured by these changes. The next research step is a small observed pilot, followed by a preregistered comparison using independent correctness and usability judgments. Coverage scores and passing software tests must not be presented as evidence that the taxonomy outperforms the baseline.
