# Nous Sight

Conversational Intelligence platform for TOTVS. Nous Sight turns sales-meeting
transcripts into structured, governed insights: every insight extracted from
a transcript gets a **confidence score**, and anything below 80% confidence
is routed to a human validation step before it enters the client's official
history — instead of being trusted blindly as an "AI black box."

## Business problem

Ricardo, a Sales Manager, loses roughly 20% of his pipeline because he has no
visibility into what's actually said in sales meetings — competitor
mentions, product interest, technology concerns all go unnoticed until it's
too late. Nous Sight scans meeting transcripts for terms from a curated
dictionary (competitors, own products, technology), raises alerts with a
confidence score, and tracks churn risk per client based on how often rival
mentions show up over time.

## Architecture

Spring Boot + Spring Data JPA + Lombok, following a strict
`entity → repository → service → controller` layering:

```
src/main/java/br/com/fiap/nous_sight/
├── entity/       Department, Participant, Meeting, MeetingParticipant,
│                 DictionaryTerm, TranscriptLine, GeneratedAlert
├── repository/   JpaRepository interfaces with derived finders
├── service/      AlertService, ClientHistoryService, MeetingService
├── controller/   REST controllers (MeetingController, AlertController,
│                 ClientHistoryController)
└── dto/          MeetingSummaryDTO, AlertValidationRequest
```

Physical table/column names and stored domain values (`'Pendente'`,
`'Validado'`, `'Corrigido'`, `'Time Rival'`, `'Organizador'`, ...) stay in
**Portuguese**, matching the real Oracle schema. Java identifiers stay in
**English**; every `@Column`/`@JoinColumn` maps explicitly to its Portuguese
physical name.

## Running locally (H2)

H2 is the default profile — no setup needed.

```bash
mvn spring-boot:run
```

- In-memory database: `jdbc:h2:mem:nous_sight`
- H2 console: http://localhost:8080/h2-console
- `spring.jpa.hibernate.ddl-auto=update` — schema is created/updated
  automatically from the entities.

Run the test suite with:

```bash
mvn test
```

## Switching to Oracle (delivery)

1. Fill in the placeholders in `src/main/resources/application-oracle.properties`
   (`<host>`, `<port>`, `<sid>`, `<username>`, `<password>`) with the real
   Oracle connection details.
2. Run with the `oracle` profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=oracle
```

`spring.jpa.hibernate.ddl-auto=none` on this profile — the schema is the one
already delivered by the Database Design discipline; Hibernate must not try
to generate or alter it.

## Business rules

| # | Method | Class | What it does |
|---|--------|-------|---------------|
| 4.1 | `needsHumanValidation(GeneratedAlert)` | `AlertService` | Returns `true` when `confidenceScore < 80` — flags an insight as needing a human to check it before it's trusted. |
| 4.2 | `registerHumanValidation(Long alertId, boolean confirmed, String correctedNote)` | `AlertService` | Records the outcome of a human review: sets `validationStatus` to `"Validado"` if confirmed, or `"Corrigido"` (with the corrected note) if not. Throws if the alert was already reviewed. |
| 4.3 | `calculateAverageConfidenceScore(Long meetingId)` | `AlertService` | Arithmetic mean of `confidenceScore` across every alert generated from a meeting's transcript; `0` if the meeting has no alerts. |
| 4.4 | `calculateChurnRisk(Meeting meeting)` | `ClientHistoryService` | Counts `"Time Rival"` alerts for the meeting in the last 30 days and classifies risk as `Low` (0–1), `Medium` (2–3), or `High` (4+). Computed, not stored, so the labels stay in English. |
| 4.5 | `generateMeetingSummary(Long meetingId)` | `ClientHistoryService` | Aggregates a meeting into a `MeetingSummaryDTO`: total transcript lines, total alerts, average confidence score (reuses 4.3), and alert terms grouped by category. |

## REST endpoints

- `/meetings` — full CRUD
- `/alerts` — full CRUD, plus `POST /alerts/{id}/validate` for rule 4.2
- `/clients/{id}/history/churn-risk` — rule 4.4 (`{id}` is the meeting id)
- `/clients/{id}/history/summary` — rule 4.5

All endpoints return `ResponseEntity`, with business-rule violations caught
and returned as `400 Bad Request` with the exception message as the body.
