    package com.buildingmanager.invite;

    import lombok.AllArgsConstructor;
    import lombok.Data;

    @Data
    @AllArgsConstructor
    public class InviteResponseDTO {
        private String email;
        private String role;
        private Integer apartmentId;
        private String token;
        private String status;
    }
