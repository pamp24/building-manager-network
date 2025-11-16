package com.buildingmanager.poll;


import com.buildingmanager.building.Building;
import com.buildingmanager.building.BuildingRepository;
import com.buildingmanager.buildingMember.BuildingMemberRepository;
import com.buildingmanager.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Transactional
public class PollService {

    private final PollRepository pollRepository;
    private final PollOptionRepository pollOptionRepository;
    private final PollMapper pollMapper;
    private final UserRepository userRepository;
    private final VoteRepository voteRepository;
    private final BuildingMemberRepository buildingMemberRepository;
    private final BuildingRepository buildingRepository;


    public List<PollDTO> getAllByBuilding(Integer buildingId, Integer userId) {

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isManager = user.getRole() != null &&
                "BuildingManager".equalsIgnoreCase(user.getRole().getName());

        if (!isManager) {
            throw new RuntimeException("Μόνο ο διαχειριστής μπορεί να βλέπει όλες τις ψηφοφορίες.");
        }

        List<Poll> polls = pollRepository.findByBuildingIdOrderByStartDateDesc(buildingId);

        return polls.stream()
                .map(pollMapper::toDTO)
                .toList();
    }


    /**
     * Επιστρέφει όλες τις ενεργές ψηφοφορίες ενός κτιρίου.
     * Αν κάποια έχει λήξει (endDate < τώρα), την κάνει αυτόματα inactive.
     */
    public List<PollDTO> getByBuildingForMember(Integer buildingId, Integer userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        //Αν είναι διαχειριστής, του επιτρέπουμε πάντα πρόσβαση
        boolean isManager = user.getRole() != null && "BuildingManager".equalsIgnoreCase(user.getRole().getName());

        boolean isMember = buildingMemberRepository
                .findByBuilding_IdAndUser_Id(buildingId, userId)
                .isPresent();

        if (!isMember && !isManager) {
            return List.of(); // ούτε μέλος ούτε διαχειριστής
        }

        // Αν η ψηφοφορία έχει λήξει, ενημέρωσε την αυτόματα
        List<Poll> polls = pollRepository.findByBuildingIdAndActiveTrueOrderByStartDateDesc(buildingId);
        LocalDateTime now = LocalDateTime.now();

        for (Poll poll : polls) {
            if (poll.getEndDate() != null && poll.getEndDate().isBefore(now)) {
                poll.setActive(false);
                pollRepository.save(poll);
            }
        }

        return polls.stream()
                .map(pollMapper::toDTO)
                .toList();
    }


    /**
     * Δημιουργία νέας ψηφοφορίας — μόνο ο διαχειριστής μπορεί.
     */
    public PollDTO create(PollDTO dto, Integer userId) {

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!"BuildingManager".equalsIgnoreCase(user.getRole().getName())) {
            throw new RuntimeException("Μόνο ο διαχειριστής μπορεί να δημιουργήσει ψηφοφορία.");
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
        return pollMapper.toDTO(saved);
    }



    /**
     * Ψήφος σε επιλογή ψηφοφορίας.
     * Ελέγχει αν ο χρήστης έχει ήδη ψηφίσει (αν όχι multiple choice).
     */
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

        if (user.getRole() == null || !"BuildingManager".equalsIgnoreCase(user.getRole().getName())) {
            throw new RuntimeException("Μόνο ο διαχειριστής μπορεί να απενεργοποιήσει ψηφοφορίες.");
        }

        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new RuntimeException("Poll not found"));

        poll.setActive(false);
        pollRepository.save(poll);
    }

    public List<VoteDTO> getVotesByPoll(Long pollId) {
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
}
