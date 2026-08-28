package com.buildingmanager.notificationPreference;

import com.buildingmanager.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    @Transactional
    public NotificationPreferenceDTO getPreferences(Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        NotificationPreference preference = preferenceRepository
                .findByUser_Id(user.getId())
                .orElseGet(() -> createDefault(user));
        return mapToDto(preference);
    }

    public NotificationPreferenceDTO getPreferencesForUser(Integer userId) {
        NotificationPreference preference = preferenceRepository
                .findByUser_Id(userId)
                .orElseGet(() -> NotificationPreference.builder()
                        .user(User.builder().id(userId).build())
                        .build());
        return mapToDto(preference);
    }

    @Transactional
    public NotificationPreferenceDTO updatePreferences(
            NotificationPreferenceDTO dto,
            Authentication connectedUser
    ) {
        User user = (User) connectedUser.getPrincipal();
        NotificationPreference preference = preferenceRepository
                .findByUser_Id(user.getId())
                .orElseGet(() -> createDefault(user));

        preference.setEmailForStatementIssued(booleanOrDefault(dto.getEmailForStatementIssued(), preference.getEmailForStatementIssued()));
        preference.setEmailForNewPoll(booleanOrDefault(dto.getEmailForNewPoll(), preference.getEmailForNewPoll()));
        preference.setEmailForNewAnnouncement(booleanOrDefault(dto.getEmailForNewAnnouncement(), preference.getEmailForNewAnnouncement()));
        preference.setEmailForAddedToBuilding(booleanOrDefault(dto.getEmailForAddedToBuilding(), preference.getEmailForAddedToBuilding()));

        preference.setAppForStatementIssued(booleanOrDefault(dto.getAppForStatementIssued(), preference.getAppForStatementIssued()));
        preference.setAppForNewPoll(booleanOrDefault(dto.getAppForNewPoll(), preference.getAppForNewPoll()));
        preference.setAppForNewAnnouncement(booleanOrDefault(dto.getAppForNewAnnouncement(), preference.getAppForNewAnnouncement()));
        preference.setAppForAddedToBuilding(booleanOrDefault(dto.getAppForAddedToBuilding(), preference.getAppForAddedToBuilding()));
        preference.setAppForJoinRequest(booleanOrDefault(dto.getAppForJoinRequest(), preference.getAppForJoinRequest()));
        preference.setAppForMemberLeave(booleanOrDefault(dto.getAppForMemberLeave(), preference.getAppForMemberLeave()));
        preference.setAppForPaymentCompleted(booleanOrDefault(dto.getAppForPaymentCompleted(), preference.getAppForPaymentCompleted()));

        preference.setSmsForStatementIssued(booleanOrDefault(dto.getSmsForStatementIssued(), preference.getSmsForStatementIssued()));
        preference.setSmsForNewPoll(booleanOrDefault(dto.getSmsForNewPoll(), preference.getSmsForNewPoll()));
        preference.setSmsForNewAnnouncement(booleanOrDefault(dto.getSmsForNewAnnouncement(), preference.getSmsForNewAnnouncement()));
        preference.setSmsForAddedToBuilding(booleanOrDefault(dto.getSmsForAddedToBuilding(), preference.getSmsForAddedToBuilding()));

        NotificationPreference saved = preferenceRepository.save(preference);
        return mapToDto(saved);
    }

    @Transactional
    protected NotificationPreference createDefault(User user) {
        NotificationPreference preference = NotificationPreference.builder()
                .user(user)
                .build();
        return preferenceRepository.save(preference);
    }

    private Boolean booleanOrDefault(Boolean value, Boolean current) {
        return value != null ? value : current;
    }

    private NotificationPreferenceDTO mapToDto(NotificationPreference p) {
        return NotificationPreferenceDTO.builder()
                .userId(p.getUser().getId())
                .emailForStatementIssued(p.getEmailForStatementIssued())
                .emailForNewPoll(p.getEmailForNewPoll())
                .emailForNewAnnouncement(p.getEmailForNewAnnouncement())
                .emailForAddedToBuilding(p.getEmailForAddedToBuilding())
                .appForStatementIssued(p.getAppForStatementIssued())
                .appForNewPoll(p.getAppForNewPoll())
                .appForNewAnnouncement(p.getAppForNewAnnouncement())
                .appForAddedToBuilding(p.getAppForAddedToBuilding())
                .appForJoinRequest(p.getAppForJoinRequest())
                .appForMemberLeave(p.getAppForMemberLeave())
                .appForPaymentCompleted(p.getAppForPaymentCompleted())
                .smsForStatementIssued(p.getSmsForStatementIssued())
                .smsForNewPoll(p.getSmsForNewPoll())
                .smsForNewAnnouncement(p.getSmsForNewAnnouncement())
                .smsForAddedToBuilding(p.getSmsForAddedToBuilding())
                .build();
    }
}
