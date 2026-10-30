package com.buildingmanager.contact;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.server.ResponseStatusException;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Properties;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ContactServiceTest {
    private final JavaMailSender sender = mock(JavaMailSender.class);
    private final ContactRequest request = new ContactRequest("Anna", "Test", "anna@example.com", null, "<script>hello</script>", true);
    private final SpringTemplateEngine templates = templates();

    private static SpringTemplateEngine templates() {
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    @Test
    void sendsTwoHtmlMessagesToFixedAdminAndUser() throws Exception {
        when(sender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
        var service = new ContactService(sender, templates, "admin@oikio.local", "no-reply@oikio.local");
        service.send(request);
        var messages = org.mockito.ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender, times(2)).send(messages.capture());
        var admin = messages.getAllValues().get(0);
        var user = messages.getAllValues().get(1);
        assertThat(admin.getAllRecipients()[0].toString()).isEqualTo("admin@oikio.local");
        assertThat(user.getAllRecipients()[0].toString()).isEqualTo("anna@example.com");
        admin.saveChanges();
        user.saveChanges();
        assertThat(admin.getContentType()).startsWithIgnoringCase("text/html");
        assertThat(admin.getContent().toString()).contains("&lt;script&gt;hello&lt;/script&gt;", "anna@example.com", "Test").doesNotContain("<script>");
        assertThat(user.getContent().toString()).contains("Σας ευχαριστούμε", "Anna").doesNotContain("hello", "anna@example.com", "Test");
        assertThat(admin.getFrom()[0].toString()).isEqualTo("no-reply@oikio.local");
    }

    @Test
    void missingOrUnsafeConfigurationFailsBeforeSending() {
        for (String admin : new String[]{"", "admin@example.com,evil@example.com", "admin@example.com\r\nBcc: evil@example.com"}) {
            assertThatThrownBy(() -> new ContactService(sender, templates, admin, "sender@example.com").send(request))
                    .isInstanceOf(ResponseStatusException.class).hasMessageNotContaining("evil@example.com");
        }
        verifyNoInteractions(sender);
    }

    @Test
    void secondSendFailureIsSanitizedAndNotReportedAsSuccess() {
        when(sender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
        doNothing().doThrow(new MailSendException("sensitive address anna@example.com")).when(sender).send(any(MimeMessage.class));
        assertThatThrownBy(() -> new ContactService(sender, templates, "admin@example.com", "sender@example.com").send(request))
                .isInstanceOf(ResponseStatusException.class).hasMessageNotContaining("anna@example.com");
        verify(sender, times(2)).send(any(MimeMessage.class));
    }
}
