package com.buildingmanager.role;


import com.buildingmanager.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "role")
@EntityListeners(AuditingEntityListener.class)
public class Role{

    @Id
    @GeneratedValue
    private int id;
    @Column(unique = true)
    private String name;
    @OneToMany(mappedBy = "role")
    private List<User> users;
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;
    @LastModifiedDate
    @Column(insertable = false)
    private LocalDateTime lastModifiedDate;


    public String getName() {
        return name;
    }
}


