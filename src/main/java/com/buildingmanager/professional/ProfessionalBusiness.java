package com.buildingmanager.professional;

import com.buildingmanager.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "professional_businesses")
public class ProfessionalBusiness {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String businessName;

    @Column(nullable = false)
    private String ownerFullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProfessionalCategory category;

    @Column(length = 2000)
    private String description;

    private String phone;
    private String email;
    private String website;
    private String country;
    private String city;
    private String region;
    private String area;

    private String address;

    private String taxNumber;

    private boolean verified;
    private boolean active;

    private Double ratingAverage;
    private Integer reviewCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Column(columnDefinition = "TEXT")
    private String workingHours;

    @Column(columnDefinition = "TEXT")
    private String serviceAreas;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.verified = false;
        this.active = false;
        this.ratingAverage = 0.0;
        this.reviewCount = 0;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
