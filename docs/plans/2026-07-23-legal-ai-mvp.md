# Legal AI MVP Implementation Plan

> **For Hermes:** Execute this plan with strict RED → GREEN → REFACTOR cycles.

**Goal:** Convert the existing legal-parser CLI into a runnable modular-monolith Spring Boot application that uploads legal documents, processes them asynchronously, stores legal chunks and pgvector embeddings, and exposes an authenticated Thymeleaf administration/search UI.

**Architecture:** Keep one deployable Spring Boot process. Controllers remain thin; upload, storage, extraction, cleaning, parsing, chunking, embedding, persistence, and job claiming are separate components. PostgreSQL is the source of truth; original files use a storage interface with a local implementation. A database-backed worker claims jobs with `FOR UPDATE SKIP LOCKED`, preventing duplicate processing across application replicas.

**Tech Stack:** Java 21, Spring Boot MVC/Security/Data JPA/JDBC/Thymeleaf/Validation, PostgreSQL 16 + pgvector, Flyway, Apache POI, PDFBox, Jsoup, Docker Compose, JUnit 5, AssertJ, MockMvc.

---

## Task 1: Replace CLI build with Spring Boot application build

**Files:**
- Modify: `pom.xml`
- Create: `src/main/java/az/legalai/LegalAiApplication.java`
- Create: `src/main/resources/application.yml`

**Steps:**
1. Add Spring Boot parent and required dependencies.
2. Keep Apache POI and add PDFBox/Jsoup/pgvector PostgreSQL support.
3. Add application entry point and type-safe configuration.
4. Run `docker run --rm -v "$PWD:/workspace" -w /workspace maven:3.9.11-eclipse-temurin-21 mvn test`.

## Task 2: Define Flyway schema and domain model

**Files:**
- Create: `src/main/resources/db/migration/V1__initial_schema.sql`
- Create: `src/main/java/az/legalai/document/domain/*`
- Create: `src/main/java/az/legalai/job/*`
- Create: `src/main/java/az/legalai/document/repository/*`

**Steps:**
1. Create `vector` extension and `legal_documents`, `document_chunks`, `document_processing_jobs` tables.
2. Add unique checksum constraint, job claim indexes, vector index-ready schema, and cascade deletion.
3. Map documents/jobs/chunks with JPA; use JDBC for vector writes and similarity queries.
4. Verify migration against the Compose pgvector database.

## Task 3: Build and test legal ingestion primitives

**Files:**
- Test: `src/test/java/az/legalai/ingestion/*`
- Create: `src/main/java/az/legalai/ingestion/extractor/*`
- Create: `src/main/java/az/legalai/ingestion/cleaner/*`
- Create: `src/main/java/az/legalai/ingestion/parser/*`
- Create: `src/main/java/az/legalai/ingestion/chunker/*`

**Steps:**
1. RED: tests for preserving article/clause numbering while normalizing noise.
2. GREEN: block-aware cleaner.
3. RED: AZ/RU section hierarchy recognition tests.
4. GREEN: parser preserving parent context.
5. RED: article/clause-aware chunk tests and oversized paragraph splitting.
6. GREEN: chunker with deterministic indexes and parent paths.
7. Add DOC, DOCX, PDF, HTML, and TXT extractors behind one registry.

## Task 4: Implement secure upload and local file storage

**Files:**
- Test: `src/test/java/az/legalai/document/service/DocumentValidatorTest.java`
- Create: `src/main/java/az/legalai/storage/*`
- Create: `src/main/java/az/legalai/document/service/DocumentValidator.java`
- Create: `src/main/java/az/legalai/document/service/DocumentUploadService.java`
- Create: `src/main/java/az/legalai/config/SecurityConfig.java`

**Steps:**
1. RED: empty, oversized, unsupported extension, and magic-byte mismatch cases.
2. GREEN: bounded validation and SHA-256 calculation.
3. Store files under UUID-derived keys; prevent path traversal.
4. Persist document and pending job; compensate storage if persistence fails.
5. Configure HTTP Basic Auth from `ADMIN_USERNAME`/`ADMIN_PASSWORD`; never commit a real password.

## Task 5: Implement embeddings, vector persistence, and semantic search

**Files:**
- Create: `src/main/java/az/legalai/embedding/*`
- Create: `src/main/java/az/legalai/document/repository/DocumentChunkVectorRepository.java`
- Create: `src/main/java/az/legalai/document/service/DocumentSearchService.java`

**Steps:**
1. Define batch-oriented `EmbeddingService` and dimension validation.
2. Provide deterministic local lexical embeddings for zero-secret development and an OpenAI-compatible HTTP implementation for production.
3. Persist vectors through PostgreSQL `CAST(? AS vector)` batches.
4. Query cosine similarity through pgvector and cap result limits.

## Task 6: Implement reliable background processing

**Files:**
- Create: `src/main/java/az/legalai/ingestion/pipeline/DocumentIngestionPipeline.java`
- Create: `src/main/java/az/legalai/job/DocumentProcessingWorker.java`
- Create: `src/main/java/az/legalai/job/DocumentProcessingJobStore.java`

**Steps:**
1. Atomically claim one due job with `FOR UPDATE SKIP LOCKED`.
2. Update visible document status at each pipeline stage.
3. Extract, clean, parse, chunk, embed in batches, and replace prior chunks idempotently.
4. Complete or retry with bounded attempts/backoff; persist concise failure diagnostics.
5. Ensure reprocessing deletes/replaces chunks rather than duplicating them.

## Task 7: Build Thymeleaf administration UI

**Files:**
- Create: `src/main/java/az/legalai/document/controller/*`
- Create: `src/main/resources/templates/documents/*`
- Create: `src/main/resources/templates/search.html`
- Create: `src/main/resources/static/css/app.css`

**Steps:**
1. Add list, upload, details/chunks, reprocess, delete, and search endpoints.
2. Validate multipart form fields and show user-facing errors.
3. Add status badges, chunk counts, metadata rendering, and similarity scores.
4. Do not render raw embedding arrays.

## Task 8: Package and verify end-to-end

**Files:**
- Modify: `Dockerfile`
- Modify: `compose.yaml`
- Modify: `.gitignore`
- Modify: `README.md`
- Create: `.env.example`

**Steps:**
1. Build an OCI image with Java 21 and non-root runtime user.
2. Add pgvector PostgreSQL, health checks, private DB networking, storage volume, and env wiring.
3. Run all tests and `mvn clean package` in the builder container.
4. Start Compose; verify database/app health.
5. Verify unauthenticated `401`, authenticated upload, worker completion, chunk page, semantic search, reprocess, and delete.
6. Inspect browser console and server logs; then stop only temporary test resources.
