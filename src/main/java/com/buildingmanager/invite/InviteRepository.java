package com.buildingmanager.invite;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InviteRepository extends JpaRepository<Invite, Integer> {

    boolean existsByApartmentIdAndRoleAndStatus(Integer apartmentId, String role, InviteStatus status);

    Optional<Invite> findByToken(String token);

    List<Invite> findByApartment_Building_Id(Integer buildingId);

    boolean existsByEmailAndRoleAndStatus(String email, String role, InviteStatus status);

    long countByStatus(InviteStatus status);

    long count();

    @Query("""
    SELECT DATE(i.createdAt), COUNT(i)
    FROM Invite i
    WHERE i.createdAt >= :fromDate
    GROUP BY DATE(i.createdAt)
    ORDER BY DATE(i.createdAt)
""")
    List<Object[]> countInvitesGroupedByDate(@Param("fromDate") LocalDateTime fromDate);


}

