package br.pucpr.prissma_server.report;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/projects/{projectId}/report")
public class ProjectReportController {

    private final ProjectReportService service;

    public ProjectReportController(ProjectReportService service) {
        this.service = service;
    }

    private Long resolveUserId(Authentication auth) {
        Object principal = auth.getPrincipal();
        if (principal instanceof Long userId) {
            return userId;
        }
        if (principal instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(auth.getName());
    }

    /**
     * Sem "produces = application/pdf" de propósito: o ExceptionHandlerAdvice
     * responde erro como ResponseEntity<String>, e um produces restrito faria um
     * 403/404 falhar a negociação de conteúdo e virar 406. O tipo é definido na
     * resposta de sucesso, como em AttachmentController#download.
     */
    @GetMapping
    public ResponseEntity<byte[]> generate(@PathVariable Long projectId,
                                           @RequestParam(required = false)
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                           @RequestParam(required = false)
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                           Authentication auth) {
        ProjectReportService.ProjectReportPdf pdf =
                service.generate(projectId, from, to, resolveUserId(auth));

        // filename simples, SEM charset: passar UTF-8 aqui faz o Spring emitir só
        // a forma estendida (filename*=UTF-8''...), que o Postman e vários
        // clientes não interpretam — o arquivo era salvo com nome truncado e sem
        // a extensão .pdf. O nome gerado é ASCII puro (id + datas), então a forma
        // simples basta e é entendida por todo mundo. Se algum dia o título da
        // obra entrar no nome, ele precisa ser transliterado para ASCII antes.
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(pdf.fileName())
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(pdf.content());
    }
}
