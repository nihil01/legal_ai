package az.legalai.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.UUID;

public final class LocalDocumentStorage implements DocumentStorage {
    private final Path root;

    public LocalDocumentStorage(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    public StoredFile store(String filename, byte[] bytes) {
        String ext = "";
        int dot = filename.lastIndexOf('.');
        if (dot >= 0)
            ext = filename.substring(dot).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9.]", "");
        String key = UUID.randomUUID() + ext;
        Path target = resolve(key);
        try {
            Files.createDirectories(root);
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
            return new StoredFile(key);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось сохранить файл", e);
        }
    }

    public InputStream load(String key) {
        try {
            return Files.newInputStream(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось загрузить файл", e);
        }
    }

    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось удалить файл", e);
        }
    }

    private Path resolve(String key) {
        Path value = root.resolve(key).normalize();
        if (!value.startsWith(root)) throw new IllegalArgumentException("Invalid storage key");
        return value;
    }
}
