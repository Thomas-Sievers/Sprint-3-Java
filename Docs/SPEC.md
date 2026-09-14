# SPEC — Nous Sight | Sprint 3 (Domain Driven Design – Java)

> Technical specification to guide the Java implementation via Claude Code,
> following Spec Driven Design: define the full contract first (entities,
> business rules, repository, service, controller, tests), then generate the
> code from it.
>
> Architecture: **Spring Boot + Spring Data JPA + Lombok**, in the
> `entity → repository → service → controller` pattern, following the
> team's established conventions and best practices. Tests with
> **JUnit 5 + Mockito**, mocking the repository at the service layer.

> **⚠ Language note:** all Java identifiers (classes, fields, methods) and
> prose in this document are in English. The **physical database table and
> column names**, and the **stored domain values** (e.g. `'Pendente'`,
> `'Validado'`, `'Corrigido'`, `'Time Rival'`, `'Organizador'`) stay in
> **Portuguese**, because they already exist in the real Oracle schema built
> by the Database Design teammate. Every entity below maps its English field
> to the original Portuguese column with an explicit `@Column(name = "...")`
> — do not let Hibernate's default naming strategy guess the column name, or
> it will silently fail to match the real schema. Any Java code that
> compares a domain value (`validationStatus`, `category`,
> `participationType`) must compare against the **Portuguese** literal
> stored in the database, not an English translation of it.

## 0. Business Context (summary)

**Nous Sight** is a Conversational Intelligence platform for TOTVS that turns
sales-meeting transcripts into structured, actionable data with governance
and transparency: every extracted insight gets a **confidence score**, and
insights with confidence **below 80%** are routed to a **human validation**
form before entering the client's official history.

Main persona: **Ricardo, Sales Manager** — loses ~20% of his pipeline due to
lack of visibility into competitor mentions during meetings.

## 1. Stack and Conventions

