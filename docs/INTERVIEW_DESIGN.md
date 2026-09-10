# Interview and SPEC design

Collection protocol: `ideaspec-collection-3`. Record format: `ideaspec-interview-3`. Extraction prompt: `evidence-extraction-3`. Evidence check: `evidence-check-1`. Question prompt: `novice-interviewer-3`.

## Research question and comparison

Does taxonomy-guided interviewing help nontechnical users produce more correct and usable requirements than a strong general-purpose LLM interview?

The treatment is question selection using a requirements taxonomy. Both conditions share onboarding, source extraction, evidence checking, review, rendering, the 24-answer budget and participant-controlled early finish. The baseline receives a substantial adaptive requirements-interview instruction and the original conversation, without the taxonomy, coverage scores, guided record or focus. It is asked to explore purpose, users, tasks, scope, information, privacy, constraints, failures and observable success. The guided questioner also receives the structured record and a prioritized gap.

Neither model can end the interview by returning `done`. Guided gap exhaustion switches to a general clarification focus; it does not trigger early completion. Only the common budget or the participant ends questioning. The session stores its budget and `BUDGET` or `PARTICIPANT` end reason. Actual time and answer count can still differ when participants stop early. Treat those differences as measured outcomes or mediators; this implementation does not impose identical elapsed time or guarantee equal exposure after participant withdrawal.

The guided planner limits repeated probes, distinguishes each capability's workflow and acceptance conditions, and suppresses explicitly deferred topics. Conflicts receive priority. These content choices are part of the intervention. Coverage values are internal diagnostics and must not be used as the primary correctness or usability outcome.

## Novice interaction

Questions use everyday language, ask one thing at a time, and can concern an imagined situation. No prior experience building or specifying software is required. Users can answer briefly, remain unsure, omit a feature, end questioning, and correct a recorded statement. The uncertainty control preserves text already typed. Generated examples are never submitted as stakeholder answers.

Both study arms have wording simplification disabled to keep the same support setting. Ordinary projects may enable it. A model failure or rejected question wording can produce a local fallback question. Every such question stores `generationOrigin`, `generationReason`, `llmCallId`, and `promptVersion`; a 429 receives `QUOTA_OR_RATE_LIMIT`. Local fallback is a protocol deviation, not an equivalent model interview. Failed and unfinished randomized sessions remain in the primary export.

## Source accounting and evidence checking

1. Project creation saves the starting idea and collection protocol. It makes no model call.
2. An answer or correction commits its exact text before model work. Identical duplicate answers are idempotent; a different answer to an already answered question requires a correction.
3. The extractor receives all stakeholder sources, the previous record, and allowed criteria. It returns capability and entry patches plus an explicit disposition for every unchecked source. A disposition is `EXTRACTED` or `NO_CHANGE`, with a reason. Missing, duplicate, unknown or unaccounted sources fail validation. `EXTRACTED` must reference a record in that patch citing the source. Explicit uncertainty must become a deferred decision, not a no-change acknowledgement.
4. The server checks stable IDs, criteria, capability references, resolution states, record bounds, source provenance and exact excerpts. Quotes must retain a complete sentence or complete source line; an isolated word from a longer sentence is rejected. Changed existing records require new source evidence. Omitted records remain intact.
5. A separate model call audits the proposed complete record against the original sources. It checks every capability and entry for support, including negation, conditions, quantifiers, uncertainty and corrections. It also checks every accounted source for omitted decisions. All expected verdicts must be present, unique, explicit booleans and positive. No-change dispositions require this check too.
6. Only after both stages succeed do source checks become verified and their IDs become processed. Their extraction and verification reasons, plus claim-check reasons, are saved with the record revision. On either-stage failure, previous facts and processed sources remain unchanged, new answers remain pending, and confirmation is blocked. A pending assessment cannot generate a completeness snapshot.
7. Provider adapters reject token-limited responses even if the text happens to be valid JSON. Grok Responses also rejects incomplete or failed response statuses.

The verifier is a second call to the same configured model, with a different checking instruction. It is not an independent human judge or a mathematical entailment proof. Correlated mistakes and missed facts remain possible. Adversarial automated tests establish excerpt boundaries and fail-closed handling of negative or missing verifier verdicts; they do not measure the model's accuracy. Live adversarial evaluation and a novice pilot remain necessary. The additional call increases latency and cost in both arms.

Source `IDEA` is the initial description. `A<id>` identifies an immutable saved answer. Questions are context, not evidence. New answers have `STAKEHOLDER` provenance. Interview mutations use short transactions, project locks and expected revisions; stale model responses cannot replace later answers. GET pages and downloads only read saved state.

## Resolution states, review and export

| State | Meaning |
| --- | --- |
| CAPTURED | An explicit stakeholder decision that passed evidence checking; still requires owner review |
| OPEN | A relevant unanswered question |
| DEFERRED | Explicit uncertainty or a choice to decide later |
| NOT_APPLICABLE | An explicit scope exclusion |
| CONFLICT | Incompatible statements awaiting clarification |
| REMOVED | A prior entry explicitly superseded or withdrawn |

Ending the interview moves it to review. The owner sees the original answers, recorded capabilities and statements, excerpts, and open decisions. Corrections append new source answers with question kind `REVIEW`. A failed submission preserves its draft; answer drafts retain their question identity so a saved answer cannot populate a different question. Unexpected create failures return to the project form with the submitted idea.

