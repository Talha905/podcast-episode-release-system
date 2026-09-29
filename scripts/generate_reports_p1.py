"""
Generate reports for Weeks 3 to 15.
Part 1: Weeks 3 to 6
"""

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from scripts.generate_docx_reports import (
    DOCS_DIR, build_base_document, add_section_heading, add_body_paragraph,
    add_bullet_point, add_code_block, add_callout, style_table
)
from docx.shared import Inches, Pt, RGBColor

def generate_week_3():
    doc = build_base_document(
        3, 
        "Requirements, Architecture and Technology Setup",
        "System Architecture, Domain Modeling & Local Stack Initialization"
    )
    
    # 1. Objective
    add_section_heading(doc, "1. Milestone Objective")
    add_body_paragraph(doc, 
        "The objective of Week 3 was to design the minimum viable application architecture for the Podcast Episode Release System. "
        "The MVP scope encompasses core record operations (create, view, update, search), a role-based status release workflow, "
        "and a status summary dashboard. Furthermore, the objective included selecting the technology stack (Java 17, Spring Boot, "
        "Maven, embedded H2 database, Apache Tomcat deployment target) and establishing a working local development environment."
    )
    
    # 2. Work Done
    add_section_heading(doc, "2. Technical Work Executed")
    add_bullet_point(doc, "Formulated the Software Requirements Specification (SRS) defining functional requirements for episode creation, review workflows, role separation, and dashboard reporting.", "SRS & Requirements Definition: ")
    add_bullet_point(doc, "Engineered a layered Model-View-Controller (MVC) architecture separating Presentation (Spring MVC / Thymeleaf), Business Logic (Spring Services), Data Access (Spring Data JPA), and Storage (H2 Database).", "Layered System Architecture: ")
    add_bullet_point(doc, "Modeled core entities including Episode (title, description, season/episode numbers, audio path, publication timestamps) and EpisodeStatus enum (DRAFT, PENDING_REVIEW, SCHEDULED, PUBLISHED, REJECTED, FAILED).", "Domain Data Modeling: ")
    add_bullet_point(doc, "Configured Maven coordinates in pom.xml for spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-validation, and com.h2database:h2.", "Dependency Setup: ")
    add_bullet_point(doc, "Established working local development setup with Spring Boot 3.3.3 and OpenJDK 17, verifying clean compilation and startup.", "Local Stack Verification: ")

    # 3. Key Code / Configuration & Commands
    add_section_heading(doc, "3. Key Code, Configuration & Commands")
    add_body_paragraph(doc, "Core Entity Definition (Episode.java snippet):")
    add_code_block(doc, 
"""@Entity
@Table(name = "episodes")
public class Episode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Title is required")
    private String title;
    
    @Column(length = 2000)
    private String description;
    
    @Enumerated(EnumType.STRING)
    private EpisodeStatus status = EpisodeStatus.DRAFT;
    
    private Integer seasonNumber;
    private Integer episodeNumber;
    private String audioFilePath;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    // Getters, setters, and lifecycle pre-persist hooks
}"""
    )
    
    add_body_paragraph(doc, "Maven Coordinates & Verification Commands:")
    add_code_block(doc,
"""# Clean and compile the application locally
mvn clean compile

# Execute initial test verification
mvn test

# Run Spring Boot application locally on port 8080/8005
mvn spring-boot:run"""
    )

    # 4. Git Commit Evidence
    add_section_heading(doc, "4. Git Commit Evidence")
    add_body_paragraph(doc, "The technical implementation for Week 3 was recorded in the repository via the following commits:")
    tbl_git = doc.add_table(rows=5, cols=4)
    git_headers = ["Commit Hash", "Date", "Author", "Commit Message"]
    git_rows = [
        ("be9ad6e", "2026-08-20", "talha", "chore: initialize Maven project with Spring Boot dependencies"),
        ("e575c65", "2026-08-20", "talha", "feat: add Spring Boot application entry point"),
        ("9e8a3af", "2026-08-20", "talha", "chore: add web, JPA, validation and H2 dependencies"),
        ("73d8d56", "2026-08-20", "talha", "feat: add Episode entity, status enum and app entry point")
    ]
    for c_idx, h in enumerate(git_headers):
        tbl_git.rows[0].cells[c_idx].paragraphs[0].text = h
    for r_idx, r_data in enumerate(git_rows):
        for c_idx, val in enumerate(r_data):
            tbl_git.rows[r_idx+1].cells[c_idx].paragraphs[0].text = val
    style_table(tbl_git, col_widths=[1.1, 1.0, 0.9, 3.7])
    doc.add_paragraph()

    # 5. Issues Faced and Fixes
    add_section_heading(doc, "5. Issues Faced and Fixes")
    add_body_paragraph(doc, "During the persistence layer setup, the following real issue was encountered and resolved:")
    add_bullet_point(doc, 
        "The initial Episode entity lacked an explicit setCreatedAt() setter method, which caused entity auditing listeners and unit tests attempting to set predefined timestamps during fixture initialization to fail compilation. "
        "Fix: Added the missing setCreatedAt(LocalDateTime) setter in Episode.java and committed the fix in commit c24d43a ('fix: add missing setCreatedAt setter on Episode entity').",
        "Missing Setter on Episode Entity: "
    )

    # 6. Outcome Against Deliverables
    add_section_heading(doc, "6. Outcome Against Week 3 Deliverables")
    tbl_out = doc.add_table(rows=5, cols=3)
    out_headers = ["Syllabus Deliverable", "Actual Implementation Status", "Verification Method"]
    out_rows = [
        ("SRS Summary & Scope", "Completed: Functional & Non-Functional scope defined for 15-week release lifecycle.", "Reviewed against project backlog and use-case matrix."),
        ("Architecture & Use-Case Model", "Completed: 3-tier Spring Boot architecture with role-based status pipeline.", "Validated layered code structure under src/main/java."),
        ("Data Model & API List", "Completed: Episode entity, EpisodeStatus enum, and planned REST/MVC endpoints.", "JPA schema generated automatically in H2 database."),
        ("Working Local Setup", "Completed: Maven 3.9 + OpenJDK 17 compiling cleanly with zero errors.", "Verified via 'mvn clean compile' with BUILD SUCCESS.")
    ]
    for c_idx, h in enumerate(out_headers):
        tbl_out.rows[0].cells[c_idx].paragraphs[0].text = h
    for r_idx, r_data in enumerate(out_rows):
        for c_idx, val in enumerate(r_data):
            tbl_out.rows[r_idx+1].cells[c_idx].paragraphs[0].text = val
    style_table(tbl_out, col_widths=[2.1, 2.7, 1.9])
    doc.add_paragraph()

    # 7. Verification Notes
    add_section_heading(doc, "7. Verification Notes")
    add_body_paragraph(doc, "Verified that the Maven build toolchain cleanly compiles all classes and initializes Spring Context with an in-memory H2 database. No external database server installation was required, ensuring portability across development workstations.")

    out_path = os.path.join(DOCS_DIR, "DEVOPS_WEEK-3.docx")
    doc.save(out_path)
    print(f"Generated {out_path}")

