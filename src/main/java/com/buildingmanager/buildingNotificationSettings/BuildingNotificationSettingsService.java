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

        settings.setManagerAppForApartmentChanges(booleanOrDefault(dto.getManagerAppForApartmentChanges(), settings.getManagerAppForApartmentChanges()));
        settings.setManagerEmailForApartmentChanges(booleanOrDefault(dto.getManagerEmailForApartmentChanges(), settings.getManagerEmailForApartmentChanges()));
        settings.setManagerAppForMemberLeave(booleanOrDefault(dto.getManagerAppForMemberLeave(), settings.getManagerAppForMemberLeave()));
        settings.setManagerEmailForMemberLeave(booleanOrDefault(dto.getManagerEmailForMemberLeave(), settings.getManagerEmailForMemberLeave()));
        settings.setManagerAppForAddedToBuilding(booleanOrDefault(dto.getManagerAppForAddedToBuilding(), settings.getManagerAppForAddedToBuilding()));
        settings.setManagerEmailForAddedToBuilding(booleanOrDefault(dto.getManagerEmailForAddedToBuilding(), settings.getManagerEmailForAddedToBuilding()));

        settings.setMembersCanCreateAnnouncement(booleanOrDefault(dto.getMembersCanCreateAnnouncement(), settings.getMembersCanCreateAnnouncement()));
        settings.setMembersCanCreatePoll(booleanOrDefault(dto.getMembersCanCreatePoll(), settings.getMembersCanCreatePoll()));

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
                .managerAppForApartmentChanges(s.getManagerAppForApartmentChanges())
                .managerEmailForApartmentChanges(s.getManagerEmailForApartmentChanges())
                .managerAppForMemberLeave(s.getManagerAppForMemberLeave())
                .managerEmailForMemberLeave(s.getManagerEmailForMemberLeave())
                .managerAppForAddedToBuilding(s.getManagerAppForAddedToBuilding())
                .managerEmailForAddedToBuilding(s.getManagerEmailForAddedToBuilding())
                .membersCanCreateAnnouncement(s.getMembersCanCreateAnnouncement())
                .membersCanCreatePoll(s.getMembersCanCreatePoll())
                .build();
    }

    private Boolean booleanOrDefault(Boolean value, Boolean current) {
        return value != null ? value : current;
    }
}
