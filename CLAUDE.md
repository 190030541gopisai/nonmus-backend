# Project Guidelines: nonmus-backend

## Technology Stack
- **Language:** Java 26
- **Framework:** Spring Boot 4.x (WebMVC, Spring Data JPA, Redis, Kafka, Mail)
- **Database / Infrastructure:** PostgreSQL, Redis, Apache Kafka (via Docker Compose)
- **Build Tool:** Maven

## Build & Test Commands
- **Compile:** `mvn clean compile`
- **Run Tests:** `mvn test`
- **Run Single Test:** `mvn test -Dtest=ClassName#methodName`
- **Package (skip tests):** `mvn clean package -DskipTests`
- **Start App:** `mvn spring-boot:run`

## Local Development Environment
- **Start Dependencies:** `./local_init.sh` or `docker compose -f ./docker-compose-local.yml up -d`
- **Environment File:** Uses `.env` (refer to `.env.example`)

## Coding & Architectural Conventions
- Follow Spring Boot layered architecture (`controller`, `service`, `repository`, `dto`, `event`, `entity`).
- Use Lombok annotations where appropriate (`@RequiredArgsConstructor`, `@Getter`, etc.).
- Maintain transaction boundaries with `@Transactional`.
- Validate all incoming API request payloads with Bean Validation (`@Valid`, Jakarta constraints).
- Sensitive environment variables are kept in `.env` and never committed or logged.

## Agent skills

### Issue tracker

Issues and specs for this repo live as GitHub issues. See `docs/agents/issue-tracker.md`.

### Triage labels

Maps the five canonical triage roles to GitHub labels (`needs-triage`, `ready-for-agent`, etc.). See `docs/agents/triage-labels.md`.

### Domain docs

Single-context domain dictionary and ADRs at repo root. See `docs/agents/domain.md`.
