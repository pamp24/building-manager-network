package com.buildingmanager.apartment;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/apartments/building/{buildingId}/bulk")
@RequiredArgsConstructor
public class BulkApartmentController {
    private final BulkApartmentService service;

    @PostMapping
    public ResponseEntity<List<Integer>> importApartments(@PathVariable Integer buildingId,
            @RequestBody List<ApartmentRequest> requests, Authentication authentication) {
        return ResponseEntity.ok(service.importApartments(buildingId, requests, authentication));
    }

    @ExceptionHandler(BulkApartmentValidationException.class)
    public ResponseEntity<Map<String, Object>> validationError(BulkApartmentValidationException ex) {
        return ResponseEntity.badRequest().body(Map.of("errors", ex.getErrors()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> invalidBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(Map.of("errors", List.of(
                new BulkApartmentValidationException.Error(0, "body", "Invalid JSON or field type"))));
    }
}
