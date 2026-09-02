package br.pucpr.prissma_server.workspaces;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Mesmo padrão do PasswordResetEmailListener: envia só após o commit. */
@Component
public class InviteEmailListener {

    private final JavaMailSender mailSender;

    public InviteEmailListener(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleInviteEmail(InviteEmailEvent event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(event.to());
        message.setSubject("Prissma - Convite para " + event.workspaceName());
        message.setText(
                "Você foi convidado(a) para o workspace \"" + event.workspaceName() + "\" no Prissma.\n\n" +
                "Clique no link abaixo para aceitar o convite:\n" +
                event.inviteLink() + "\n\n" +
                "O convite expira em 7 dias.\n" +
                "Se você não esperava este convite, ignore este e-mail."
        );
        mailSender.send(message);
    }
}
