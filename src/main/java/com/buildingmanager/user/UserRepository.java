package com.buildingmanager.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    List<User> findByRoleName(String roleName);
    List<User> findByCompanyIdAndRole_Name(Integer companyId, String roleName);

    Optional<User> findByIdAndRole_Name(Integer id, String roleName);

    long countByRole_Name(String roleName);

    long countByLastLoginDateAfter(java.time.LocalDateTime dateTime);

    long countByCreatedDateAfter(java.time.LocalDateTime dateTime);

    @Query("""
    SELECT DATE(u.createdDate), COUNT(u)
    FROM User u
    WHERE u.createdDate >= :fromDate
    GROUP BY DATE(u.createdDate)
    ORDER BY DATE(u.createdDate)
""")
    List<Object[]> countUsersGroupedByDate(@Param("fromDate") LocalDateTime fromDate);

}
