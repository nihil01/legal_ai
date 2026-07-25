package az.legalai.eqanun.parser;

import java.io.InputStream;

public interface EqanunDocParser {
    EqanunParsedLaw parse(
            EqanunLawCandidate candidate,
            String filename,
            String mimeType,
            InputStream documentStream);
}
