package com.buildingmanager.poll;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PollOptionRepository extends JpaRepository<PollOption, Integer> {

    List<PollOption> findAllByPollOrderByPositionAsc(Poll poll);


}
