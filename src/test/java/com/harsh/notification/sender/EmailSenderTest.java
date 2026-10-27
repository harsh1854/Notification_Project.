
package com.harsh.notification.sender;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmailSenderTest {

    private JavaMailSender mailSender;
    private EmailSender emailSender;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        emailSender = new EmailSender(mailSender);
    }

    @Test
    void shouldSendEmailWithCorrectDetails() {
        String recipient = "harsh@example.com";
        String message = "Your order has been placed";

        emailSender.sendNotification(recipient, message);

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage sentEmail = captor.getValue();

        assertEquals(recipient, sentEmail.getTo()[0]);
        assertEquals("Notification System Alert", sentEmail.getSubject());
        assertEquals(message, sentEmail.getText());
    }

    @Test
    void shouldReturnEmailChannel() {
        assertEquals("EMAIL", emailSender.getChannel());
    }

    @Test
    void shouldPropagateMailException() {
        String recipient = "harsh@example.com";
        String message = "Test notification";

        doThrow(new org.springframework.mail.MailSendException("SMTP failed"))
                .when(mailSender)
                .send(any(SimpleMailMessage.class));

        assertThrows(
                org.springframework.mail.MailSendException.class,
                () -> emailSender.sendNotification(recipient, message)
        );
    }
}
