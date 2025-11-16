package com.buildingmanager.poll;

import com.buildingmanager.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/polls")
@RequiredArgsConstructor
public class PollController {

    private final PollService pollService;

    @GetMapping("/building/{buildingId}/all")
    public List<PollDTO> getAll(@PathVariable Integer buildingId, Authentication authentication) {
        var user = (User) authentication.getPrincipal();
        return pollService.getAllByBuilding(buildingId, user.getId());
    }

    @GetMapping("/building/{buildingId}")
    public List<PollDTO> getByBuilding(@PathVariable Integer buildingId, Authentication authentication) {
        var user = (User) authentication.getPrincipal();
        return pollService.getByBuildingForMember(buildingId, user.getId());
    }

    @PostMapping
    public PollDTO create(@RequestBody PollDTO dto, Authentication authentication) {
        var user = (User) authentication.getPrincipal();
        return pollService.create(dto, user.getId());
    }

    @PostMapping("/{pollId}/vote/{optionId}")
    public ResponseEntity<PollDTO> vote(
            @PathVariable Integer pollId,
            @PathVariable Integer optionId,
            Authentication authentication) {

        var user = (User) authentication.getPrincipal();
        PollDTO updatedPoll = pollService.voteAndReturnPoll(user.getId(), pollId, optionId);
        return ResponseEntity.ok(updatedPoll);
    }

    @DeleteMapping("/{id}")
    public void deactivate(@PathVariable Integer id, Authentication authentication) {
        var user = (User) authentication.getPrincipal();
        pollService.deactivate(id, user.getId());
    }

    @GetMapping("/{id}/votes")
    public ResponseEntity<List<VoteDTO>> getVotes(@PathVariable Long id) {
        return ResponseEntity.ok(pollService.getVotesByPoll(id));
    }
}
