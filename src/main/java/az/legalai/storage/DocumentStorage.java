package az.legalai.storage;

import java.io.InputStream;

public interface DocumentStorage {
    StoredFile store(String originalFilename, byte[] bytes);

    InputStream load(String storageKey);

    void delete(String storageKey);
}
