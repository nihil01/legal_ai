package az.legalai.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.legalai.document.repository.ChunkView;
import az.legalai.document.repository.DocumentChunkStore;
import az.legalai.document.repository.LegalDocumentRepository;
import az.legalai.embedding.EmbeddingService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DocumentQueryServiceTest {

    @Test
    void returnsRequestedChunkPageWithoutLoadingTheWholeDocument() {
        UUID documentId = UUID.randomUUID();
        LegalDocumentRepository documents = mock(LegalDocumentRepository.class);
        DocumentChunkStore chunks = mock(DocumentChunkStore.class);
        EmbeddingService embeddings = mock(EmbeddingService.class);
        ChunkView item = mock(ChunkView.class);
        when(chunks.count(documentId, "maddə")).thenReturn(2304L);
        when(chunks.list(documentId, "maddə", 25, 50)).thenReturn(List.of(item));

        ChunkPage result =
                new DocumentQueryService(documents, chunks, embeddings)
                        .chunks(documentId, "maddə", 2, 25);

        assertThat(result.items()).containsExactly(item);
        assertThat(result.total()).isEqualTo(2304);
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.pageSize()).isEqualTo(25);
        assertThat(result.totalPages()).isEqualTo(93);
        assertThat(result.firstItemNumber()).isEqualTo(51);
        assertThat(result.lastItemNumber()).isEqualTo(51);
        verify(chunks).list(documentId, "maddə", 25, 50);
    }
}
