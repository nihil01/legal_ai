CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE legal_documents (
 id UUID PRIMARY KEY, title TEXT, original_filename TEXT NOT NULL, document_type VARCHAR(100), language VARCHAR(20), source_url TEXT,
 storage_key TEXT NOT NULL, mime_type VARCHAR(150), file_size BIGINT NOT NULL, adoption_date DATE, effective_date DATE, expiration_date DATE,
 status VARCHAR(50) NOT NULL, processing_error TEXT, checksum VARCHAR(128) NOT NULL, version_number INTEGER NOT NULL DEFAULT 1,
 document_group_id UUID, valid_from DATE, valid_to DATE, is_current BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT uk_legal_documents_checksum UNIQUE(checksum)
);
CREATE INDEX idx_legal_documents_status ON legal_documents(status);

CREATE TABLE document_chunks (
 id UUID PRIMARY KEY, document_id UUID NOT NULL REFERENCES legal_documents(id) ON DELETE CASCADE, chunk_index INTEGER NOT NULL,
 section_type VARCHAR(50), section_number VARCHAR(100), section_title TEXT, article_number VARCHAR(100), clause_number VARCHAR(100),
 parent_path TEXT, content TEXT NOT NULL, embedding_content TEXT NOT NULL, token_count INTEGER, metadata JSONB NOT NULL DEFAULT '{}',
 embedding VECTOR(1536), created_at TIMESTAMPTZ NOT NULL, UNIQUE(document_id,chunk_index)
);
CREATE INDEX idx_document_chunks_document ON document_chunks(document_id);
CREATE INDEX idx_document_chunks_article ON document_chunks(article_number);

CREATE TABLE document_processing_jobs (
 id UUID PRIMARY KEY, document_id UUID NOT NULL REFERENCES legal_documents(id) ON DELETE CASCADE, status VARCHAR(50) NOT NULL,
 attempts INTEGER NOT NULL DEFAULT 0, next_attempt_at TIMESTAMPTZ, locked_at TIMESTAMPTZ, locked_by VARCHAR(255), lock_token UUID, error_message TEXT,
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_processing_jobs_poll ON document_processing_jobs(status,next_attempt_at,created_at);
CREATE UNIQUE INDEX uk_processing_jobs_active_document ON document_processing_jobs(document_id) WHERE status IN ('PENDING','PROCESSING');
