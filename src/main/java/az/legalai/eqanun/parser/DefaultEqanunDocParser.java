package az.legalai.eqanun.parser;

import az.legalai.ingestion.cleaner.LegalTextCleaner;
import az.legalai.ingestion.extractor.DocTextExtractor;
import az.legalai.ingestion.parser.LegalStructureParser;
import java.io.InputStream;
import org.springframework.stereotype.Component;

@Component
public class DefaultEqanunDocParser implements EqanunDocParser {
    private final DocTextExtractor extractor;
    private final LegalTextCleaner cleaner;
    private final LegalStructureParser structureParser;

    public DefaultEqanunDocParser(
            DocTextExtractor extractor,
            LegalTextCleaner cleaner,
            LegalStructureParser structureParser) {
        this.extractor = extractor;
        this.cleaner = cleaner;
        this.structureParser = structureParser;
    }

    @Override
    public EqanunParsedLaw parse(EqanunLawCandidate candidate, InputStream document) {
        var extracted = extractor.extract(document);
        var cleaned = cleaner.clean(extracted.blocks());
        var structure = structureParser.parse(candidate.title(), cleaned);

        // TODO(eq-anun): Map e-qanun-specific metadata, amendment links and actuality dates
        // into version_number, valid_from, valid_to and is_current before ingestion.
        return new EqanunParsedLaw(candidate, extracted, structure);
    }
}
