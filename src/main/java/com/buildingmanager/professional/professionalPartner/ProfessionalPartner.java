package com.buildingmanager.professional.professionalPartner;

import com.buildingmanager.building.Building;
import com.buildingmanager.professional.ProfessionalBusiness;
import com.buildingmanager.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfessionalPartner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    private Building building;

    @ManyToOne(optional = false)
    private ProfessionalBusiness professional;

    @ManyToOne(optional = false)
    private User addedBy;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}