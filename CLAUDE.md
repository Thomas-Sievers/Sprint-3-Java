# Nous Sight — Project Instructions
 
Conversational Intelligence platform for TOTVS. Turns sales-meeting
transcripts into structured insights (confidence score + human validation
for anything below 80%, churn-risk tracking per client).
 
## Full spec
 
@docs/SPEC.md
 
The file above is the source of truth for entities, business rules,
repository/service/controller contracts, connection config, and tests.
Read it before generating or editing any code in this project.
 
## Stack
 
Spring Boot + Spring Data JPA + Lombok. Package pattern:
`entity → repository → service → controller`. Tests: JUnit 5 + Mockito.
 
## Non-negotiable rule
 
Physical DB table/column names and stored domain values (e.g. `'Pendente'`,
`'Validado'`, `'Time Rival'`) stay in **Portuguese** — they already exist in
the real Oracle schema. Java identifiers (classes, fields, methods) stay in
**English**. Every `@Column`/`@JoinColumn` must use an explicit
`name = "..."` pointing at the Portuguese physical name. Never let Hibernate
guess the column name from the English field name.
 
## Commands
 
- `mvn spring-boot:run` — run locally against H2 (default profile)
- `mvn spring-boot:run -Dspring-boot.run.profiles=oracle` — run against Oracle
- `mvn test` — run the test suite
## Working style
 
Build one layer at a time (entity → repository → service → controller →
config/tests), following the roadmap in docs/SPEC.md §10. Show the result
after each layer before moving to the next.