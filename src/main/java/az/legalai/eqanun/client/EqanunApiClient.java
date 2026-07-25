package az.legalai.eqanun.client;

import az.legalai.eqanun.parser.EqanunLawCandidate;
import java.util.Optional;

public interface EqanunApiClient {
    Optional<EqanunLawCandidate> findLatestVersion(String codexId);

    EqanunDocumentPayload downloadCurrentDocument(String codexId);
}
