package br.pucpr.prissma_server.attachments.storage;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

/**
 * Validação de arquivo enviado: tipo declarado, tamanho e assinatura real.
 *
 * Nasceu dentro do {@code AttachmentService} e saiu de lá quando a prévia por IA
 * passou a receber upload também — a checagem de magic bytes é o que impede um
 * executável renomeado de entrar como {@code image/png}, e não pode existir em
 * duas cópias que divergem com o tempo.
 */
@Component
public class UploadedFileValidator {

    public static final String DOCX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    /** Tipos que o gerador de imagens aceita como entrada. */
    public static final Set<String> IMAGE_CONTENT_TYPES = Set.of("image/png", "image/jpeg", "image/webp");

    private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46};
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] GIF87_MAGIC = {0x47, 0x49, 0x46, 0x38, 0x37, 0x61};
    private static final byte[] GIF89_MAGIC = {0x47, 0x49, 0x46, 0x38, 0x39, 0x61};
    private static final byte[] BMP_MAGIC = {0x42, 0x4D};
    private static final byte[] TIFF_LE_MAGIC = {0x49, 0x49, 0x2A, 0x00};
    private static final byte[] TIFF_BE_MAGIC = {0x4D, 0x4D, 0x00, 0x2A};
    private static final byte[] WEBP_RIFF_MAGIC = {0x52, 0x49, 0x46, 0x46};
    private static final byte[] WEBP_FORMAT_TAG = {0x57, 0x45, 0x42, 0x50};
    private static final byte[] ZIP_MAGIC = {0x50, 0x4B, 0x03, 0x04};

    private final StorageProperties storageProperties;

    public UploadedFileValidator(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    /** Corta o {@code ;charset=...} e normaliza a caixa. */
    public String normalizeContentType(String raw) {
        if (raw == null) {
            return null;
        }
        int semi = raw.indexOf(';');
        return (semi >= 0 ? raw.substring(0, semi) : raw).trim().toLowerCase();
    }

    public void validateContentType(String contentType) {
        List<String> allowed = storageProperties.getLimits().getAllowedContentTypes();
        if (contentType == null || !allowed.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Unsupported file type. Allowed: " + allowed);
        }
    }

    /** Recorte do anterior para quem só aceita imagem (a prévia por IA). */
    public void validateImageContentType(String contentType, String field) {
        if (contentType == null || !IMAGE_CONTENT_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "O arquivo '" + field + "' deve ser PNG, JPEG ou WebP");
        }
    }

    public void validateSize(long size) {
        long max = storageProperties.getLimits().getMaxFileSizeBytes();
        if (size > max) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Arquivo excede o tamanho máximo permitido de " + max + " bytes");
        }
    }

    /**
     * Confere que o começo do arquivo bate com o tipo declarado. Consome os
     * primeiros bytes do stream, então quem grava precisa abrir outro.
     */
    public void verifyMagicBytes(InputStream in, String contentType) throws IOException {
        int needed = switch (contentType) {
            case "image/webp" -> 12;
            case "image/png" -> 8;
            case "image/gif" -> 6;
            case "application/pdf", "image/tiff", DOCX_CONTENT_TYPE -> 4;
            case "image/jpeg" -> 3;
            case "image/bmp" -> 2;
            default -> 0;
        };
        if (needed == 0) {
            return;
        }

        byte[] header = in.readNBytes(needed);
        if (header.length < needed || !matchesSignature(header, contentType)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "File content does not match declared type");
        }
    }

    public String resolveExtension(String contentType) {
        return switch (contentType) {
            case "application/pdf" -> "pdf";
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            case "image/gif" -> "gif";
            case "image/bmp" -> "bmp";
            case "image/tiff" -> "tiff";
            case "image/webp" -> "webp";
            case DOCX_CONTENT_TYPE -> "docx";
            default -> "bin";
        };
    }

    public String sanitizeFileName(String name) {
        if (name == null || name.isBlank()) {
            return "file";
        }
        String base = name.replace("\\", "/");
        int slash = base.lastIndexOf('/');
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        base = base.replaceAll("[\\r\\n\\t]", "_");
        if (base.length() > 255) {
            base = base.substring(0, 255);
        }
        return base;
    }

    private boolean matchesSignature(byte[] header, String contentType) {
        return switch (contentType) {
            case "application/pdf" -> startsWith(header, PDF_MAGIC);
            case "image/png" -> startsWith(header, PNG_MAGIC);
            case "image/jpeg" -> startsWith(header, JPEG_MAGIC);
            case "image/gif" -> startsWith(header, GIF87_MAGIC) || startsWith(header, GIF89_MAGIC);
            case "image/bmp" -> startsWith(header, BMP_MAGIC);
            case "image/tiff" -> startsWith(header, TIFF_LE_MAGIC) || startsWith(header, TIFF_BE_MAGIC);
            case "image/webp" -> startsWith(header, WEBP_RIFF_MAGIC) && regionEquals(header, 8, WEBP_FORMAT_TAG);
            case DOCX_CONTENT_TYPE -> startsWith(header, ZIP_MAGIC);
            default -> true;
        };
    }

    private boolean startsWith(byte[] header, byte[] expected) {
        return regionEquals(header, 0, expected);
    }

    private boolean regionEquals(byte[] header, int offset, byte[] expected) {
        if (header.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (header[offset + i] != expected[i]) {
                return false;
            }
        }
        return true;
    }
}
