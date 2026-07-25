package az.legalai.document.service;

import az.legalai.document.repository.ChunkView;
import java.util.List;

public record ChunkPage(List<ChunkView> items, long total, int page, int pageSize, int totalPages) {

    public ChunkPage {
        items = List.copyOf(items);
        if (total < 0 || page < 0 || pageSize <= 0 || totalPages < 0) {
            throw new IllegalArgumentException("Səhifələmə parametrləri yanlışdır");
        }
    }

    public boolean hasPrevious() {
        return page > 0;
    }

    public boolean hasNext() {
        return page + 1 < totalPages;
    }

    public long firstItemNumber() {
        return total == 0 ? 0 : (long) page * pageSize + 1;
    }

    public long lastItemNumber() {
        return Math.min(total, (long) page * pageSize + items.size());
    }
}
