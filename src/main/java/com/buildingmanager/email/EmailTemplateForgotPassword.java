package com.buildingmanager.email;

import lombok.Getter;

@Getter
public enum EmailTemplateForgotPassword implements EmailTemplateName {
    RESET_PASSWORD("reset_password");

    private final String name;

    EmailTemplateForgotPassword(String name) {
        this.name = name;
    }
}
