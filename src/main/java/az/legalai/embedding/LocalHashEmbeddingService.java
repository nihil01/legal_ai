package az.legalai.embedding;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

public final class LocalHashEmbeddingService implements EmbeddingService {
    private final int dimension;

    public LocalHashEmbeddingService(int dimension) {
        if (dimension < 8) throw new IllegalArgumentException("dimension too small");
        this.dimension = dimension;
    }

    public int dimension() {
        return dimension;
    }

    public List<float[]> embedBatch(List<String> texts) {
        return texts.stream().map(this::vector).toList();
    }

    private float[] vector(String text) {
        float[] v = new float[dimension];
        String normalized =
                Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        String[] terms = normalized.split("[^\\p{L}\\p{N}.]+");
        for (String term : terms) {
            if (term.isBlank()) continue;
            int h = term.hashCode();
            int idx = (h & 0x7fffffff) % dimension;
            v[idx] += ((h >>> 31) == 0 ? 1f : -1f);
        }
        double norm = 0;
        for (float n : v) norm += n * n;
        if (norm > 0) {
            float d = (float) Math.sqrt(norm);
            for (int i = 0; i < v.length; i++) v[i] /= d;
        }
        return v;
    }
}
