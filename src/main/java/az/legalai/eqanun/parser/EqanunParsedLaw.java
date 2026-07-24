package az.legalai.eqanun.parser;

import az.legalai.ingestion.extractor.ExtractedDocument;
import az.legalai.ingestion.parser.LegalSection;

public record EqanunParsedLaw(
        EqanunLawCandidate candidate, ExtractedDocument extracted, LegalSection structure) {}
