package com.buildingmanager.adminDashboard;

import com.buildingmanager.apartment.ApartmentRepository;
import com.buildingmanager.building.BuildingRepository;
import com.buildingmanager.calendar.CalendarRepository;
import com.buildingmanager.company.CompanyRepository;
import com.buildingmanager.invite.InviteRepository;
import com.buildingmanager.invite.InviteStatus;
import com.buildingmanager.poll.PollRepository;
import com.buildingmanager.poll.VoteRepository;
import com.buildingmanager.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserRepository userRepository;
    private final BuildingRepository buildingRepository;
    private final ApartmentRepository apartmentRepository;
    private final CompanyRepository companyRepository;
    private final InviteRepository inviteRepository;
    private final CalendarRepository calendarRepository;
    private final VoteRepository voteRepository;
    private final PollRepository pollRepository;

    @Override
    public AdminDashboardResponse getDashboard() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfWeek = LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        PlatformOverviewDto overview = new PlatformOverviewDto(
                buildingRepository.count(),
                userRepository.count(),
                companyRepository.count(),
                userRepository.countByRole_Name("BuildingManager") + userRepository.countByRole_Name("PropertyManager")
        );

        UserActivityDto userActivity = new UserActivityDto(
                userRepository.countByLastLoginDateAfter(startOfToday),
                userRepository.countByLastLoginDateAfter(startOfWeek),
                userRepository.countByCreatedDateAfter(startOfMonth)
        );

        BuildingActivityDto buildingActivity = new BuildingActivityDto(
                buildingRepository.countByCreatedDateAfter(startOfMonth),
                buildingRepository.count(),
                buildingRepository.countByManagerIsNull(),
                buildingRepository.countBuildingsWithoutApartments()
        );

        ApartmentUsageDto apartmentUsage = new ApartmentUsageDto(
                apartmentRepository.count(),
                apartmentRepository.countAssignedApartments(),
                apartmentRepository.countVacantApartments()
        );

        OperationalIssuesDto operationalIssues = new OperationalIssuesDto(
                buildingRepository.countByManagerIsNull(),
                apartmentRepository.countByOwnerIsNull(),
                inviteRepository.countByStatus(InviteStatus.PENDING)
        );

        long announcementsCount = calendarRepository.count();
        long votingsCount = pollRepository.count();

        long invitesSent = inviteRepository.count();
        long invitesAccepted = inviteRepository.countByStatus(InviteStatus.ACCEPTED);
        long pendingInvites = inviteRepository.countByStatus(InviteStatus.PENDING);

        double participationRate = 0.0;

        // Αν έχεις vote repo και eligible users logic:
        long totalVotes = voteRepository.count();
        long totalEligibleUsers = userRepository.count(); // προσωρινό fallback
        if (totalEligibleUsers > 0) {
            participationRate = Math.round(((double) totalVotes / totalEligibleUsers) * 10000.0) / 100.0;
        }

        EngagementStatsDto engagementStats = new EngagementStatsDto(
                announcementsCount,
                votingsCount,
                participationRate,
                invitesSent,
                invitesAccepted,
                pendingInvites
        );

        LocalDateTime fromDate = LocalDateTime.now().minusDays(30);

        List<GrowthPointDto> userGrowth = userRepository.countUsersGroupedByDate(fromDate)
                .stream()
                .map(r -> new GrowthPointDto(r[0].toString(), ((Number) r[1]).longValue()))
                .toList();

        List<GrowthPointDto> buildingGrowth = buildingRepository.countBuildingsGroupedByDate(fromDate)
                .stream()
                .map(r -> new GrowthPointDto(r[0].toString(), ((Number) r[1]).longValue()))
                .toList();

        List<GrowthPointDto> inviteGrowth = inviteRepository.countInvitesGroupedByDate(fromDate)
                .stream()
                .map(r -> new GrowthPointDto(r[0].toString(), ((Number) r[1]).longValue()))
                .toList();

        GrowthStatsDto growthStats = new GrowthStatsDto(
                userGrowth,
                buildingGrowth,
                inviteGrowth
        );

        return new AdminDashboardResponse(
                overview,
                userActivity,
                buildingActivity,
                apartmentUsage,
                operationalIssues,
                engagementStats,
                growthStats
        );
    }
}