package az.legalai.ingestion.parser;

import static org.assertj.core.api.Assertions.assertThat;

import az.legalai.ingestion.cleaner.TextBlock;
import java.util.List;
import org.junit.jupiter.api.Test;

class AzerbaijaniLegalStructureParserTest {
    private final LegalStructureParser parser = new AzerbaijaniLegalStructureParser();

    @Test
    void recognizesAzerbaijaniHierarchyAndClauseNumbers() {
        var root =
                parser.parse(
                        "Mülki Məcəllə",
                        List.of(
                                block(0, "I Bölmə. Ümumi müddəalar", true, 16),
                                block(1, "2-ci Fəsil. Fiziki şəxslər", true, 15),
                                block(2, "Maddə 10. Fiziki şəxslərin hüquq qabiliyyəti", true, 14),
                                block(
                                        3,
                                        "10.1. Fiziki şəxsin hüquq qabiliyyəti doğulduğu anda yaranır.",
                                        false,
                                        11),
                                block(
                                        4,
                                        "10.1.1. Qanunda nəzərdə tutulan hallar istisnadır.",
                                        false,
                                        11)));

        var section = root.children().getFirst();
        var chapter = section.children().getFirst();
        var article = chapter.children().getFirst();

        assertThat(section.type()).isEqualTo(SectionType.SECTION);
        assertThat(chapter.type()).isEqualTo(SectionType.CHAPTER);
        assertThat(article.type()).isEqualTo(SectionType.ARTICLE);
        assertThat(article.number()).isEqualTo("10");
        assertThat(article.children())
                .extracting(LegalSection::type)
                .containsExactly(SectionType.CLAUSE, SectionType.SUBCLAUSE);
        assertThat(article.children())
                .extracting(LegalSection::number)
                .containsExactly("10.1", "10.1.1");
    }

    @Test
    void recognizesRussianMarkersCaseInsensitively() {
        var root =
                parser.parse(
                        "Тестовый закон",
                        List.of(
                                block(0, "Раздел 1. Общие положения", true, 16),
                                block(1, "Глава 2. Права", true, 15),
                                block(2, "Статья 7. Правоспособность", true, 14),
                                block(
                                        3,
                                        "7.1. Правоспособность возникает при рождении.",
                                        false,
                                        11)));

        assertThat(root.children().getFirst().type()).isEqualTo(SectionType.SECTION);
        assertThat(root.children().getFirst().children().getFirst().type())
                .isEqualTo(SectionType.CHAPTER);
    }

    private TextBlock block(int index, String text, boolean bold, double fontSize) {
        return new TextBlock(index, text, "PARAGRAPH", null, null, bold, fontSize);
    }
}