def generate_week_4():
    doc = build_base_document(
        4,
        "Git and GitHub Repository Initialization",
        "Repository Setup, Governance, Branching Policy & Issue Templates"
    )
    
    # 1. Objective
    add_section_heading(doc, "1. Milestone Objective")
    add_body_paragraph(doc,
        "The objective of Week 4 was to initialize the Git version control repository and connect it to GitHub. "
        "This included configuring .gitignore rules, establishing standardized project folder structures, creating GitHub issue templates "
        "and pull request templates, formalizing branch naming conventions, and committing the initial application skeleton with meaningful messages."
    )
    
    # 2. Work Done
    add_section_heading(doc, "2. Technical Work Executed")
    add_bullet_point(doc, "Initialized local Git tracking and linked remote repository at https://github.com/Talha905/podcast-episode-release-system.git.", "Git Repository Initialization: ")
    add_bullet_point(doc, "Engineered .gitignore to prevent committing build artifacts (target/), IDE files (.idea, .vscode, *.iml), operating system temp files, and logs.", "Git Ignore Configuration: ")
    add_bullet_point(doc, "Authored comprehensive project README.md documenting prerequisites, build commands, architecture overview, and running instructions.", "Project Documentation: ")
    add_bullet_point(doc, "Created GitHub issue templates under .github/ISSUE_TEMPLATE/ for bug reports and feature requests, standardizing issue tracking.", "Issue Governance: ")
    add_bullet_point(doc, "Created .github/PULL_REQUEST_TEMPLATE.md establishing mandatory PR checklists (testing evidence, documentation updates, code review).", "PR Governance: ")
    add_bullet_point(doc, "Documented branch naming rules: main (production-ready), develop (integration branch), feature/<issue-id>-<short-description>, and bugfix/<issue-id>-<short-description>.", "Branching Policy: ")

    # 3. Key Code / Configuration & Commands
    add_section_heading(doc, "3. Key Code, Configuration & Commands")
    add_body_paragraph(doc, "Standardized .gitignore Configuration:")
    add_code_block(doc,
"""target/
!.mvn/wrapper/maven-wrapper.jar
!**/src/main/**/target/
!**/src/test/**/target/

### STS ###
.apt_generated
.classpath
.factorypath
.project
.settings
.springBeans
.sts4-check

### IntelliJ IDEA ###
.idea
*.iws
*.iml
*.ipr

### NetBeans ###
/nbproject/private/
/nbbuild/
/dist/
/nbdist/
/.built-jar.properties

### VS Code ###
.vscode/

### Log files ###
*.log"""
    )
    
    add_body_paragraph(doc, "Repository Initialization & Linking Commands:")
    add_code_block(doc,
"""# Initialize local repository
git init

# Add remote origin
git remote add origin https://github.com/Talha905/podcast-episode-release-system.git

# Stage initial governance files and application skeleton
git add .gitignore README.md pom.xml .github/ src/

# Commit with structured semantic message
git commit -m "docs: add README with setup and branching policy; add issue and PR templates"

# Push to primary branch
git branch -M main
git push -u origin main"""
    )

    # 4. Git Commit Evidence
    add_section_heading(doc, "4. Git Commit Evidence")
    tbl_git = doc.add_table(rows=6, cols=4)
    git_headers = ["Commit Hash", "Date", "Author", "Commit Message"]
    git_rows = [
        ("2834b37", "2026-08-20", "Talha905", "Initial commit"),
        ("e4aeb54", "2026-08-20", "talha", "chore: add .gitignore for Maven/IDE artifacts"),
        ("86612d4", "2026-08-20", "talha", "docs: add project README with setup instructions"),
        ("a1bf33e", "2026-08-20", "talha", "chore: add issue templates and PR template"),
        ("f65822d", "2026-08-20", "talha", "docs: add README with setup and branching policy; add issue and PR templates")
    ]
    for c_idx, h in enumerate(git_headers):
        tbl_git.rows[0].cells[c_idx].paragraphs[0].text = h
    for r_idx, r_data in enumerate(git_rows):
        for c_idx, val in enumerate(r_data):
            tbl_git.rows[r_idx+1].cells[c_idx].paragraphs[0].text = val
    style_table(tbl_git, col_widths=[1.1, 1.0, 0.9, 3.7])
    doc.add_paragraph()

    # Note: No issues faced during standard git init, so Issues section omitted per prompt instructions.

    # 5. Outcome Against Deliverables
    add_section_heading(doc, "5. Outcome Against Week 4 Deliverables")
    tbl_out = doc.add_table(rows=5, cols=3)
    out_headers = ["Syllabus Deliverable", "Actual Implementation Status", "Verification Method"]
    out_rows = [
        ("GitHub Repository URL", "Completed: Published at https://github.com/Talha905/podcast-episode-release-system", "Verified accessible via browser and git clone."),
        ("README & Setup Guide", "Completed: Root README.md with prerequisites, architecture, and commands.", "Inspected in repository root on GitHub."),
        (".gitignore & Governance", "Completed: .gitignore, issue templates, and PR template active in .github/", "Verified file existence and formatting in repo."),
        ("Branching Policy", "Completed: Branching standards (main, develop, feature/*) established and documented.", "Documented in README.md and enforced in development.")
    ]
    for c_idx, h in enumerate(out_headers):
        tbl_out.rows[0].cells[c_idx].paragraphs[0].text = h
    for r_idx, r_data in enumerate(out_rows):
        for c_idx, val in enumerate(r_data):
            tbl_out.rows[r_idx+1].cells[c_idx].paragraphs[0].text = val
    style_table(tbl_out, col_widths=[2.0, 2.8, 1.9])
    doc.add_paragraph()

    # 6. Verification Notes
    add_section_heading(doc, "6. Verification Notes")
    add_body_paragraph(doc, "Verified remote synchronization with GitHub over HTTPS. Issue templates and PR templates were verified to appear automatically in GitHub's web interface when opening new issues and pull requests.")

    out_path = os.path.join(DOCS_DIR, "DEVOPS_WEEK-4.docx")
    doc.save(out_path)
    print(f"Generated {out_path}")

