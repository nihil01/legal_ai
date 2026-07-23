package az.legalparser;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFtnEdnRef;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class DocxDocumentExtractor implements WordDocumentExtractor {
    @Override
    public ExtractionResult extract(Path file) throws IOException {
        try (OPCPackage pkg = OPCPackage.open(file.toFile());
             XWPFDocument document = new XWPFDocument(pkg)) {

            List<RawParagraph> output = new ArrayList<>();
            int sourceCount = document.getParagraphs().size();

            for (IBodyElement element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph) {
                    output.add(convert(paragraph));
                } else if (element instanceof XWPFTable table) {
                    output.addAll(convertTable(table));
                }
            }
            return new ExtractionResult(sourceCount, output);
        } catch (Exception e) {
            throw new IOException("Failed to parse DOCX/OOXML file: " + file, e);
        }
    }

    private RawParagraph convert(XWPFParagraph paragraph) {
        StringBuilder text = new StringBuilder();
        List<String> refs = new ArrayList<>();
        int textRuns = 0;
        int struckRuns = 0;

        for (XWPFRun run : paragraph.getRuns()) {
            String runText = run.text();
            if (runText != null && !runText.isBlank()) {
                text.append(runText);
                textRuns++;
                if (run.isStrikeThrough() || run.isDoubleStrikeThrough()) {
                    struckRuns++;
                }
            }

            for (CTFtnEdnRef ref : run.getCTR().getEndnoteReferenceList()) {
                refs.add(ref.getId().toString());
            }
            for (CTFtnEdnRef ref : run.getCTR().getFootnoteReferenceList()) {
                refs.add(ref.getId().toString());
            }
        }

        boolean fullyStruck = textRuns > 0 && struckRuns == textRuns;
        boolean partiallyStruck = struckRuns > 0 && struckRuns < textRuns;
        return new RawParagraph(text.toString(), fullyStruck, partiallyStruck, List.copyOf(refs));
    }

    private List<RawParagraph> convertTable(XWPFTable table) {
        List<RawParagraph> rows = new ArrayList<>();
        for (XWPFTableRow row : table.getRows()) {
            List<String> cells = new ArrayList<>();
            for (XWPFTableCell cell : row.getTableCells()) {
                cells.add(cell.getText());
            }
            rows.add(new RawParagraph("[TABLE] " + String.join(" | ", cells), false, false, List.of()));
        }
        return rows;
    }
}
