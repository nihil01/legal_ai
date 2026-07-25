package az.legalai.eqanun.client;

import az.legalai.eqanun.dto.CodexVersions;
import az.legalai.eqanun.parser.EqanunLawCandidate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class HttpEqanunApiClient implements EqanunApiClient {
    private static final DateTimeFormatter EQANUN_DATE =
            DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.ROOT)
                    .withResolverStyle(ResolverStyle.STRICT);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI apiBaseUri;
    private final URI publicBaseUri;
    private final Set<String> allowedDownloadHosts;
    private final long maxDocumentBytes;
    private final Duration requestTimeout;

    public HttpEqanunApiClient(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            URI apiBaseUri,
            URI publicBaseUri,
            Set<String> allowedDownloadHosts,
            long maxDocumentBytes,
            Duration requestTimeout) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.apiBaseUri = requireAbsoluteBase(apiBaseUri, "API");
        this.publicBaseUri = requireAbsoluteBase(publicBaseUri, "public");
        this.allowedDownloadHosts = Set.copyOf(allowedDownloadHosts);
        if (this.allowedDownloadHosts.isEmpty()) {
            throw new IllegalArgumentException("E-qanun download host allowlist must not be empty");
        }
        if (maxDocumentBytes <= 0 || maxDocumentBytes >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "E-qanun document size limit must be between 1 and Integer.MAX_VALUE - 1");
        }
        this.maxDocumentBytes = maxDocumentBytes;
        if (requestTimeout == null || requestTimeout.isZero() || requestTimeout.isNegative()) {
            throw new IllegalArgumentException("E-qanun request timeout must be positive");
        }
        this.requestTimeout = requestTimeout;
    }

    @Override
    public Optional<EqanunLawCandidate> findLatestVersion(String codexId) {
        validateCodexId(codexId);
        HttpRequest request =
                HttpRequest.newBuilder(apiBaseUri.resolve("getVersions?id=" + codexId))
                        .timeout(requestTimeout)
                        .header("Accept", "application/json")
                        .GET()
                        .build();
        String body = send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        CodexVersions response;
        try {
            response = objectMapper.readValue(body, CodexVersions.class);
        } catch (IOException exception) {
            throw new EqanunClientException("Invalid e-qanun versions response", exception);
        }
        if (response.codexList() == null) return Optional.empty();

        return response.codexList().stream()
                .filter(version -> version.id() != null)
                .filter(version -> version.title() != null && !version.title().isBlank())
                .filter(version -> version.effectDate() != null && !version.effectDate().isBlank())
                .map(version -> toCandidate(codexId, version))
                .max(
                        Comparator.comparing(EqanunLawCandidate::effectiveDate)
                                .thenComparing(
                                        candidate ->
                                                Integer.parseInt(candidate.externalVersionId())));
    }

    @Override
    public EqanunDocumentPayload downloadCurrentDocument(String codexId) {
        validateCodexId(codexId);
        HttpRequest linkRequest =
                HttpRequest.newBuilder(apiBaseUri.resolve("downloadWord/" + codexId))
                        .timeout(requestTimeout)
                        .header("Accept", "text/plain")
                        .GET()
                        .build();
        String rawLink =
                send(linkRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                        .trim();
        if (rawLink.length() >= 2 && rawLink.startsWith("\"") && rawLink.endsWith("\"")) {
            rawLink = rawLink.substring(1, rawLink.length() - 1);
        }
        URI downloadUri;
        try {
            downloadUri = URI.create(rawLink);
        } catch (IllegalArgumentException exception) {
            throw new EqanunClientException("E-qanun returned an invalid download URL", exception);
        }
        validateDownloadUri(downloadUri);

        HttpRequest documentRequest =
                HttpRequest.newBuilder(downloadUri)
                        .timeout(requestTimeout)
                        .header(
                                "Accept",
                                "application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/octet-stream")
                        .GET()
                        .build();
        try {
            HttpResponse<InputStream> response =
                    httpClient.send(documentRequest, HttpResponse.BodyHandlers.ofInputStream());
            ensureSuccess(response.statusCode(), downloadUri);
            try (InputStream input = response.body()) {
                byte[] bytes = input.readNBytes(Math.toIntExact(maxDocumentBytes + 1));
                if (bytes.length > maxDocumentBytes) {
                    throw new EqanunClientException(
                            "E-qanun document exceeds configured size limit");
                }
                return classifyDocument(codexId, bytes);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new EqanunClientException("E-qanun document download was interrupted", exception);
        } catch (IOException exception) {
            throw new EqanunClientException("Failed to download e-qanun document", exception);
        }
    }

    private EqanunDocumentPayload classifyDocument(String codexId, byte[] bytes) {
        if (bytes.length >= 4
                && (bytes[0] & 0xff) == 0xd0
                && (bytes[1] & 0xff) == 0xcf
                && (bytes[2] & 0xff) == 0x11
                && (bytes[3] & 0xff) == 0xe0) {
            return new EqanunDocumentPayload(
                    bytes, "eqanun-" + codexId + ".doc", "application/msword");
        }
        if (bytes.length >= 4
                && bytes[0] == 'P'
                && bytes[1] == 'K'
                && ((bytes[2] == 3 && bytes[3] == 4)
                        || (bytes[2] == 5 && bytes[3] == 6)
                        || (bytes[2] == 7 && bytes[3] == 8))) {
            return new EqanunDocumentPayload(
                    bytes,
                    "eqanun-" + codexId + ".docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        }
        throw new EqanunClientException("E-qanun returned an unsupported document format");
    }

    private EqanunLawCandidate toCandidate(String codexId, CodexVersions.Codex version) {
        try {
            return new EqanunLawCandidate(
                    codexId,
                    version.id().toString(),
                    version.title().trim(),
                    publicBaseUri.resolve(codexId).toString(),
                    LocalDate.parse(version.effectDate(), EQANUN_DATE));
        } catch (DateTimeParseException exception) {
            throw new EqanunClientException(
                    "Invalid e-qanun effectDate for codex " + codexId + ": " + version.effectDate(),
                    exception);
        }
    }

    private <T> T send(HttpRequest request, HttpResponse.BodyHandler<T> handler) {
        try {
            HttpResponse<T> response = httpClient.send(request, handler);
            ensureSuccess(response.statusCode(), request.uri());
            return response.body();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new EqanunClientException("E-qanun request was interrupted", exception);
        } catch (IOException exception) {
            throw new EqanunClientException("E-qanun request failed", exception);
        }
    }

    private void ensureSuccess(int statusCode, URI uri) {
        if (statusCode < 200 || statusCode >= 300) {
            throw new EqanunClientException(
                    "E-qanun request failed with HTTP " + statusCode + " for " + uri);
        }
    }

    private void validateDownloadUri(URI uri) {
        String host = uri.getHost();
        if (uri.getUserInfo() != null
                || host == null
                || !allowedDownloadHosts.contains(host.toLowerCase(Locale.ROOT))) {
            throw new EqanunClientException(
                    "E-qanun returned a download URL from an untrusted host");
        }
        if ("https".equalsIgnoreCase(uri.getScheme())) return;
        if ("http".equalsIgnoreCase(uri.getScheme()) && isLoopback(host)) return;
        throw new EqanunClientException("E-qanun download URL must use HTTPS");
    }

    private boolean isLoopback(String host) {
        try {
            return InetAddress.getByName(host).isLoopbackAddress();
        } catch (IOException exception) {
            return false;
        }
    }

    private static URI requireAbsoluteBase(URI uri, String name) {
        if (uri == null
                || !uri.isAbsolute()
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || !("https".equalsIgnoreCase(uri.getScheme())
                        || "http".equalsIgnoreCase(uri.getScheme()))) {
            throw new IllegalArgumentException(
                    "E-qanun "
                            + name
                            + " base URI must be an absolute HTTP(S) URL without credentials");
        }
        String value = uri.toString();
        return value.endsWith("/") ? uri : URI.create(value + "/");
    }

    private void validateCodexId(String codexId) {
        if (codexId == null || !codexId.matches("[0-9]+")) {
            throw new IllegalArgumentException("E-qanun codex id must be numeric");
        }
    }
}
