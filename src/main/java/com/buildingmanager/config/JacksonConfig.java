package com.buildingmanager.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Configuration
public class JacksonConfig {

    /**
     * Επιτρέπει την ανάλυση ημερομηνιών τύπου "YYYY-MM-DD" (χωρίς ώρα)
     * σε LocalDateTime (τα μεσάνυχτα).
     *
     * Χρησιμοποιούμε deserializersByType(...), που προσθέτει ΜΟΝΟ επιπλέον
     * deserializers πάνω από τα προεπιλεγμένα modules. Δεν αντικαθιστά το
     * ενσωματωμένο JavaTimeModule, οπότε η σειριοποίηση των ημερομηνιών
     * στις απαντήσεις (GET) δεν σπάει.
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer localDateTimeCustomizer() {
        return builder -> builder.deserializersByType(Map.of(
                LocalDateTime.class, new LenientLocalDateTimeDeserializer(),
                LocalDate.class, new LenientLocalDateDeserializer()
        ));
    }

    static class LenientLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (p.currentToken() == JsonToken.VALUE_STRING) {
                String text = p.getText().trim();
                if (text.isEmpty()) {
                    return null;
                }
                if (text.length() == 10) {
                    return LocalDate.parse(text).atStartOfDay();
                }
                if (text.length() == 16) {
                    return LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
                }
                return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
            return (LocalDateTime) ctxt.handleUnexpectedToken(LocalDateTime.class, p);
        }
    }

    static class LenientLocalDateDeserializer extends JsonDeserializer<LocalDate> {
        @Override
        public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (p.currentToken() == JsonToken.VALUE_STRING) {
                String text = p.getText().trim();
                if (text.isEmpty()) {
                    return null;
                }
                return LocalDate.parse(text);
            }
            return (LocalDate) ctxt.handleUnexpectedToken(LocalDate.class, p);
        }
    }
}
