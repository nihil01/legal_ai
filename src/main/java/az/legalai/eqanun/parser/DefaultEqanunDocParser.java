package az.legalai.eqanun.parser;

import az.legalai.ingestion.cleaner.LegalTextCleaner;
import az.legalai.ingestion.extractor.DocumentExtractorRegistry;
import az.legalai.ingestion.parser.LegalStructureParser;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultEqanunDocParser implements EqanunDocParser {
    private final DocumentExtractorRegistry extractors;
    private final LegalTextCleaner cleaner;
    private final LegalStructureParser structureParser;

    @Override
    public EqanunParsedLaw parse(
            EqanunLawCandidate candidate,
            String filename,
            String mimeType,
            InputStream documentStream) {
        var extractor = extractors.get(mimeType, filename);
        var extracted = extractor.extract(documentStream);
        var cleaned = cleaner.clean(extracted.blocks());
        var structure = structureParser.parse(candidate.title(), cleaned);
        return new EqanunParsedLaw(candidate, filename, mimeType, extracted, structure);
    }
}
