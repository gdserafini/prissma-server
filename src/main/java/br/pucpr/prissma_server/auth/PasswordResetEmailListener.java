package br.pucpr.prissma_server.auth;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PasswordResetEmailListener {

    private final JavaMailSender mailSender;

    public PasswordResetEmailListener(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePasswordResetEmail(PasswordResetEmailEvent event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(event.to());
        message.setSubject("Prissma - Password Reset");
        message.setText(
                "You requested a password reset.\n\n" +
                "Click the link below to reset your password:\n" +
                event.resetLink() + "\n\n" +
                "This link expires in 15 minutes.\n" +
                "If you did not request this, ignore this email."
        );
        mailSender.send(message);
    }
}
