package az.legalai.eqanun.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CodexVersions(@JsonProperty("data") List<Codex> codexList) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Codex(Integer id, String title, String effectDate) {}
}
