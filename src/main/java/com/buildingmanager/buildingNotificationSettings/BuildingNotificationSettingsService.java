package com.buildingmanager.buildingNotificationSettings;

import com.buildingmanager.building.Building;
import com.buildingmanager.building.BuildingRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BuildingNotificationSettingsService {

    private final BuildingRepository buildingRepository;
    private final BuildingNotificationSettingsRepository settingsRepository;

    public BuildingNotificationSettingsDTO getSettings(Integer buildingId) {
        BuildingNotificationSettings settings = settingsRepository.findByBuilding_Id(buildingId)
                .orElseGet(() -> createDefaultSettings(buildingId));

        return mapToDto(settings);
    }

    public BuildingNotificationSettingsDTO updateSettings(Integer buildingId, BuildingNotificationSettingsDTO dto) {
        BuildingNotificationSettings settings = settingsRepository.findByBuilding_Id(buildingId)
                .orElseGet(() -> createDefaultSettings(buildingId));

        settings.setEmailForStatementIssued(dto.getEmailForStatementIssued());
        settings.setEmailForNewPoll(dto.getEmailForNewPoll());
        settings.setEmailForNewAnnouncement(dto.getEmailForNewAnnouncement());

        settings.setAppForJoinRequest(dto.getAppForJoinRequest());
        settings.setAppForMemberLeave(dto.getAppForMemberLeave());
        settings.setAppForPaymentCompleted(dto.getAppForPaymentCompleted());
        settings.setAppForNewPoll(dto.getAppForNewPoll());
        settings.setAppForNewAnnouncement(dto.getAppForNewAnnouncement());

        settings.setManagerEmailForApartmentChanges(dto.getManagerEmailForApartmentChanges());
        settings.setManagerEmailForDirectMessage(dto.getManagerEmailForDirectMessage());
        settings.setManagerEmailForAddedToBuilding(dto.getManagerEmailForAddedToBuilding());

        settingsRepository.save(settings);
        return mapToDto(settings);
    }

    private BuildingNotificationSettings createDefaultSettings(Integer buildingId) {
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new EntityNotFoundException("Building not found"));

        BuildingNotificationSettings settings = BuildingNotificationSettings.builder()
                .building(building)
                .build();

        return settingsRepository.save(settings);
    }

    private BuildingNotificationSettingsDTO mapToDto(BuildingNotificationSettings s) {
        return BuildingNotificationSettingsDTO.builder()
                .buildingId(s.getBuilding().getId())
                .emailForStatementIssued(s.getEmailForStatementIssued())
                .emailForNewPoll(s.getEmailForNewPoll())
                .emailForNewAnnouncement(s.getEmailForNewAnnouncement())
                .appForJoinRequest(s.getAppForJoinRequest())
                .appForMemberLeave(s.getAppForMemberLeave())
                .appForPaymentCompleted(s.getAppForPaymentCompleted())
                .appForNewPoll(s.getAppForNewPoll())
                .appForNewAnnouncement(s.getAppForNewAnnouncement())
                .managerEmailForApartmentChanges(s.getManagerEmailForApartmentChanges())
                .managerEmailForDirectMessage(s.getManagerEmailForDirectMessage())
                .managerEmailForAddedToBuilding(s.getManagerEmailForAddedToBuilding())
                .build();
    }
}
