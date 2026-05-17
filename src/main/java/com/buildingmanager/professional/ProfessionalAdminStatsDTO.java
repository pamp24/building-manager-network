package com.buildingmanager.professional;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfessionalAdminStatsDTO {
    private long totalBusinesses;
    private long pendingBusinesses;
    private long approvedBusinesses;
    private long inactiveBusinesses;
    private long totalReviews;
}