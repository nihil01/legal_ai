ALTER TABLE legal_documents
    ADD COLUMN external_source VARCHAR(50),
    ADD COLUMN external_id VARCHAR(100),
    ADD COLUMN external_version_id VARCHAR(100);

ALTER TABLE legal_documents
    ADD CONSTRAINT ck_legal_documents_external_identity
    CHECK (
        (external_source IS NULL AND external_id IS NULL AND external_version_id IS NULL)
        OR
        (external_source IS NOT NULL AND external_id IS NOT NULL AND external_version_id IS NOT NULL)
    );

CREATE UNIQUE INDEX uk_legal_documents_external_version
    ON legal_documents(external_source, external_id, external_version_id)
    WHERE external_source IS NOT NULL;

CREATE UNIQUE INDEX uk_legal_documents_external_local_version
    ON legal_documents(external_source, external_id, version_number)
    WHERE external_source IS NOT NULL;

ALTER TABLE legal_documents DROP CONSTRAINT uk_legal_documents_checksum;
CREATE UNIQUE INDEX uk_legal_documents_manual_checksum
    ON legal_documents(checksum)
    WHERE external_source IS NULL;
CREATE INDEX idx_legal_documents_checksum ON legal_documents(checksum);

CREATE UNIQUE INDEX uk_legal_documents_current_external
    ON legal_documents(external_source, external_id)
    WHERE external_source IS NOT NULL AND is_current = TRUE;
