package com.buildingmanager.email;

import lombok.Getter;

@Getter
public enum EmailTemplateActivateAccount implements EmailTemplateName {
    ACTIVATE_ACCOUNT("activate_account");

    private final String name;

    EmailTemplateActivateAccount(String name) {
        this.name = name;
    }
}
