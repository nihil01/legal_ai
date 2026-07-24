package az.legalai.eqanun.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import az.legalai.ingestion.cleaner.LegalTextCleaner;
import az.legalai.ingestion.cleaner.TextBlock;
import az.legalai.ingestion.extractor.DocTextExtractor;
import az.legalai.ingestion.extractor.ExtractedDocument;
import az.legalai.ingestion.parser.LegalSection;
import az.legalai.ingestion.parser.LegalStructureParser;
import az.legalai.ingestion.parser.SectionType;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DefaultEqanunDocParserTest {
    @Test
    void extractsCleansAndBuildsLegalStructure() {
        DocTextExtractor extractor = mock(DocTextExtractor.class);
        LegalTextCleaner cleaner = new LegalTextCleaner();
        LegalStructureParser structureParser = mock(LegalStructureParser.class);
        ExtractedDocument extracted =
                new ExtractedDocument(
                        "Maddə 1. Test",
                        List.of(
                                new TextBlock(
                                        0, "Maddə 1. Test", "PARAGRAPH", null, null, true, 14.0)),
                        Map.of());
        LegalSection root = new LegalSection(SectionType.DOCUMENT, null, "Test law", "", List.of());
        when(extractor.extract(org.mockito.ArgumentMatchers.any())).thenReturn(extracted);
        when(structureParser.parse(
                        org.mockito.ArgumentMatchers.eq("Test law"),
                        org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(root);

        EqanunDocParser parser = new DefaultEqanunDocParser(extractor, cleaner, structureParser);
        EqanunParsedLaw result =
                parser.parse(
                        new EqanunLawCandidate("42", "Test law", "https://e-qanun.az/42", null),
                        new ByteArrayInputStream(new byte[] {1}));

        assertThat(result.candidate().externalId()).isEqualTo("42");
        assertThat(result.extracted()).isSameAs(extracted);
        assertThat(result.structure()).isSameAs(root);
    }
}
