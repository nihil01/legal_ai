# Legal Parser CLI

Java 21 command-line utility for reading `.doc`/`.docx` legal documents, cleaning paragraph text, classifying basic legal structure, and exporting JSON.

## Build

```bash
mvn clean package
```

The executable fat JAR will be created at:

```text
target/legal-parser.jar
```

## Parse one file

```bash
java -jar target/legal-parser.jar "/path/to/document.doc" -o output
```

## Parse all DOC/DOCX files in a directory

```bash
java -jar target/legal-parser.jar ./documents -o output
```

Recursive scan:

```bash
java -jar target/legal-parser.jar ./documents -o output --recursive
```

The utility detects actual file format by magic bytes, so a DOCX file incorrectly named `.doc` is handled correctly.

## Run with Docker

Put files into `./documents`, then run:

```bash
docker compose run --rm legal-parser
```

Or parse one mounted file directly:

```bash
docker build -t legal-parser .
docker run --rm \
  -v "$PWD/documents:/documents:ro" \
  -v "$PWD/output:/output" \
  legal-parser "/documents/example.doc" -o /output
```
