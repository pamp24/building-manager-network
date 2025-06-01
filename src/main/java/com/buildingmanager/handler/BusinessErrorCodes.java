package com.buildingmanager.handler;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

public enum BusinessErrorCodes {
    NO_CODE(0, NOT_IMPLEMENTED, "Χωρίς Κωδικό"),
    INCORRECT_CURRENT_PASSWORD(300, BAD_REQUEST,"Ο τρέχων κωδικός είναι λάθος"),
    NEW_PASSWORD_DOES_NOT_MATCH(301, BAD_REQUEST,"Ο νέος κωδικός δεν ταιριάζει"),
    ACCOUNT_LOCKED(302, FORBIDDEN, "Ο λογαριασμός χρήστη είναι κλειδωμένος"),
    ACCOUNT_DISABLE(303, FORBIDDEN, "Ο λογαριασμός χρήστη είναι απενεργοποιημένος"),
    BAD_CREDENTIALS(304, FORBIDDEN, "Το όνομα χρήστη και/ή ο κωδικός είναι λάθος");
    @Getter
    private final int code;
    @Getter
    private final String description;
    @Getter
    private final HttpStatus httpStatus;

    BusinessErrorCodes(int code,HttpStatus httpStatus, String description) {
        this.code = code;
        this.description = description;
        this.httpStatus = httpStatus;
    }

}