Confirmation requires the exact current revision, all sources checked, a concrete need, a valid title and explicit owner acknowledgement. It freezes the active overview into the document summary. The exporter uses that frozen summary, performs no LLM call, stores the generated bytes with their source revision, and reuses the artifact for repeated exports. It rejects an incomplete confirmed record. Corrections invalidate current downloads until rechecked and reconfirmed; historical artifacts remain stored.

The SPEC contains active capabilities, captured requirements, exclusions, unresolved decisions, unassessed topics and evidence references. It does not invent implementation choices or add product defaults. Open decisions can remain in a confirmed SPEC; confirmation establishes the owner's intention, not universal implementation readiness.

## Enrollment and model control

Registration creates an ordinary account, not a study participant. Unassigned accounts use ordinary guided interviews and are excluded from the study dataset. An administrator enrolls a participant through **Enroll and randomly assign**. Assignment uses an unbiased secure random Boolean under a user-row lock; it cannot be rerandomized or manually overwritten. The method and condition are frozen on each newly created session.

Manual or external assignments remain available for pilots and controlled external workflows. They are labeled `MANUAL` and appear only in the all-data diagnostic export. Administrators can explicitly preview either condition when creating their own project. An empty selection inherits the account assignment; explicit `GUIDED` overrides a baseline assignment. Preview sessions are labeled `ADMIN_PREVIEW` and are not enrolled study sessions.

Study project creation requires `app.study.strict-model=true`, the provider named in `app.study.provider` (default `OPENAI`), and a nonempty provider model override such as `app.llm.openai.model`. The configured model is saved on the session. Continuing a study interview validates that its model and support settings have not changed. Use a compatible versioned provider model identifier, and validate it before collection; an alias can change remotely despite local strict selection.

OpenAI sends the requested temperature for recognized GPT-3.5, GPT-4, GPT-4.1 and GPT-4o text-model names. Other model names use no temperature override and explicitly record that omission. In particular, the original GPT-5 family rejects temperature overrides, including `gpt-5-mini`; blindly sending the requested value would break those requests. See [OpenAI parameter compatibility](https://developers.openai.com/api/docs/guides/latest-model?model=gpt-5.2). Omitted temperature is not claimed to be zero, and provider-default sampling is not claimed equivalent across providers. Reasoning parameters remain at provider defaults unless already supplied by an adapter; model selection and pilot checks must account for this.

## Audit and dataset boundaries

`llm_call_audits` records call ID, project, phase, prompt version, provider, requested temperature, outcome, elapsed time and attempts with requested/returned model, sent temperature or explicit omission, output limit, finish reason, usage where available and sanitized failure classification. Missing actual model information remains unknown. It stores no prompts, answer text, credentials or generated response bodies. Question call IDs link the applied question to its generation call; unused concurrent calls remain distinguishable in the audit.

Rotating logs retain bounded, sanitized exception messages, causes and stack frames. HTTP exceptions expose status, allowlisted provider error code/status/type, request ID and retry-after, while their raw bodies are omitted. JSON parser payloads and recognizable credentials are redacted. Requirements and excerpts belong in the protected database revision history, not ordinary call logs.

The default JSON/CSV study export contains only current-protocol randomized sessions, including failures and unfinished interviews. It includes an exclusion manifest for other projects and a diagnostic-only label for taxonomy metrics. Do not silently discard fallback sessions after randomization. Prespecify intention-to-treat analysis, missing outcomes, and any secondary analysis of sessions without deviations. Multiple projects from the same participant require a predefined task-selection and repeated-measures policy.

The explicitly labeled all-data export retains manual assignments, ordinary use and legacy projects for diagnostics. Do not pool it as one experiment. CSV archives carry a manifest, provenance and assignment fields, revision history, and model-call records.

## Legacy migration

V10 is additive and does not change V9's checksum. Every project already present at migration is labeled `LEGACY_UNVERIFIED`; existing answers and questions become `LEGACY_UNKNOWN`, and existing sessions are not enrolled. This includes records created by the earlier protocol-2 implementation. Their original data and artifacts remain unchanged.

Collection provenance is separate from JSON format. Reading a null document cannot turn an old collection session into a protocol-3 interview. The application blocks extraction, answers, corrections, confirmation and new export of legacy collections. The review page explains the boundary and offers a new project in the owner's own words. It does not invite a novice to approve a batch of potentially generated legacy answers as new evidence. All-data diagnostics preserve historical exports; the current reviewed download remains gated.

## Evaluation still required

Use realistic tasks with independent stakeholder decision sheets or an independently established record of intended decisions. Blind raters to condition and measure correct supported decisions, invented or contradicted behavior, important omissions, usable acceptance conditions, ambiguity and implementer clarification needs. Record time, answer count, uncertainty, correction effort, fallback exposure and participant burden separately.

Evaluate both the `INTERVIEW_ENDED` revision and the final `REVIEW_CONFIRMED` revision because the common review may change the observed effect. An ended interview with pending extraction is a failed or missing assessed outcome, not a complete document. Preserve it in intention-to-treat accounting.

Pilot terse answers, uncertainty, multiple features, changing one's mind, explicit exclusions, contradictions, misleading quotations and provider failures with representative beginners. The software supports this evaluation; passing tests cannot establish that taxonomy guidance improves requirements.