def generate_week_5():
    doc = build_base_document(
        5,
        "Feature Development with Branching",
        "Feature Branching Workflow, Episode Creation & GitHub Pull Request"
    )
    
    # 1. Objective
    add_section_heading(doc, "1. Milestone Objective")
    add_body_paragraph(doc,
        "The objective of Week 5 was to execute the first core feature workflow using strict Git branching practices. "
        "This entailed creating an isolated feature branch (feature/US-01-create-view-episode), implementing episode creation and detail viewing "
        "capabilities in the service and web layers, writing unit tests to verify default status behavior, pushing the branch, "
        "raising Pull Request #2 on GitHub, conducting code review, and merging the feature into the development baseline."
    )
    
    # 2. Work Done
    add_section_heading(doc, "2. Technical Work Executed")
    add_bullet_point(doc, "Created dedicated branch feature/US-01-create-view-episode from main to isolate development.", "Branch Creation: ")
    add_bullet_point(doc, "Implemented EpisodeRepository extending JpaRepository and EpisodeService providing createEpisode(Episode), getAllEpisodes(), and getEpisodeById(Long).", "Service & Repository Layer: ")
    add_bullet_point(doc, "Built EpisodeController mapping /episodes endpoints for form display, episode submission, and single-record viewing.", "Controller Endpoints: ")
    add_bullet_point(doc, "Authored unit test in EpisodeServiceTest verifying that any newly created episode strictly defaults to EpisodeStatus.DRAFT.", "Unit Test Verification: ")
    add_bullet_point(doc, "Pushed feature branch to GitHub, opened Pull Request #2, documented acceptance criteria verification, and executed clean merge.", "Pull Request & Merge: ")

    # 3. Key Code / Configuration & Commands
    add_section_heading(doc, "3. Key Code, Configuration & Commands")
    add_body_paragraph(doc, "Episode Service Implementation (EpisodeService.java snippet):")
    add_code_block(doc,
"""@Service
public class EpisodeService {
    private final EpisodeRepository episodeRepository;

    public EpisodeService(EpisodeRepository episodeRepository) {
        this.episodeRepository = episodeRepository;
    }

    public Episode createEpisode(Episode episode) {
        if (episode.getStatus() == null) {
            episode.setStatus(EpisodeStatus.DRAFT);
        }
        if (episode.getCreatedAt() == null) {
            episode.setCreatedAt(LocalDateTime.now());
        }
        return episodeRepository.save(episode);
    }

    public List<Episode> getAllEpisodes() {
        return episodeRepository.findAll();
    }

    public Optional<Episode> getEpisodeById(Long id) {
        return episodeRepository.findById(id);
    }
}"""
    )
    
    add_body_paragraph(doc, "Feature Branch Git Workflow Commands:")
    add_code_block(doc,
"""# Branch out from main
git checkout -b feature/US-01-create-view-episode

# Stage feature code and unit tests
git add src/main/java/com/podcastrelease/repository/EpisodeRepository.java
git add src/main/java/com/podcastrelease/service/EpisodeService.java
git add src/main/java/com/podcastrelease/controller/EpisodeController.java
git add src/test/java/com/podcastrelease/service/EpisodeServiceTest.java

# Commit with semantic tags
git commit -m "feat: add EpisodeRepository and EpisodeService (create, list, findById)"
git commit -m "feat: add EpisodeController with create and view endpoints"
git commit -m "test: add unit test for episode creation defaulting to DRAFT"

# Push feature branch to GitHub
git push -u origin feature/US-01-create-view-episode"""
    )

    # 4. Git Commit Evidence
    add_section_heading(doc, "4. Git Commit Evidence")
    tbl_git = doc.add_table(rows=5, cols=4)
    git_headers = ["Commit Hash", "Date", "Author", "Commit Message"]
    git_rows = [
        ("be4cee7", "2026-08-20", "talha", "feat: add EpisodeRepository and EpisodeService (create, list, findById)"),
        ("f1d4072", "2026-08-20", "talha", "feat: add EpisodeController with create and view endpoints"),
        ("16b53ef", "2026-08-20", "talha", "test: add unit test for episode creation defaulting to DRAFT"),
        ("cca9d80", "2026-08-20", "Talha905", "Merge pull request #2 from Talha905/feature/US-01-create-view-episode")
    ]
    for c_idx, h in enumerate(git_headers):
        tbl_git.rows[0].cells[c_idx].paragraphs[0].text = h
    for r_idx, r_data in enumerate(git_rows):
        for c_idx, val in enumerate(r_data):
            tbl_git.rows[r_idx+1].cells[c_idx].paragraphs[0].text = val
    style_table(tbl_git, col_widths=[1.1, 1.0, 0.9, 3.7])
    doc.add_paragraph()

    # Note: No issues faced during feature 1; section omitted.

    # 5. Outcome Against Deliverables
    add_section_heading(doc, "5. Outcome Against Week 5 Deliverables")
    tbl_out = doc.add_table(rows=5, cols=3)
    out_headers = ["Syllabus Deliverable", "Actual Implementation Status", "Verification Method"]
    out_rows = [
        ("Working Feature 1", "Completed: Create and view episode records implemented and operational.", "Verified through unit tests and controller HTTP tests."),
        ("Feature Branch", "Completed: feature/US-01-create-view-episode created and tracked.", "Verified in GitHub branch list and git reflog."),
        ("Pull Request & Review", "Completed: Pull Request #2 raised, reviewed, and documented on GitHub.", "Inspected PR #2 on GitHub with associated diff."),
        ("Merged Baseline", "Completed: Clean merge into primary branch (commit cca9d80).", "Verified git log showing merge commit.")
    ]
    for c_idx, h in enumerate(out_headers):
        tbl_out.rows[0].cells[c_idx].paragraphs[0].text = h
    for r_idx, r_data in enumerate(out_rows):
        for c_idx, val in enumerate(r_data):
            tbl_out.rows[r_idx+1].cells[c_idx].paragraphs[0].text = val
    style_table(tbl_out, col_widths=[1.9, 2.9, 1.9])
    doc.add_paragraph()

    # 6. Verification Notes
    add_section_heading(doc, "6. Verification Notes")
    add_body_paragraph(doc, "Verified via unit test execution that new episodes reliably receive the DRAFT status. Verified that Pull Request #2 cleanly merged into the upstream repository without conflicting files.")

    out_path = os.path.join(DOCS_DIR, "DEVOPS_WEEK-3.docx") # will overwrite or save appropriately
    out_path = os.path.join(DOCS_DIR, "DEVOPS_WEEK-5.docx")
    doc.save(out_path)
    print(f"Generated {out_path}")

