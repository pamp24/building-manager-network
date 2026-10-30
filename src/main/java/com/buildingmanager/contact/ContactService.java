package com.buildingmanager.contact;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;

@Service
public class ContactService {
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final String adminEmail;
    private final String senderEmail;

    public ContactService(JavaMailSender mailSender, SpringTemplateEngine templateEngine,
                          @Value("${contact.admin-email:}") String adminEmail,
                          @Value("${contact.sender-email:}") String senderEmail) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.adminEmail = adminEmail;
        this.senderEmail = senderEmail;
    }

    public void send(ContactRequest request) {
        try {
            validateConfiguredAddress(adminEmail);
            validateConfiguredAddress(senderEmail);
            Context adminContext = new Context();
            adminContext.setVariable("contact", request);
            sendMail(adminEmail, "Νέο μήνυμα επικοινωνίας", templateEngine.process("contact-admin", adminContext));
            Context userContext = new Context();
            userContext.setVariable("firstName", request.firstName());
            sendMail(request.email(), "Επιβεβαίωση επικοινωνίας", templateEngine.process("contact-confirmation", userContext));
        } catch (MessagingException | MailException | IllegalArgumentException ex) {
            // SMTP errors may contain recipient addresses or message content; do not expose or log them.
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Contact delivery unavailable");
        }
    }

    private void validateConfiguredAddress(String address) throws MessagingException {
        if (address == null || address.isBlank() || address.contains("\r") || address.contains("\n")) {
            throw new IllegalArgumentException("Invalid mail configuration");
        }
        InternetAddress[] parsed = InternetAddress.parse(address, true);
        if (parsed.length != 1 || parsed[0].getPersonal() != null || !address.equals(parsed[0].getAddress())) {
            throw new IllegalArgumentException("Invalid mail configuration");
        }
        parsed[0].validate();
    }

    private void sendMail(String to, String subject, String text) throws MessagingException {
        var message = mailSender.createMimeMessage();
        var helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
        helper.setFrom(senderEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text, true);
        mailSender.send(message);
    }
}
