package com.buildingmanager.user;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.apartment.ApartmentRepository;
import com.buildingmanager.email.EmailService;
import com.buildingmanager.role.Role;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ApartmentRepository apartmentRepository;
    private final EmailService emailService;

    public boolean updateUserRole(Integer userId, Role newRole) {
        return userRepository.findById(userId).map(user -> {
            user.setRole(newRole);
            userRepository.save(user);
            return true;
        }).orElse(false);
    }

    public Optional<User> findById(Integer id) {

        return userRepository.findById(id);
    }
    public Optional<User> findByEmail(String email) {

        return userRepository.findByEmail(email);
    }

    public List<UserTableDto> getUsersInSameBuilding(Integer userId) {
        Apartment apartment = apartmentRepository.findByResident_Id(userId)
                .or(() -> apartmentRepository.findByOwner_Id(userId))
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε διαμέρισμα για τον χρήστη."));

        Integer buildingId = apartment.getBuilding().getId();
        List<Apartment> apartments = apartmentRepository.findAllByBuilding_Id(buildingId);

        return apartments.stream()
                .flatMap(a -> Stream.of(a.getResident(), a.getOwner()))
                .filter(Objects::nonNull)
                .distinct()
                .map(u -> new UserTableDto(
                        u.getFullName(),
                        u.getEmail(),
                        u.getRole().getName(),
                        "Joined" // μπορείς να προσθέσεις λογική αν χρειάζεται να διαχωρίσεις Invited/Joined
                ))
                .toList();
    }

    public void inviteUserToBuilding(String email, Authentication auth) {
        User inviter = (User) auth.getPrincipal();

        Apartment inviterApartment = apartmentRepository.findByResident_Id(inviter.getId())
                .or(() -> apartmentRepository.findByOwner_Id(inviter.getId()))
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε διαμέρισμα για τον χρήστη"));

        Integer buildingId = inviterApartment.getBuilding().getId();

        String inviteLink = "http://localhost:4200/invite-accept?email=" + email + "&buildingId=" + buildingId;

        try {
            emailService.sendInviteEmail(email, inviter.getFullName(), inviteLink);
        } catch (MessagingException e) {
            throw new RuntimeException("Αποτυχία αποστολής email", e);
        }
    }



}