def generate_week_6():
    doc = build_base_document(
        6,
        "MVP Completion and Git Collaboration",
        "Full MVP Capabilities, RBAC Workflow, Conflict Resolution & Release v0.6.0"
    )
    
    # 1. Objective
    add_section_heading(doc, "1. Milestone Objective")
    add_body_paragraph(doc,
        "The objective of Week 6 was to complete all remaining core MVP capabilities of the Podcast Episode Release System. "
        "This included implementing episode record updating, full-text case-insensitive searching, a role-based status transition workflow "
        "(Creator, Admin, Auditor), and a summary metrics dashboard. Furthermore, the milestone required demonstrating a second feature branch, "
        "generating and resolving a merge conflict, updating the project backlog, and creating an annotated release tag (v0.6.0)."
    )
    
    # 2. Work Done
    add_section_heading(doc, "2. Technical Work Executed")
    add_bullet_point(doc, "Implemented updateEpisode() in EpisodeService and added search query methods in EpisodeRepository using findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase.", "Update & Search Features: ")
    add_bullet_point(doc, "Enforced role-based access control (RBAC) in SecurityConfig: Creator (drafting/editing), Admin (approving, scheduling, releasing), Auditor (read-only audit trails).", "Role-Based Access Control: ")
    add_bullet_point(doc, "Engineered DashboardService and DashboardController calculating live metrics for total episodes, published episodes, pending reviews, and draft counts.", "Summary Metrics Dashboard: ")
    add_bullet_point(doc, "Developed responsive Thymeleaf UI templates (dashboard.html, list.html, form.html, detail.html) providing interactive web navigation.", "Thymeleaf Web Interface: ")
    add_bullet_point(doc, "Simulated concurrent branch modifications to test conflict resolution, manually merged conflicting changes, and tagged the stable release as v0.6.0.", "Git Collaboration & Release Tag: ")

    # 3. Key Code / Configuration & Commands
    add_section_heading(doc, "3. Key Code, Configuration & Commands")
    add_body_paragraph(doc, "Search Query Method (EpisodeRepository.java snippet):")
    add_code_block(doc,
"""public interface EpisodeRepository extends JpaRepository<Episode, Long> {
    List<Episode> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
        String titleKeyword, String descKeyword
    );
    
    long countByStatus(EpisodeStatus status);
}"""
    )
    
    add_body_paragraph(doc, "Release Tagging Commands:")
    add_code_block(doc,
"""# Create annotated release tag
git tag -a v0.6.0 -m "release: Week 6 MVP Core complete (v0.6.0)"

# Push tags to GitHub
git push origin v0.6.0

# Verify tagged commit
git show v0.6.0"""
    )

    # 4. Git Commit Evidence
    add_section_heading(doc, "4. Git Commit Evidence")
    tbl_git = doc.add_table(rows=5, cols=4)
    git_headers = ["Commit Hash", "Date", "Author", "Commit Message"]
    git_rows = [
        ("f3c3a9c", "2026-08-26", "talha", "feat: complete Week 5 & 6 MVP core with update, search, status workflow, RBAC, and Thymeleaf UI"),
        ("4978c19", "2026-08-26", "talha", "merge: feature/US-01-create-view-episode into develop"),
        ("e81b9d5", "2026-08-26", "talha", "release: Week 6 MVP Core complete (v0.6.0)"),
        ("a03ff7b", "2026-08-26", "talha", "docs: add RUNNING_INSTRUCTIONS.md with detailed guide to run project")
    ]
    for c_idx, h in enumerate(git_headers):
        tbl_git.rows[0].cells[c_idx].paragraphs[0].text = h
    for r_idx, r_data in enumerate(git_rows):
        for c_idx, val in enumerate(r_data):
            tbl_git.rows[r_idx+1].cells[c_idx].paragraphs[0].text = val
    style_table(tbl_git, col_widths=[1.1, 1.0, 0.9, 3.7])
    doc.add_paragraph()

    # 5. Issues Faced and Fixes
    add_section_heading(doc, "5. Issues Faced and Fixes")
    add_body_paragraph(doc, "During the consolidation of the web controllers, the following issue was encountered and fixed:")
    add_bullet_point(doc,
        "Both HomeController and DashboardController were mapped to the root path '/', causing Spring MVC to fail on startup with 'java.lang.IllegalStateException: Ambiguous mapping'. "
        "Fix: Removed the redundant mapping in HomeController and configured a clean forward to /dashboard in commit cbe2168 ('fix: resolve ambiguous handler mapping conflict for root path /').",
        "Ambiguous Request Mapping on Root Path: "
    )

    # 6. Outcome Against Deliverables
    add_section_heading(doc, "6. Outcome Against Week 6 Deliverables")
    tbl_out = doc.add_table(rows=5, cols=3)
    out_headers = ["Syllabus Deliverable", "Actual Implementation Status", "Verification Method"]
    out_rows = [
        ("Functional MVP Core", "Completed: Update, search, role-based workflow, and summary dashboard operational.", "Tested in browser and via automated test cases."),
        ("Resolved Merge Conflict", "Completed: Demonstrated branch convergence and manual conflict reconciliation.", "Verified in git log showing merged develop branch."),
        ("Tagged Release Baseline", "Completed: Tagged v0.6.0 with release metadata in Git.", "Verified via 'git tag -l' and GitHub release view."),
        ("Updated Backlog & Guide", "Completed: Authored RUNNING_INSTRUCTIONS.md with credentials and run steps.", "Documented in commit a03ff7b.")
    ]
    for c_idx, h in enumerate(out_headers):
        tbl_out.rows[0].cells[c_idx].paragraphs[0].text = h
    for r_idx, r_data in enumerate(out_rows):
        for c_idx, val in enumerate(r_data):
            tbl_out.rows[r_idx+1].cells[c_idx].paragraphs[0].text = val
    style_table(tbl_out, col_widths=[2.0, 2.8, 1.9])
    doc.add_paragraph()

    # 7. Verification Notes
    add_section_heading(doc, "7. Verification Notes")
    add_body_paragraph(doc, "Verified all role flows locally: Creator creating draft, Admin approving to Scheduled and Published, and Auditor inspecting audit logs. Tag v0.6.0 was verified in the GitHub releases section.")

    out_path = os.path.join(DOCS_DIR, "DEVOPS_WEEK-6.docx")
    doc.save(out_path)
    print(f"Generated {out_path}")

generate_week_3()
generate_week_4()
generate_week_5()
generate_week_6()
