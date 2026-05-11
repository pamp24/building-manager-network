package com.buildingmanager.poll;


import com.buildingmanager.building.Building;
import com.buildingmanager.building.BuildingRepository;
import com.buildingmanager.buildingMember.BuildingMemberRepository;
import com.buildingmanager.notification.NotificationService;
import com.buildingmanager.permission.BuildingPermissionService;
import com.buildingmanager.permission.UserBuildingPermission;
import com.buildingmanager.permission.UserBuildingPermissionRepository;
import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class PollService {

    private final PollRepository pollRepository;
    private final PollOptionRepository pollOptionRepository;
    private final PollMapper pollMapper;
    private final UserRepository userRepository;
    private final VoteRepository voteRepository;
    private final BuildingRepository buildingRepository;
    private final BuildingPermissionService buildingPermissionService;
    private final UserBuildingPermissionRepository userBuildingPermissionRepository;
    private final NotificationService notificationService;
    private final BuildingMemberRepository buildingMemberRepository;

    public List<PollDTO> getAllByBuilding(Integer buildingId, Integer userId) {

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));


        List<Poll> polls = pollRepository.findByBuildingIdOrderByStartDateDesc(buildingId);

        if (!buildingPermissionService.canViewBuilding(user, buildingId)) {
            return List.of();
        }

        return polls.stream()
                .map(poll -> toDTOWithPermissions(poll, user))
                .toList();
    }

    public List<PollDTO> getByBuildingForMember(Integer buildingId, Integer userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!buildingPermissionService.canViewBuilding(user, buildingId)) {
            return List.of();
        }

        // Αν η ψηφοφορία έχει λήξει, ενημέρωσε την αυτόματα
        List<Poll> polls = pollRepository.findByBuildingIdAndActiveTrueOrderByStartDateDesc(buildingId);
        LocalDateTime now = LocalDateTime.now();

        for (Poll poll : polls) {
            if (poll.isActive()
                    && poll.getEndDate() != null
                    && poll.getEndDate().isBefore(now)) {

                poll.setActive(false);
                pollRepository.save(poll);

                if (!poll.isExpirationNotified()) {
                    notifyPollExpired(poll);
                }
            }
        }

        return polls.stream()
                .map(poll -> toDTOWithPermissions(poll, user))
                .toList();
    }

    public PollDTO create(PollDTO dto, Integer userId) {

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!buildingPermissionService.canManageBuilding(user, dto.getBuildingId())) {
            throw new AccessDeniedException("Δεν έχεις δικαίωμα δημιουργίας ψηφοφορίας σε αυτή την πολυκατοικία.");
        }

        Poll poll = pollMapper.toEntity(dto);

        Building building = buildingRepository.findById(dto.getBuildingId())
                .orElseThrow(() -> new RuntimeException("Building not found"));
        poll.setBuilding(building);

        poll.setCreatedByUser(user);
        poll.setActive(true);
        poll.setStartDate(dto.getStartDate() != null ? dto.getStartDate() : LocalDateTime.now());
        poll.setEndDate(dto.getEndDate());

        // Options
        List<PollOption> options = new ArrayList<>();
        int index = 1;
        for (PollOptionDTO optDto : dto.getOptions()) {
            PollOption option = new PollOption();
            option.setPoll(poll);
            option.setText(optDto.getText());
            option.setVotes(0);
            option.setNumber(index);
            option.setPosition(index);
            options.add(option);
            index++;
        }

        poll.setOptions(options);

        Poll saved = pollRepository.save(poll);

        notifyBuildingUsersForNewPoll(saved, user.getId());

        return pollMapper.toDTO(saved);
    }

    private void notifyBuildingUsersForNewPoll(Poll poll, Integer creatorUserId) {
        Integer buildingId = poll.getBuilding().getId();

        Set<User> receivers = new HashSet<>();

        userBuildingPermissionRepository.findByBuilding_Id(buildingId)
                .forEach(p -> {
                    if (p.getUser() != null) {
                        receivers.add(p.getUser());
                    }
                });

        buildingMemberRepository.findByBuilding_Id(buildingId)
                .forEach(m -> {
                    if (m.getUser() != null) {
                        receivers.add(m.getUser());
                    }
                });

        String message = "Δημιουργήθηκε νέα ψηφοφορία: " + poll.getTitle();

        String payload = """
            {
              "pollId": %d,
              "buildingId": %d
            }
            """.formatted(poll.getId(), buildingId);

        receivers.stream()
                .filter(receiver -> !receiver.getId().equals(creatorUserId))
                .forEach(receiver ->
                        notificationService.create(
                                receiver,
                                "POLL_CREATED",
                                message,
                                payload
                        )
                );
    }


    public void vote(Integer userId, Integer pollId, Integer optionId) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Η ψηφοφορία δεν βρέθηκε."));

        if (!poll.isActive() || (poll.getEndDate() != null && poll.getEndDate().isBefore(LocalDateTime.now()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Η ψηφοφορία έχει λήξει ή είναι ανενεργή.");
        }

        PollOption option = pollOptionRepository.findById(optionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Η επιλογή δεν βρέθηκε."));

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ο χρήστης δεν βρέθηκε."));

        if (!buildingPermissionService.canViewBuilding(user, poll.getBuilding().getId())) {
            throw new AccessDeniedException("Δεν έχεις δικαίωμα ψήφου σε αυτή την πολυκατοικία.");
        }

        // Όλες οι ψήφοι του χρήστη για αυτή την ψηφοφορία
        List<Vote> userVotes = voteRepository.findByPollAndUser(poll, user);

        if (!poll.isMultipleChoice()) {
            //Single choice: Αντικατάσταση ψήφου
            if (!userVotes.isEmpty()) {
                Vote existingVote = userVotes.get(0);

                // Αν ψήφισε ήδη την ίδια επιλογή → τίποτα
                if (existingVote.getOption().getId().equals(optionId)) {
                    return;
                }

                // Αφαίρεσε την παλιά ψήφο
                PollOption oldOption = existingVote.getOption();
                oldOption.setVotes(Math.max(0, oldOption.getVotes() - 1));
                pollOptionRepository.save(oldOption);

                voteRepository.delete(existingVote);
            }

            // Πρόσθεσε τη νέα ψήφο
            Vote newVote = new Vote(user, poll, option);
            voteRepository.save(newVote);
            option.setVotes(option.getVotes() + 1);
            pollOptionRepository.save(option);

        } else {
            //Multiple choice: toggle συμπεριφορά
            Optional<Vote> existing = userVotes.stream()
                    .filter(v -> v.getOption().getId().equals(optionId))
                    .findFirst();

            if (existing.isPresent()) {
                // Αν έχει ήδη ψηφίσει → αφαίρεση (unvote)
                voteRepository.delete(existing.get());
                option.setVotes(Math.max(0, option.getVotes() - 1));
            } else {
                // Αν δεν έχει ψηφίσει → νέα ψήφος
                Vote newVote = new Vote(user, poll, option);
                voteRepository.save(newVote);
                option.setVotes(option.getVotes() + 1);
            }

            pollOptionRepository.save(option);
        }
    }

    public PollDTO voteAndReturnPoll(Integer userId, Integer pollId, Integer optionId) {
        vote(userId, pollId, optionId);

        Poll updated = pollRepository.findByIdWithOptionsOrdered(pollId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Poll not found"));

        return pollMapper.toDTO(updated);
    }

    /**
     * Απενεργοποίηση (λήξη) ψηφοφορίας — μόνο από διαχειριστή.
     */
    public void deactivate(Integer pollId, Integer userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new RuntimeException("Poll not found"));

        if (!buildingPermissionService.canManageBuilding(user, poll.getBuilding().getId())) {
            throw new AccessDeniedException("Δεν έχεις δικαίωμα απενεργοποίησης ψηφοφορίας.");
        }

        poll.setActive(false);
        pollRepository.save(poll);
    }

    public List<VoteDTO> getVotesByPoll(Long pollId, Integer userId) {

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Poll poll = pollRepository.findById(pollId.intValue())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Poll not found"));

        if (!buildingPermissionService.canManageBuilding(user, poll.getBuilding().getId())) {
            throw new AccessDeniedException("Δεν έχεις δικαίωμα προβολής ψήφων.");
        }

        List<Vote> votes = voteRepository.findByPollId(pollId);

        return votes.stream().map(v -> {
            VoteDTO dto = new VoteDTO();
            dto.setUserId(v.getUser().getId());
            dto.setUserFullName(v.getUser().getFullName());
            dto.setOptionNumber(v.getOption().getNumber());
            dto.setOptionText(v.getOption().getText());
            dto.setVoteDate(v.getVoteDate());
            return dto;
        }).toList();
    }

    public List<PollDTO> getMyPolls(Integer userId) {

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<Integer> buildingIds = buildingPermissionService.getUserBuildingIds(user);

        if (buildingIds.isEmpty()) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();

        List<Poll> allPolls = new ArrayList<>();

        for (Integer buildingId : buildingIds) {

            boolean canManage = buildingPermissionService.canManageBuilding(user, buildingId);

            List<Poll> polls = canManage
                    ? pollRepository.findByBuildingIdOrderByStartDateDesc(buildingId)
                    : pollRepository.findByBuildingIdAndActiveTrueOrderByStartDateDesc(buildingId);

            for (Poll poll : polls) {
                if (poll.isActive() && poll.getEndDate() != null && poll.getEndDate().isBefore(now)) {
                    poll.setActive(false);
                    pollRepository.save(poll);
                }
            }

            if (!canManage) {
                polls = pollRepository.findByBuildingIdAndActiveTrueOrderByStartDateDesc(buildingId);
            }

            allPolls.addAll(polls);
        }

        return allPolls.stream()
                .map(poll -> toDTOWithPermissions(poll, user))
                .toList();
    }

    private PollDTO toDTOWithPermissions(Poll poll, com.buildingmanager.user.User user) {
        PollDTO dto = pollMapper.toDTO(poll);

        Integer buildingId = poll.getBuilding().getId();

        dto.setCanView(buildingPermissionService.canViewBuilding(user, buildingId));
        dto.setCanManage(buildingPermissionService.canManageBuilding(user, buildingId));

        return dto;
    }

    private void notifyPollExpired(Poll poll) {

        Integer buildingId = poll.getBuilding().getId();

        List<UserBuildingPermission> permissions =
                userBuildingPermissionRepository.findByBuilding_Id(buildingId);

        String message = "Η ψηφοφορία έληξε: " + poll.getTitle();

        String payload = """
        {
          "pollId": %d,
          "buildingId": %d
        }
        """.formatted(poll.getId(), buildingId);

        permissions.stream()
                .map(UserBuildingPermission::getUser)
                .filter(user -> user != null)
                .distinct()
                .forEach(user ->
                        notificationService.create(
                                user,
                                "POLL_EXPIRED",
                                message,
                                payload
                        )
                );

        poll.setExpirationNotified(true);
        pollRepository.save(poll);
    }

}
