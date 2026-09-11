package br.pucpr.prissma_server.design;

import br.pucpr.prissma_server.config.AsyncConfig;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Mesmo padrão do InviteEmailListener: só age depois do commit.
 *
 * Aqui isso não é preferência — a linha da prévia precisa existir no banco antes
 * de o job tentar carregá-la, e ele roda em outra thread.
 */
@Component
public class EnvironmentPreviewListener {

    private final EnvironmentPreviewJobService jobService;

    public EnvironmentPreviewListener(EnvironmentPreviewJobService jobService) {
        this.jobService = jobService;
    }

    @Async(AsyncConfig.PREVIEW_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePreviewRequested(EnvironmentPreviewRequestedEvent event) {
        jobService.process(event.previewId());
    }
}
