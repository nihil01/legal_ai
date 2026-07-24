package az.legalai.ingestion.chunker;

import static org.assertj.core.api.Assertions.assertThat;

import az.legalai.ingestion.parser.LegalSection;
import az.legalai.ingestion.parser.SectionType;
import java.util.List;
import org.junit.jupiter.api.Test;

class LegalDocumentChunkerTest {
    private final LegalDocumentChunker chunker = new LegalDocumentChunker(500);

    @Test
    void keepsParentContextAndUsesClausesForLargeArticles() {
        var clause1 = leaf(SectionType.CLAUSE, "25.1", "", "Birinci qayda ".repeat(15));
        var clause2 = leaf(SectionType.CLAUSE, "25.2", "", "İkinci qayda ".repeat(15));
        var article =
                new LegalSection(
                        SectionType.ARTICLE, "25", "Hüquqlar", "", List.of(clause1, clause2));
        var chapter = new LegalSection(SectionType.CHAPTER, "4", "Şəxslər", "", List.of(article));
        var root =
                new LegalSection(SectionType.DOCUMENT, null, "Mülki Məcəllə", "", List.of(chapter));

        var chunks = chunker.chunk(root);

        assertThat(chunks).hasSize(2);
        assertThat(chunks).extracting(ChunkDraft::articleNumber).containsOnly("25");
        assertThat(chunks).extracting(ChunkDraft::clauseNumber).containsExactly("25.1", "25.2");
        assertThat(chunks.getFirst().parentPath()).contains("Mülki Məcəllə", "Fəsil 4", "Maddə 25");
        assertThat(chunks.getFirst().embeddingContent()).startsWith("Sənəd: Mülki Məcəllə");
    }

    @Test
    void keepsSmallArticleAsSingleChunk() {
        var article =
                new LegalSection(
                        SectionType.ARTICLE,
                        "10",
                        "Başlıq",
                        "Maddə mətni",
                        List.of(
                                leaf(SectionType.CLAUSE, "10.1", "", "Qısa bənd"),
                                leaf(SectionType.CLAUSE, "10.2", "", "Digər bənd")));
        var root = new LegalSection(SectionType.DOCUMENT, null, "Məcəllə", "", List.of(article));

        var chunks = chunker.chunk(root);

        assertThat(chunks)
                .singleElement()
                .satisfies(
                        chunk -> {
                            assertThat(chunk.articleNumber()).isEqualTo("10");
                            assertThat(chunk.content()).contains("10.1", "10.2");
                        });
    }

    private LegalSection leaf(SectionType type, String number, String title, String text) {
        return new LegalSection(type, number, title, text, List.of());
    }
}
