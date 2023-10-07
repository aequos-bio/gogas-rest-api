package eu.aequos.gogas.notification.mail;

import eu.aequos.gogas.dto.AttachmentDTO;
import eu.aequos.gogas.notification.builder.AccountedNotificationBuilder;
import eu.aequos.gogas.notification.builder.OpenedNotificationBuilder;
import eu.aequos.gogas.notification.builder.OrderNotificationBuilder;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.OrderType;
import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.repository.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.javamail.JavaMailSender;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MailNotificationChannelTest {

    @Captor
    ArgumentCaptor<InternetAddress> recepientAddressCaptor;
    @Captor
    ArgumentCaptor<InternetAddress> fromAddressCaptor;

    @Captor
    ArgumentCaptor<MimeMultipart> mailContentCaptor;

    @Mock
    private JavaMailSender javaMailSender;
    private UserRepo userRepo;

    private MailNotificationChannel underTest;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        userRepo = mock(UserRepo.class);
        underTest = new MailNotificationChannel(javaMailSender, userRepo);
    }

    @Test
    void canSendOrderNotification() throws MessagingException {
        // given
        String to = "to@mail.com";
        String cc = "cc@mail.com";
        String replyTo = "replyto@mail.com";
        String subject = "subject";
        String body = "this is the body of the email";
        String contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        String contentString = "content";
        String name = "attachment name";
        MimeMessage mimeMessage = mock(MimeMessage.class);

        List<AttachmentDTO> attachments = List.of(new AttachmentDTO(contentString.getBytes(), contentType, name));

        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // when
        underTest.sendMail(to, cc, replyTo, subject, body, attachments);

        // then
        verify(javaMailSender).createMimeMessage();
        verify(javaMailSender).send(mimeMessage);
    }

    @ParameterizedTest
    @MethodSource("OrdersAndMessages")
    void canSendOrderNotificationMessage(Order order, OrderNotificationBuilder orderNotificationBuilder) throws MessagingException {
        // then
        Set<String> targetUserIds = Set.of("userId");
        User user1 = new User();
        user1.setFirstName("name1");
        user1.setLastName("lastname1");
        user1.setEmail("email1@mail.com");
        User user2 = new User();
        user2.setEmail("email2@mail.com");
        List<User> targetUsers = List.of(user1, user2);
        MimeMessage mimeMessage = mock(MimeMessage.class);

        when(userRepo.findByIdIn(targetUserIds, User.class)).thenReturn(targetUsers);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // when
        underTest.sendOrderNotification(order, orderNotificationBuilder, targetUserIds);

        // then
        verify(userRepo).findByIdIn(targetUserIds, User.class);
        verify(mimeMessage, times(2)).setRecipient(eq(Message.RecipientType.TO), recepientAddressCaptor.capture());

        List<InternetAddress> addresses = recepientAddressCaptor.getAllValues();
        assertEquals(addresses.size(), 2);
        assertTrue(addresses.stream().anyMatch(add -> add.getAddress().equals(user1.getEmail())));
        assertTrue(addresses.stream().anyMatch(add -> add.getAddress().equals(user2.getEmail())));

        verify(mimeMessage, times(0)).setRecipient(eq(Message.RecipientType.CC), any());
        verify(mimeMessage, times(0)).setReplyTo(any());
        verify(mimeMessage, times(2)).setSubject(orderNotificationBuilder.getHeading());

        verify(mimeMessage, times(2)).setFrom(fromAddressCaptor.capture());
        List<InternetAddress> froms = fromAddressCaptor.getAllValues();
        assertEquals(froms.size(), 2);
        assertTrue(froms.stream().allMatch(from -> from.getAddress().equals("noreply@aequos.bio")));

        verify(mimeMessage, times(2)).setContent(mailContentCaptor.capture());
        List<MimeMultipart> notificationTexts = mailContentCaptor.getAllValues();
        assertEquals(notificationTexts.size(), 2);

        verify(javaMailSender, times(2)).send(mimeMessage);
    }

    private static Stream<Arguments> OrdersAndMessages() {
        Order openedOrder = new Order();
        openedOrder.setStatusCode(0);
        openedOrder.setDeliveryDate(LocalDate.now());
        OrderType orderType1 = new OrderType();
        orderType1.setDescription("description order 1");
        openedOrder.setOrderType(orderType1);

       /* Order closedOrder = new Order();
        closedOrder.setStatusCode(1);
        closedOrder.setDeliveryDate(LocalDate.now());
        OrderType orderType2 = new OrderType();
        orderType2.setDescription("description order 2");
        closedOrder.setOrderType(orderType1);*/

        Order accountedOrder = new Order();
        accountedOrder.setStatusCode(2);
        accountedOrder.setDeliveryDate(LocalDate.now());
        OrderType orderType3 = new OrderType();
        orderType3.setDescription("description order 3");
        accountedOrder.setOrderType(orderType1);

       /* Order cancelledOrder = new Order();
        cancelledOrder.setStatusCode(3);
        cancelledOrder.setDeliveryDate(LocalDate.now());
        OrderType orderType4 = new OrderType();
        orderType4.setDescription("description order 4");
        cancelledOrder.setOrderType(orderType1);*/

        return Stream.of(
                Arguments.of(openedOrder, new OpenedNotificationBuilder()),
                // non testabile, non capisco quale builder dovrebbe essere coinvolto
                //Arguments.of(closedOrder, ),
                //Arguments.of(cancelledOrder, ),
                Arguments.of(accountedOrder, new AccountedNotificationBuilder(null, null))
        );
    }
}