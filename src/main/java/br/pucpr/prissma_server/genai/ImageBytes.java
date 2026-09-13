package br.pucpr.prissma_server.genai;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Uma imagem já lida para memória.
 *
 * Existe porque a geração deixou de acontecer dentro da requisição: o job
 * assíncrono lê o arquivo do storage e não tem {@code MultipartFile} nenhum para
 * entregar ao gateway.
 */
public record ImageBytes(byte[] content, String contentType) {

    public static ImageBytes from(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String contentType = file.getContentType() == null
                ? "application/octet-stream"
                : file.getContentType();
        return new ImageBytes(file.getBytes(), contentType);
    }

    public long size() {
        return content.length;
    }
}
