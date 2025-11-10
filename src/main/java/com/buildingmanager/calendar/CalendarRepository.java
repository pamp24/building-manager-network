package com.buildingmanager.calendar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CalendarRepository extends JpaRepository<Calendar, Integer> {


    List<Calendar> findByBuildingIdAndActiveTrue(Integer buildingId);




}
