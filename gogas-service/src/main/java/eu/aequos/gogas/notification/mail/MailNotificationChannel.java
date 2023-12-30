package eu.aequos.gogas.notification.mail;

import eu.aequos.gogas.dto.AttachmentDTO;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.notification.NotificationChannel;
import eu.aequos.gogas.notification.builder.OrderNotificationBuilder;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class MailNotificationChannel implements NotificationChannel {

    private final JavaMailSender javaMailSender;
    private final UserRepo userRepo;

    public void sendOrderNotification(Order order, OrderNotificationBuilder notificationBuilder, Set<String> targetUserIds) {
        //TODO: improve email body
        /*List<User> targetUsers = userRepo.findByIdIn(targetUserIds, User.class);
        String notificationHeader = notificationBuilder.getHeading();
        String notificationText = notificationBuilder.formatOrderMessage(order);
        targetUsers.forEach(user -> sendOrderNotificationMessage(user, notificationHeader, notificationText));*/
    }

    /*private void sendOrderNotificationMessage(User user, String subject, String text) {
        String[] to = { user.getEmail() };
        String[] cc = {};
        String body = String.format("Ciao %s %s,<br/><br/>%s." +
                        "<br/><br/><i>Questo messaggio è stato generato automaticamente, si prega di non rispondere.</i>",
                user.getFirstName(), user.getLastName(), text);

        try {
            sendMail(to, cc, subject, body);
        } catch (MessagingException e) {
            log.error("Unable to send email message to " + user.getEmail());
        }
    }*/

    public void sendResetPasswordMessage(User user, String newPassword) throws GoGasException {
        String[] to = { user.getEmail() };
        String[] cc = {};
        String subject = "Reset password";
        String body = String.format("Ciao %s %s,<br/><br/>la tua nuova password per l'accesso a Go!Gas è <i>%s</i>.<br/>" +
                "Per motivi di sicurezza è consigliato cambiarla al primo login." +
                "<br/><br/><i>Questo messaggio è stato generato automaticamente, si prega di non rispondere.</i>",
                user.getFirstName(), user.getLastName(), newPassword);

        try {
            sendMail(to, cc, null, subject, body, null);
        } catch (MessagingException e) {
            throw new GoGasException("Unable to send email message to " + user.getEmail());
        }
    }

    public void sendMail(String[] to, String cc[], String replyTo, String subject, String body, List<AttachmentDTO> attachments) throws MessagingException {
        MimeMessage msg = javaMailSender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(msg, true);
        helper.setFrom("noreply@aequos.bio");
        helper.setTo(to);
        helper.setCc(cc);
        helper.setSubject(subject);
        helper.setText(body, true);

        if (replyTo != null) {
            helper.setReplyTo(replyTo);
        }

        if (attachments != null) {
            attachments.forEach(attachment -> attachment.addToMail(helper));
        }

        javaMailSender.send(msg);
    }
}
