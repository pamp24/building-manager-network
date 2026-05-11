package com.buildingmanager.poll;

import com.buildingmanager.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Integer> {

    boolean existsByPollIdAndUserId(Integer pollId, Integer userId);

    boolean existsByOptionIdAndUserId(Integer optionId, Integer userId);

    List<Vote> findByPollAndUser(Poll poll, User user);

    List<Vote> findByPollId(Long pollId);



}
