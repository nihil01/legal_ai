package az.legalai.eqanun.parser;

import java.io.InputStream;

public interface EqanunDocParser {
    EqanunParsedLaw parse(EqanunLawCandidate candidate, InputStream document);
}
