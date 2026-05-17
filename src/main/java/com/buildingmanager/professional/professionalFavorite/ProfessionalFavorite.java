package com.buildingmanager.professional.professionalFavorite;

import com.buildingmanager.professional.ProfessionalBusiness;
import com.buildingmanager.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "professional_favorites",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "professional_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfessionalFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professional_id")
    private ProfessionalBusiness professional;
}
