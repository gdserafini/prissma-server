package br.pucpr.prissma_server.attachments.storage;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@EnableConfigurationProperties(StorageProperties.class)
@ConditionalOnProperty(prefix = "app.storage", name = "backend", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private final StorageProperties properties;
    private Path rootPath;

    public LocalFileStorageService(StorageProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        try {
            this.rootPath = Paths.get(properties.getLocal().getRoot()).toAbsolutePath().normalize();
            Files.createDirectories(rootPath);
        } catch (IOException e) {
            throw new StorageException("Failed to initialize local storage root", e);
        }
    }

    @Override
    public StoredFile store(InputStream in, String extension, String namespace) {
        String safeNamespace = sanitizeNamespace(namespace);
        String fileName = UUID.randomUUID() + (extension == null || extension.isBlank() ? "" : "." + extension);
        String storageKey = safeNamespace + "/" + fileName;

        Path target = resolveSafe(storageKey);
        try {
            Files.createDirectories(target.getParent());
            long size = Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            return new StoredFile(storageKey, size);
        } catch (IOException e) {
            throw new StorageException("Failed to store file", e);
        }
    }

    @Override
    public Resource load(String storageKey) {
        Path path = resolveSafe(storageKey);
        if (!Files.exists(path) || !Files.isReadable(path)) {
            throw new StorageException("File not found or not readable: " + storageKey, null);
        }
        return new PathResource(path);
    }

    @Override
    public void delete(String storageKey) {
        Path path = resolveSafe(storageKey);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new StorageException("Failed to delete file: " + storageKey, e);
        }
    }

    private Path resolveSafe(String storageKey) {
        Path resolved = rootPath.resolve(storageKey).normalize();
        if (!resolved.startsWith(rootPath)) {
            throw new StorageException("Storage key escapes root: " + storageKey, null);
        }
        return resolved;
    }

    private String sanitizeNamespace(String namespace) {
        if (namespace == null || namespace.isBlank()) {
            return "misc";
        }
        String cleaned = namespace.replaceAll("[^a-zA-Z0-9/_-]", "_");
        if (cleaned.startsWith("/")) cleaned = cleaned.substring(1);
        return cleaned.isBlank() ? "misc" : cleaned;
    }
}
