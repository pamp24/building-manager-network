package com.buildingmanager.apartment;

import java.util.List;

public class BulkApartmentValidationException extends RuntimeException {
    public record Error(int row, String field, String message) {}

    private final List<Error> errors;

    public BulkApartmentValidationException(List<Error> errors) {
        super("Bulk apartment validation failed");
        this.errors = List.copyOf(errors);
    }

    public List<Error> getErrors() {
        return errors;
    }
}
