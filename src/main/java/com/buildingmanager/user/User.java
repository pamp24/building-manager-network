package com.buildingmanager.user;


import com.buildingmanager.building.Building;
import com.buildingmanager.buildingMember.BuildingMember;
import com.buildingmanager.common.BaseEntity;
import com.buildingmanager.company.Company;
import com.buildingmanager.role.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name= "_user")
@EntityListeners(AuditingEntityListener.class)
public class User extends BaseEntity implements UserDetails {

    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    @Column(unique = true)
    private String email;
    private String password;
    private String phoneNumber;
    private String profileImageUrl;
    private String address1;
    private String addressNumber1;
    private String address2;
    private String addressNumber2;
    private LocalDateTime lastLoginDate;
    private String country;
    private String state;
    private String city;
    private String region;
    private String postalCode;
    private boolean accountLocked;
    private boolean enable;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BuildingMember> memberships;


    @OneToMany(mappedBy = "manager")
    private List<Building> managedBuildings;

    @ManyToMany
    @JoinTable(
            name = "property_agent_buildings",
            joinColumns = @JoinColumn(name = "agent_id"),
            inverseJoinColumns = @JoinColumn(name = "building_id")
    )
    private Set<Building> assignedBuildings = new HashSet<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.getName()));
    }

    @Override
    public String getPassword() {

        return password;
    }
    @Override
    public String getUsername() {

        return email;
    }

    @Override
    public boolean isAccountNonExpired() {

        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !accountLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enable;
    }

    public String fullName(){
        return firstName + " " + lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;
}