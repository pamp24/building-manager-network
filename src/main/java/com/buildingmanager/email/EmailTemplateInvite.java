package com.buildingmanager.email;


public enum EmailTemplateInvite implements EmailTemplateName{
    CONFIRM_EMAIL("confirm-email"),
    INVITE("invite-email");

    private final String name;

    EmailTemplateInvite(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}


