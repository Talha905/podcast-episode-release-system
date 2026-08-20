# Podcast Episode Release System

A Jenkins-based CI/CD release pipeline that automates podcast episode publishing — replacing a manual, error-prone workflow with a validated, auditable, status-driven release process.

## Problem

Podcast teams currently release episodes by hand: manual transcoding, manual upload, manual RSS editing, no validation, no audit trail. This project builds an automated release pipeline (Spring Boot app + Jenkins CI/CD) that standardizes and de-risks that process. See `docs/week1-problem-definition.md` for the full problem statement and MVP scope.

## Tech Stack

| Layer | Choice |
|---|---|
| Language / Runtime | Java 17 |
| Framework | Spring Boot 3 (Spring MVC + Spring Data JPA) |
| Build tool | Maven |
| Database | H2 (local dev), MySQL 8 (prod) |
| View layer | Thymeleaf + REST API |
| Deployment | Embedded Tomcat, reverse-proxied by Nginx |
| CI/CD | Jenkins |
| Testing | JUnit 5, Selenium WebDriver |

## Local Setup

**Prerequisites:** JDK 17, Maven 3.9+, Git

```bash
git clone https://github.com/<your-username>/podcast-episode-release-system.git
cd podcast-episode-release-system
mvn clean install
mvn spring-boot:run
```

App runs at `http://localhost:8080`. H2 console (dev only): `http://localhost:8080/h2-console`

## Project Structure

src/main/java/com/podcastrelease/
├── controller/ # REST endpoints
├── service/ # Business logic, status workflow
├── repository/ # Spring Data JPA
└── model/ # Entity + enum definitions


## Episode Status Workflow

DRAFT → VALIDATED → PUBLISHED
↓
FAILED


## Branching Policy

| Branch | Purpose | Rule |
|---|---|---|
| `main` | Always deployable / release-ready | No direct commits — PR only |
| `develop` | Sprint integration branch | Feature branches merge here first |
| `feature/US-XX-short-desc` | One user story at a time | e.g. `feature/US-01-create-episode` |
| `bugfix/short-desc` | Defect fixes | e.g. `bugfix/rss-validation-null` |
| `release/x.y` | Release prep | Cut from `develop`, merged to `main` |

**Commit convention:** Conventional Commits style — `feat:`, `fix:`, `chore:`, `docs:`, `test:`.

**PR requirements:** linked issue, passing build, at least 1 review (or self-review checklist), Definition of Done met (see `docs/week2-agile-planning.md`).

## Project Status

Tracking a 15-week MVP build. See open Issues for the current backlog (mirrors the Week 2 product backlog) and the Projects board for sprint status.

## License

Academic project — for coursework purposes.