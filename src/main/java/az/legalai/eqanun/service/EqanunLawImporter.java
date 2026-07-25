package az.legalai.eqanun.service;

import az.legalai.eqanun.client.EqanunDocumentPayload;
import az.legalai.eqanun.parser.EqanunLawCandidate;

public interface EqanunLawImporter {
    boolean isImported(EqanunLawCandidate candidate);

    EqanunImportOutcome importLaw(EqanunLawCandidate candidate, EqanunDocumentPayload document);
}
