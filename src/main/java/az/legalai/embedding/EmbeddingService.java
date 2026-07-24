package az.legalai.embedding;

import java.util.List;

public interface EmbeddingService {
    int dimension();

    List<float[]> embedBatch(List<String> texts);

    default float[] embed(String text) {
        return embedBatch(List.of(text)).getFirst();
    }
}
