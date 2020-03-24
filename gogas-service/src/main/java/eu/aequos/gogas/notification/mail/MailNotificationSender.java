package eu.aequos.gogas.notification.mail;

import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.persistence.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Slf4j
@Component
public class MailNotificationSender {

    private JavaMailSender javaMailSender;

    public MailNotificationSender(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    public void sendResetPasswordMessage(User user, String newPassword) throws GoGasException {
        String[] to = { user.getEmail() };
        String[] cc = {};
        String subject = "Reset password";
        String body = String.format("Ciao %s %s,<br/><br/>la tua nuova password per l'accesso a Go!Gas è <i>%s</i>.<br/>" +
                "Per motivi di sicurezza è consigliato cambiarla al primo login." +
                "<br/><br/><i>Questo messaggio è stato generato automaticamente, si prega di non rispondere.</i>",
                user.getFirstName(), user.getLastName(), newPassword);

        try {
            sendMail(to, cc, subject, body);
        } catch (MessagingException e) {
            throw new GoGasException("Unable to send email message to " + user.getEmail());
        }
    }

    private void sendMail(String[] to, String cc[], String subject, String body) throws MessagingException {
        MimeMessage msg = javaMailSender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(msg, true);
        helper.setFrom("noreply@aequos.bio");
        helper.setTo(to);
        helper.setCc(cc);
        helper.setSubject(subject);
        helper.setText(body, true);

        javaMailSender.send(msg);
    }
}
