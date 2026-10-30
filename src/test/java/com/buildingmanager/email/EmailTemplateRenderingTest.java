package com.buildingmanager.email;

import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplateRenderingTest {
    @Test
    void rendersAccountInviteAndNotificationVariablesSafely() {
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        var context = new Context();
        context.setVariables(Map.of("username", "<b>name</b>", "activation_code", "123456",
                "confirmationUrl", "https://example.com/action?a=1&b=2", "roleLabel", "<role>",
                "contextName", "<building>", "message", "<script>message</script>",
                "buildingName", "<building>", "managerName", "<manager>",
                "buildingAddress", "<address>", "apartmentLabel", "<apartment>"));
        String activation = engine.process("activate_account", context);
        assertThat(activation).contains("&lt;b&gt;name&lt;/b&gt;", "123456", "https://example.com/action?a=1&amp;b=2").doesNotContain("<b>name</b>");
        assertThat(engine.process("reset_password", context)).contains("&lt;b&gt;name&lt;/b&gt;", "https://example.com/action?a=1&amp;b=2");
        assertThat(engine.process("invite-email", context)).contains("&lt;role&gt;", "&lt;building&gt;", "https://example.com/action?a=1&amp;b=2");
        assertThat(engine.process("notification-email", context)).contains("&lt;script&gt;message&lt;/script&gt;", "&lt;building&gt;", "&lt;manager&gt;", "&lt;address&gt;", "&lt;apartment&gt;").doesNotContain("<script>");
        var minimal = new Context();
        minimal.setVariable("username", "User");
        minimal.setVariable("message", "Message");
        assertThat(engine.process("notification-email", minimal)).contains("Message").doesNotContain("Το διαμέρισμά σας");
        minimal.setVariable("confirmationUrl", "https://example.com/invite");
        assertThat(engine.process("invite-email", minimal)).contains("https://example.com/invite").doesNotContain("ρόλος");
    }
}
