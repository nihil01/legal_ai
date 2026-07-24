package az.legalai.document.repository;

import az.legalai.ingestion.chunker.ChunkDraft;
import az.legalai.job.JobClaim;
import az.legalai.job.JobLeaseGuard;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class DocumentChunkStore {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final JobLeaseGuard leaseGuard;

    public DocumentChunkStore(JdbcTemplate jdbc, ObjectMapper mapper, JobLeaseGuard leaseGuard) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.leaseGuard = leaseGuard;
    }

    @Transactional
    public void replace(JobClaim job, List<ChunkDraft> chunks, List<float[]> vectors) {
        if (chunks.size() != vectors.size()) {
            throw new IllegalArgumentException("Chunk/vector count mismatch");
        }
        leaseGuard.lock(job);
        UUID documentId = job.documentId();
        jdbc.update("delete from document_chunks where document_id=?", documentId);
        String sql =
                """
                insert into document_chunks(
                    id, document_id, chunk_index, section_type, section_number, section_title,
                    article_number, clause_number, parent_path, content, embedding_content,
                    token_count, metadata, embedding, created_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS vector), now())
                """;
        jdbc.batchUpdate(
                sql,
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ChunkDraft chunk = chunks.get(i);
                        ps.setObject(1, UUID.randomUUID());
                        ps.setObject(2, documentId);
                        ps.setInt(3, chunk.chunkIndex());
                        ps.setString(4, chunk.sectionType().name());
                        ps.setString(5, chunk.sectionNumber());
                        ps.setString(6, chunk.sectionTitle());
                        ps.setString(7, chunk.articleNumber());
                        ps.setString(8, chunk.clauseNumber());
                        ps.setString(9, chunk.parentPath());
                        ps.setString(10, chunk.content());
                        ps.setString(11, chunk.embeddingContent());
                        ps.setInt(12, chunk.tokenCount());
                        ps.setString(13, json(chunk.metadata()));
                        ps.setString(14, vector(vectors.get(i)));
                    }

                    @Override
                    public int getBatchSize() {
                        return chunks.size();
                    }
                });
    }

    public long count(UUID documentId) {
        Long value =
                jdbc.queryForObject(
                        "select count(*) from document_chunks where document_id=?",
                        Long.class,
                        documentId);
        return value == null ? 0 : value;
    }

    public List<ChunkView> list(UUID documentId) {
        return jdbc.query(
                """
                        select id, chunk_index, section_type, section_number, section_title,
                               article_number, clause_number, parent_path, content, token_count, metadata::text
                        from document_chunks where document_id=? order by chunk_index
                        """,
                (row, number) -> map(row, null),
                documentId);
    }

    public List<ChunkView> search(float[] embedding, int limit) {
        String value = vector(embedding);
        return jdbc.query(
                """
                        select id, chunk_index, section_type, section_number, section_title,
                               article_number, clause_number, parent_path, content, token_count, metadata::text,
                               1 - (embedding <=> CAST(? AS vector)) similarity
                        from document_chunks
                        where embedding is not null
                        order by embedding <=> CAST(? AS vector)
                        limit ?
                        """,
                (row, number) -> map(row, row.getDouble(12)),
                value,
                value,
                limit);
    }

    private ChunkView map(java.sql.ResultSet row, Double similarity) throws SQLException {
        return new ChunkView(
                row.getObject(1, UUID.class),
                row.getInt(2),
                row.getString(3),
                row.getString(4),
                row.getString(5),
                row.getString(6),
                row.getString(7),
                row.getString(8),
                row.getString(9),
                row.getInt(10),
                row.getString(11),
                similarity);
    }

    private String json(Map<String, Object> value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize chunk metadata", e);
        }
    }

    private String vector(float[] values) {
        StringBuilder result = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) result.append(',');
            result.append(values[i]);
        }
        return result.append(']').toString();
    }
}
