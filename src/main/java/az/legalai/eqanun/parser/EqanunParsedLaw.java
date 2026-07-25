package az.legalai.eqanun.parser;

import az.legalai.ingestion.extractor.ExtractedDocument;
import az.legalai.ingestion.parser.LegalSection;

public record EqanunParsedLaw(
        EqanunLawCandidate candidate,
        String originalFilename,
        String mimeType,
        ExtractedDocument extractedDocument,
        LegalSection structure) {}
