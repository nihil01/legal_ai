package az.legalai.ingestion.cleaner;

public record TextBlock(
        int orderIndex,
        String text,
        String blockType,
        Integer pageNumber,
        String styleName,
        boolean bold,
        Double fontSize) {}
