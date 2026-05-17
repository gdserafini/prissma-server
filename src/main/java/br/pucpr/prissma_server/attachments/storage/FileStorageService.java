package br.pucpr.prissma_server.attachments.storage;

import org.springframework.core.io.Resource;

import java.io.InputStream;

public interface FileStorageService {

    StoredFile store(InputStream in, String extension, String namespace);

    Resource load(String storageKey);

    void delete(String storageKey);
}
