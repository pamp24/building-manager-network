package com.buildingmanager.poll;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PollRepository extends JpaRepository<Poll, Integer> {

    //όλα
    List<Poll> findByBuildingIdOrderByStartDateDesc(Integer buildingId);

    // ενεργά
    List<Poll> findByBuildingIdAndActiveTrueOrderByStartDateDesc(Integer buildingId);

    // Νέα μέθοδος για ταξινομημένες επιλογές συγκεκριμένου poll
    @Query("SELECT DISTINCT p FROM Poll p " +
            "LEFT JOIN FETCH p.options o " +
            "WHERE p.id = :pollId " +
            "ORDER BY o.position ASC")

    Optional<Poll> findByIdWithOptionsOrdered(@Param("pollId") Integer pollId);
}

