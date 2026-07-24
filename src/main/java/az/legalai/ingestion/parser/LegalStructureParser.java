package az.legalai.ingestion.parser;

import az.legalai.ingestion.cleaner.TextBlock;
import java.util.List;

public interface LegalStructureParser {
    LegalSection parse(String documentTitle, List<TextBlock> blocks);
}