- **Base package:** `br.com.fiap.nous_sight` (adjust to the group's actual convention)
- **Dependencies:** `spring-boot-starter-data-jpa`, `spring-boot-starter-web`,
  `lombok`, `spring-boot-starter-test`, Oracle driver (`ojdbc17`), H2 for
  local development (H2 in dev, Oracle in production/delivery).
- **Entities:** `@Entity`, `@Table(name = "...")`, `@Getter @Setter
  @NoArgsConstructor @AllArgsConstructor @Builder @ToString @EqualsAndHashCode`,
  `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)`.
- **Repository:** interface `extends JpaRepository<Entity, Long>`, with
  derived query methods (`findByXxx`) whenever possible — avoid hand-written
  SQL, let Spring Data generate the query.
- **Service:** `@Service` class, constructor-based dependency injection
  (no explicit `@Autowired`), business rules as private validation methods
  called from a central `validate(...)`, exceptions as `RuntimeException`
  with a descriptive message.
- **Controller:** `@RestController`, `@RequestMapping("/resource")`,
  endpoints `GET / GET{id} / POST / PUT{id} / DELETE{id}`, returning
  `ResponseEntity<?>` with `try/catch (RuntimeException e) →
  ResponseEntity.badRequest().body(e.getMessage())`.
- **Tests:** `@ExtendWith(MockitoExtension.class)`, `@Mock` on the
  repository, service instantiated manually in `@BeforeEach`, one test per
  business rule (`assertThrows` / `assertDoesNotThrow` / `verify(repository,
  never())...`).

## 2. Domain Model (`entity` layer)

Based on the relational model already defined in the Database Design
discipline (Oracle). Relationships use standard JPA annotations
(`@ManyToOne`, `@JoinColumn`) instead of manual FKs.

### 2.1 Department (table `DEPARTAMENTO`)
```java
@Entity
@Table(name = "DEPARTAMENTO")
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_DEPARTAMENTO")
    private Long id;

    @Column(name = "NOME_DEPARTAMENTO", nullable = false)
    private String departmentName;

    @Column(name = "SIGLA", nullable = false, unique = true)
    private String acronym;
}
```

### 2.2 Participant (table `PARTICIPANTE`)
```java
@Entity
@Table(name = "PARTICIPANTE")
public class Participant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_PARTICIPANTE")
    private Long id;

    @Column(name = "NOME", nullable = false)
    private String name;

    @Column(name = "EMAIL", nullable = false, unique = true)
    private String email;

    @ManyToOne
    @JoinColumn(name = "ID_DEPARTAMENTO", nullable = false)
    private Department department;
}
```

### 2.3 Meeting (table `REUNIAO`)
```java
@Entity
@Table(name = "REUNIAO")
public class Meeting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_REUNIAO")
    private Long id;

    @Column(name = "TITULO", nullable = false)
    private String title;

    @Column(name = "DATA_HORA_INICIO", nullable = false)
    private LocalDateTime startDateTime;

    @Column(name = "DATA_HORA_FIM")
    private LocalDateTime endDateTime; // optional; if present, must be > startDateTime
}
```

### 2.4 MeetingParticipant (table `REUNIAO_PARTICIPANTE`, associative with its own attribute)
```java
@Entity
@Table(name = "REUNIAO_PARTICIPANTE")
public class MeetingParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ID_REUNIAO", nullable = false)
    private Meeting meeting;

    @ManyToOne
    @JoinColumn(name = "ID_PARTICIPANTE", nullable = false)
    private Participant participant;

    @Column(name = "TIPO_PARTICIPACAO", nullable = false)
    private String participationType;
    // stored values (Portuguese, keep as-is): "Organizador" | "Ouvinte" | "Apresentador"
}
```
> A "pure" N:N relationship (no extra attribute) is usually modeled with
> `@ManyToMany`, but here we need a dedicated entity because
> `participationType` belongs to the relationship itself, not to either
> entity — hence `@ManyToOne` on both sides instead of `@ManyToMany`.

### 2.5 DictionaryTerm (table `DICIONARIO_TERMOS`)
```java
@Entity
@Table(name = "DICIONARIO_TERMOS")
public class DictionaryTerm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_TERMO")
    private Long id;

    @Column(name = "PALAVRA_CHAVE", nullable = false, unique = true)
    private String keyword;

    @Column(name = "CATEGORIA", nullable = false)
    private String category;
    // stored values (Portuguese, keep as-is): "Time Rival" | "Produto Próprio" | "Tecnologia"
}
```

### 2.6 TranscriptLine (table `FALA_TRANSCRICAO`)
```java
@Entity
@Table(name = "FALA_TRANSCRICAO")
public class TranscriptLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_TRANSCRICAO")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ID_REUNIAO", nullable = false)
    private Meeting meeting;

    @ManyToOne
    @JoinColumn(name = "ID_PARTICIPANTE", nullable = false)
    private Participant participant;

    @Column(name = "HORA_FALA", nullable = false)
    private LocalDateTime spokenAt;

    @Column(name = "TEXTO_FALA", nullable = false, length = 4000)
    private String text;
}
```

### 2.7 GeneratedAlert (table `ALERTA_GERADO`)
```java
@Entity
@Table(name = "ALERTA_GERADO")
public class GeneratedAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_ALERTA")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ID_TRANSCRICAO", nullable = false)
    private TranscriptLine transcriptLine;

    @ManyToOne
    @JoinColumn(name = "ID_TERMO", nullable = false)
    private DictionaryTerm term;

    @Column(name = "OBSERVACAO", length = 200)
    private String note;

    @Column(name = "SCORE_CONFIANCA", nullable = false)
    private Integer confidenceScore; // 0–100

    @Column(name = "STATUS_VALIDACAO", nullable = false)
    private String validationStatus;
    // stored values (Portuguese, keep as-is): "Pendente" | "Validado" | "Corrigido"
}
```

### 2.8 ✅ Database model confirmed

The `SCORE_CONFIANCA` and `STATUS_VALIDACAO` columns have already been added
to the official `ALERTA_GERADO` DDL by the Database Design discipline, with
the parameters below — database, Java, and the Figma prototype are
consistent with each other.

- **SCORE_CONFIANCA (NN): NUMBER(3)** - AI confidence score for the insight,
  from 0 to 100. Drives the human-validation rule (score < 80 → pending).
- **STATUS_VALIDACAO (NN): VARCHAR2(15)** - Human validation status of the
  alert. Closed domain: `'Pendente'`, `'Validado'` or `'Corrigido'`.

## 3. `repository` Layer

One interface per entity, `extends JpaRepository<Entity, Long>`, with the
derived finders needed for the business rules:

```java
public interface GeneratedAlertRepository extends JpaRepository<GeneratedAlert, Long> {
    List<GeneratedAlert> findByValidationStatus(String status);
    List<GeneratedAlert> findByTerm_CategoryAndTranscriptLine_Meeting(String category, Meeting meeting);
}

public interface TranscriptLineRepository extends JpaRepository<TranscriptLine, Long> {
    List<TranscriptLine> findByMeeting(Meeting meeting);
}

public interface MeetingRepository extends JpaRepository<Meeting, Long> {}
public interface ParticipantRepository extends JpaRepository<Participant, Long> {}
public interface DepartmentRepository extends JpaRepository<Department, Long> {}
public interface DictionaryTermRepository extends JpaRepository<DictionaryTerm, Long> {}
```

This already delivers the full CRUD (inherited from `JpaRepository`) required
by the assignment — `save`, `findById`, `findAll`, `deleteById` — without
writing a manual DAO.

## 4. `service` Layer — Business Method Contracts

The assignment requires at least 4 methods — the list below has 5, pick the
ones that fit your time budget best (the first 4 are the most representative
of the product's value proposition). They follow the pattern from section 1:
private rule methods + a public `save`/`update` that validates before
persisting.

### `AlertService`

**4.1 `needsHumanValidation(GeneratedAlert alert): boolean`**
Rule: returns `true` if `confidenceScore < 80`. This is the core data
governance rule of the product (avoids the "AI black box" problem).

**4.2 `registerHumanValidation(Long alertId, boolean confirmed, String correctedNote): GeneratedAlert`**
Rule: sets `validationStatus` to `"Validado"` (if `confirmed = true`) or
`"Corrigido"` (if `false`, using `correctedNote`) — **values stay in
Portuguese to match the stored domain**. Throws `RuntimeException` if the
alert has already been validated.

**4.3 `calculateAverageConfidenceScore(Long meetingId): double`**
Rule: arithmetic mean of `confidenceScore` across all `GeneratedAlert`
records linked to that meeting's transcript lines. Returns 0 if there are no
alerts.

### `ClientHistoryService` (or inside `MeetingService`)

**4.4 `calculateChurnRisk(Meeting meeting): String`**
Rule: counts how many `GeneratedAlert` records linked to the client's
meetings have `DictionaryTerm.category = "Time Rival"` in the last 30 days.
- 0–1 mentions → `"Low"`
- 2–3 mentions → `"Medium"`
- 4+ mentions → `"High"`
*(This is a computed, non-persisted value — safe to use English labels here,
unlike the DB-backed domain values above.)*

**4.5 `generateMeetingSummary(Long meetingId): MeetingSummaryDTO`**
Rule: aggregates every `TranscriptLine` and `GeneratedAlert` for a meeting
and returns a DTO with: total transcript lines, total alerts generated,
average confidence score (reuses 4.3), and a list of terms by category.

## 5. `controller` Layer

For Sprint 3, the assignment requires functional CRUD — it does not require
a full REST API yet (that carries more weight in **Sprint 4**: "RESTful API
with all necessary endpoints"). Still, following the group's convention, it's
worth creating the basic controllers now — it reduces rework in Sprint 4.

```java
@RestController
@RequestMapping("/meetings")
public class MeetingController {
    private final MeetingService service;
    public MeetingController(MeetingService service) { this.service = service; }

    @GetMapping
    public List<Meeting> list() { return service.list(); }

    @GetMapping("/{id}")
    public ResponseEntity<?> search(@PathVariable Long id) { ... }

    @PostMapping
    public ResponseEntity<?> post(@RequestBody Meeting meeting) { ... }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Meeting meeting) { ... }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) { ... }
}
```
Replicate for `/alerts` (with an extra `POST /alerts/{id}/validate` endpoint
for method 4.2) and `/clients/{id}/history` (methods 4.4 and 4.5).

## 6. "Connection Layer" — adapted for Spring Data JPA

The assignment asks for a "connection class with username and password in
the code". Since the group uses Spring Data JPA (not raw JDBC), this
requirement is satisfied by `application.properties` — which is
version-controlled source code in the project, just declarative instead of a
Java class:

```properties
# src/main/resources/application-oracle.properties
spring.datasource.url=jdbc:oracle:thin:@<host>:<port>:<sid>
spring.datasource.username=<username>
spring.datasource.password=<password>
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver
spring.jpa.hibernate.ddl-auto=none
```

Keep the H2 profile (`application.properties`) for local
development/testing, and the Oracle profile for the delivery.

> ⚠ If the professor requires a literal Java connection class (raw JDBC, as
> the assignment text literally describes), it's quick to add a simple
> `ConnectionFactory` just to satisfy the letter of the assignment — ask
> before doing this, to avoid duplicate work.

## 7. Tests (`src/test/java`)

Follow the pattern already used by the group: `@ExtendWith(MockitoExtension.class)`,
mocked repository, service instantiated in `@BeforeEach`, one `@Test` per rule.

```java
@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private GeneratedAlertRepository repository;

    private AlertService service;

    @BeforeEach
    void setUp() {
        service = new AlertService(repository);
    }

    @Test
    void needsHumanValidation_returnsTrueForScoreBelow80() {
        GeneratedAlert alert = GeneratedAlert.builder().confidenceScore(68).build();
        assertTrue(service.needsHumanValidation(alert));
    }

    @Test
    void registerHumanValidation_rejectsAlreadyValidatedAlert() {
        // ...mock findById returning an alert with validationStatus = "Validado"
        // ...assertThrows(RuntimeException.class, () -> service.registerHumanValidation(...))
    }

    // + tests for calculateChurnRisk, calculateAverageConfidenceScore, generateMeetingSummary
}
```

## 8. Package Structure

```
src/main/java/br/com/fiap/nous_sight/
├── NousSightApplication.java
├── entity/        (section 2)
├── repository/     (section 3)
├── service/        (section 4)
└── controller/      (section 5)
src/main/resources/
├── application.properties         (H2 — dev/local)
└── application-oracle.properties  (Oracle — delivery, section 6)
src/test/java/br/com/fiap/nous_sight/service/
└── AlertServiceTest.java, ClientHistoryServiceTest.java  (section 7)
README.md  (project overview — see section 11)
```

## 9. Assignment Compliance Checklist (Sprint 3 – DDD Java)

- [ ] Complete Model/Entity layer following patterns covered in class (20 pts) → §2
- [ ] 4+ business methods with real logic, not CRUD in disguise (20 pts) → §4
- [ ] Test class instantiating and testing the methods (10 pts) → §7
- [ ] Connection class/config with username/password in the code (10 pts) → §6
- [ ] Fully functional CRUD (20 pts) → §3 (repository) + §5 (controller)
- [ ] PDF document: cover page, table of contents, objective/scope,
      features, prototype (Figma screens), database model, updated class
      diagram (20 pts)

## 10. Claude Code Prompt Roadmap

Paste this entire SPEC.md along with each prompt, one layer at a time:

1. *"Based on the attached SPEC.md, create the entities from section 2,
   following exactly the Lombok/JPA annotation style described in section 1
   (Getter, Setter, NoArgsConstructor, AllArgsConstructor, Builder,
   ToString, EqualsAndHashCode). Make sure every `@Column`/`@JoinColumn` uses
   the explicit Portuguese physical name from section 2, even though the
   Java field names are in English."*
2. *"Now create the repository interfaces from section 3, extends
   JpaRepository, with the derived finders indicated."*
3. *"Implement the services from section 4 (AlertService and
   ClientHistoryService), following the private-validation +
   RuntimeException pattern described in section 1. Keep domain value
   comparisons (validationStatus, category, participationType) in
   Portuguese, matching what's actually stored in the database — only the
   churn-risk labels in 4.4 should be in English, since they're computed,
   not stored."*
4. *"Create the controllers from section 5, following exactly the REST
   pattern described in section 1 (ResponseEntity, try/catch RuntimeException)."*
5. *"Configure the two datasource profiles from section 6 (H2 dev + Oracle
   delivery) and create the JUnit5+Mockito tests from section 7, in the same
   style described in section 1."*
6. *"Write a README.md at the project root explaining the project: what
   Nous Sight does, the business problem it solves, the architecture
   (entity/repository/service/controller), how to run it locally with H2,
   how to switch to the Oracle profile for delivery, and a short description
   of each business rule from section 4."*

Working this way, one layer at a time with a review between each step,
keeps Claude Code from generating everything at once in a way that drifts
from what the database model or the rest of the group agreed on.

## 11. README.md (requested separately, see prompt 6 above)

The README.md is a **separate deliverable file**, not part of this SPEC —
its purpose is to be the entry point for anyone opening the repository
(professors included). At minimum it should cover:

- One-paragraph pitch of what Nous Sight does and why (business context
  from section 0).
- Architecture diagram or bullet list of the layers (section 1).
- How to run locally (H2 profile) and how to switch to Oracle for delivery
  (section 6).
- A short table mapping each business rule (section 4) to the class/method
  that implements it — handy for whoever is grading the delivery.
