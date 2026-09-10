# IdeaSpec

IdeaSpec helps a person describe a software idea, answer questions in everyday language, review the recorded requirements, and download `SPEC.md`.

The research question is: **Does taxonomy-guided interviewing help nontechnical users produce more correct and usable requirements than a strong general-purpose LLM interview?** The application now supports both interview conditions with a shared evidence extractor, review process, and deterministic exporter. Improved software behavior is not evidence that the research hypothesis is true; a participant study is still needed.

## Run locally

Requires a Java 21 toolchain and PostgreSQL. The Gradle wrapper is included.

The local profile connects to `jdbc:postgresql://127.0.0.1:5432/thesis_app` with the existing development credentials `thesis` / `thesis`. Override Spring datasource properties for a different database. Flyway applies schema migrations at startup.

```powershell
.\gradlew.bat bootRun
```

Open `http://127.0.0.1:8080`. The local profile binds to loopback, and passwordless admin login and automatic browser launch are disabled. Local seeding creates `admin@thesis.local` / `admin123` and `user@thesis.local` / `user123` only if missing. These accounts are for local development. Existing accounts and encrypted provider keys are retained.

Connect a provider in Settings, create a project, and select **Check notes and continue**. Answers are saved before model calls. **I'm not sure yet** also preserves any partial answer already typed. **Review what we have** ends questioning early. Review and correct the recorded needs, acknowledge the open decisions, then confirm and download the SPEC.

## Verify

```powershell
.\gradlew.bat test bootJar
.\scripts\test-postgres.ps1
```

The ordinary suite uses H2 and mocked provider calls. The PostgreSQL script creates an isolated cluster on port 55439, runs the web/service integration tests against real migrations, verifies an upgrade from version 8, and stops the cluster. It uses PostgreSQL 18 at `C:\Program Files\PostgreSQL\18\bin` by default; parameters allow another installation or port. It does not connect to the application database.

Standard reports: `build/reports/tests/test/index.html`. PostgreSQL logs, reports, and isolated test data: `build/postgres-verification-<run-id>/`. No real provider credentials are required for these tests.

## Research setup

An administrator enrolls a participant through **Enroll and randomly assign** on the participant's admin page. Randomized assignments are fixed; their condition and method are frozen on new sessions. Registration alone creates an ordinary account. Unassigned guided use, manual assignments and explicit administrator previews are excluded from the default study dataset. The administrator's project form has explicit guided and baseline overrides; an empty choice inherits the account assignment.

Study projects require `app.study.strict-model=true`, one `app.study.provider` (default `OPENAI`), and a configured compatible model snapshot in `app.llm.openai.model`, `app.llm.anthropic.model`, `app.llm.gemini.model`, or `app.llm.grok.model`. Environment equivalents include `APP_LLM_OPENAI_MODEL` and `APP_STUDY_STRICT_MODEL`. The chosen model is stored on the session and rechecked when continuing; wording simplification is disabled for both study arms. Validate the provider contract and quality before collection. The inexpensive development defaults are not a claim that the baseline uses the strongest available model.

Both conditions use the same 24-answer budget, input controls, source extraction, separate evidence check, correction process, and SPEC renderer. Neither a model `done` response nor guided gap exhaustion ends the interview. Both permit participant-controlled early finish and record its reason. The baseline receives the conversation and a general requirements-interview instruction without taxonomy, scores, or guided focus. The shared review exposes open topics after questioning ends.

Default study JSON and CSV contain current-protocol randomized sessions, including unfinished sessions and generation failures, with an exclusion manifest. The labeled all-data diagnostic exports preserve the other collections. Exports include original answers and provenance, question origin and failure reason, evidence-check verdicts, revisions, model-call records and export revision IDs. Taxonomy scores are explicitly diagnostic. See `docs/INTERVIEW_DESIGN.md` and `docs/DEFECT_REVIEW.md` for design choices and limitations.

V10 preserves existing answers and artifacts but labels all pre-upgrade projects as legacy collections, including protocol-2 projects. It blocks their re-extraction as verified human evidence. Open the historical review or all-data diagnostic export to inspect them; start a new project in the owner's own words for new collection. Collection provenance is stored separately from document format.

## Logs and deployment

`logs/app-steps.log` contains rotating audit events with sanitized exception messages, causes and stack frames. HTTP diagnostics retain status, allowlisted provider error codes, request IDs and retry-after without raw response bodies. The `llm_call_audits` table records requested versus sent temperature (including explicit omission), model identifiers, output limits, finish reasons, usage and failures. OpenAI temperature is sent only for supported model families; `gpt-5-mini` keeps its provider default, explicitly recorded as an omission. Unknown settings are not reported as zero. Prompts, answers and credentials are excluded from call metadata. Requirements and verification reasons remain in the protected revision history.

For deployment, explicitly select the `prod` profile and provide `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, and a strong `APP_ENCRYPTION_SECRET`. Production does not seed demo accounts. Provision an administrator through your controlled database administration process. Keep the encryption secret stable for existing credentials; changing it without re-encrypting stored keys makes them unreadable. The local fallback encryption secret is retained only for compatibility with existing local installations.

The automated checks verify implementation behavior, database compatibility, and mocked provider contracts. They do not validate real model output quality, participant usability, or the thesis hypothesis.
