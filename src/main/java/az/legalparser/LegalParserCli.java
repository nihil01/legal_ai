package az.legalparser;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

@Command(
        name = "legal-parser",
        mixinStandardHelpOptions = true,
        version = "legal-parser 0.1.0",
        description = "Reads DOC/DOCX legal documents and exports cleaned paragraphs to JSON."
)
public final class LegalParserCli implements Callable<Integer> {
    @Parameters(index = "0", description = "Input DOC/DOCX file or directory")
    private Path input;

    @Option(names = {"-o", "--output"}, description = "Output directory", defaultValue = "output")
    private Path outputDirectory;

    @Option(names = {"-r", "--recursive"}, description = "Scan subdirectories")
    private boolean recursive;

    private final LegalDocumentParser parser = new LegalDocumentParser();
    private final JsonExporter exporter = new JsonExporter();

    public static void main(String[] args) {
        int exitCode = new CommandLine(new LegalParserCli()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        if (!Files.exists(input)) {
            System.err.println("Input does not exist: " + input);
            return 2;
        }

        List<Path> files = collectFiles(input);
        if (files.isEmpty()) {
            System.err.println("No DOC/DOCX files found: " + input);
            return 3;
        }

        int failed = 0;
        for (Path file : files) {
            try {
                process(file);
            } catch (Exception e) {
                failed++;
                System.err.printf("[ERROR] %s: %s%n", file, e.getMessage());
            }
        }

        System.out.printf("Finished. Success: %d, failed: %d%n", files.size() - failed, failed);
        return failed == 0 ? 0 : 1;
    }

    private void process(Path file) throws IOException {
        System.out.println("Reading: " + file);
        ParsedDocument parsed = parser.parse(file);

        String outputName = stripExtension(file.getFileName().toString()) + ".json";
        Path output = outputDirectory.resolve(outputName);
        exporter.write(parsed, output);

        long articles = parsed.paragraphs().stream()
                .filter(p -> p.kind() == ParagraphKind.ARTICLE)
                .count();
        long numbered = parsed.paragraphs().stream()
                .filter(p -> p.kind() == ParagraphKind.NUMBERED_PARAGRAPH)
                .count();

        System.out.printf(
                "  format=%s, sourceParagraphs=%d, cleaned=%d, articles=%d, numbered=%d%n",
                parsed.detectedFormat(),
                parsed.sourceParagraphCount(),
                parsed.outputParagraphCount(),
                articles,
                numbered
        );
        System.out.println("  output=" + output.toAbsolutePath());
    }

    private List<Path> collectFiles(Path path) throws IOException {
        if (Files.isRegularFile(path)) {
            return List.of(path);
        }

        int maxDepth = recursive ? Integer.MAX_VALUE : 1;
        try (Stream<Path> stream = Files.walk(path, maxDepth)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(this::hasSupportedExtension)
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }
    }

    private boolean hasSupportedExtension(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".doc") || name.endsWith(".docx");
    }

    private String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
