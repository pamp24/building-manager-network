package com.buildingmanager.contact;

import com.buildingmanager.config.ApiVersionConfig;
import com.buildingmanager.handler.GlobalExceptionHandler;
import com.buildingmanager.security.JwtFilter;
import com.buildingmanager.security.JwtService;
import com.buildingmanager.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ContextConfiguration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContactController.class)
@ContextConfiguration(classes = ContactControllerTest.TestApp.class)
class ContactControllerTest {
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({ContactController.class, GlobalExceptionHandler.class, ApiVersionConfig.class, SecurityConfig.class, JwtFilter.class})
    static class TestApp {}
    @Autowired MockMvc mvc;
    @MockBean ContactService service;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService users;
    @MockBean AuthenticationProvider provider;

    private static final String VALID = "{\"firstName\":\"Anna\",\"lastName\":\"Test\",\"email\":\"anna@example.com\",\"message\":\"Hello\",\"privacyAccepted\":true}";

    @Test
    void publicPostIgnoresStaleTokenButOtherMethodsAndPathsStayProtected() throws Exception {
        mvc.perform(post("/api/v1/contact").servletPath("/api/v1/contact").header("Authorization", "Bearer expired")
                .contentType(MediaType.APPLICATION_JSON).content(VALID)).andExpect(status().isNoContent());
        verify(service).send(any());
        verifyNoInteractions(jwtService);
        mvc.perform(get("/api/v1/contact")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/contact/other")).andExpect(status().isForbidden());
    }

    @Test
    void rejectsInvalidInputWithoutSending() throws Exception {
        for (String body : new String[]{
                VALID.replace("true", "false"),
                VALID.replace(",\"privacyAccepted\":true", ""),
                VALID.replace("anna@example.com", "bad-address"),
                VALID.replace("anna@example.com", "a@example.com,b@example.com"),
                VALID.replace("anna@example.com", "a@example.com\\r\\nBcc:evil@example.com"),
                VALID.replace("Anna", "A\\nB"),
                VALID.replace("Hello", " "),
                VALID.replace("Hello", "x".repeat(5001)),
                VALID.replace("Anna", "x".repeat(101)),
                VALID.replace("\"message\"", "\"phone\":\"123\\n456\",\"message\"")}) {
            mvc.perform(post("/api/v1/contact").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }
}
