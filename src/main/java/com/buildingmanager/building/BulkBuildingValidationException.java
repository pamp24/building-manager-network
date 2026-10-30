package com.buildingmanager.building;

import java.util.List;

public class BulkBuildingValidationException extends RuntimeException {
    public record Error(int row, String field, String message) {}

    private final List<Error> errors;

    public BulkBuildingValidationException(List<Error> errors) {
        super("Bulk building validation failed");
        this.errors = List.copyOf(errors);
    }

    public List<Error> getErrors() {
        return errors;
    }
}
