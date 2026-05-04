package com.buildingmanager.propertyAgent;

import com.buildingmanager.building.Building;
import com.buildingmanager.building.BuildingRepository;
import com.buildingmanager.company.Company;
import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertyAgentManagementServiceImpl implements PropertyAgentManagementService {

    private final UserRepository userRepository;
    private final BuildingRepository buildingRepository;
    private final PropertyAgentSecurityHelper propertyAgentSecurityHelper;

    @Override
    public List<PropertyAgentManagementResponse> getMyCompanyAgents() {
        User currentUser = propertyAgentSecurityHelper.getCurrentUser();
        Company company = currentUser.getCompany();

        if (company == null) {
            throw new RuntimeException("Ο τρέχων χρήστης δεν ανήκει σε εταιρία");
        }

        List<User> agents = userRepository.findByCompanyIdAndRole_Name(company.getId(), "PropertyAgent");

        return agents.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public PropertyAgentManagementResponse updateAgentBuildings(Integer agentId, UpdatePropertyAgentBuildingsRequest request) {
        User currentUser = propertyAgentSecurityHelper.getCurrentUser();
        Integer companyId = getCurrentCompanyId(currentUser);

        User agent = userRepository.findByIdAndRole_Name(agentId, "PropertyAgent")
                .orElseThrow(() -> new RuntimeException("Property Agent not found"));

        validateSameCompany(agent, companyId);

        agent.getAssignedBuildings().clear();

        if (request.getBuildingIds() != null && !request.getBuildingIds().isEmpty()) {
            List<Building> buildings = buildingRepository.findByIdIn(request.getBuildingIds());

            List<Building> validBuildings = buildings.stream()
                    .filter(building -> building.getCompany() != null && building.getCompany().getId().equals(companyId))
                    .toList();

            agent.getAssignedBuildings().addAll(validBuildings);
        }

        User saved = userRepository.save(agent);
        return mapToResponse(saved);
    }

    @Override
    public void removeAgent(Integer agentId) {
        User currentUser = propertyAgentSecurityHelper.getCurrentUser();
        Integer companyId = getCurrentCompanyId(currentUser);

        User agent = userRepository.findByIdAndRole_Name(agentId, "PropertyAgent")
                .orElseThrow(() -> new RuntimeException("Property Agent not found"));

        validateSameCompany(agent, companyId);

        agent.getAssignedBuildings().clear();
        agent.setCompany(null);

        // Αν θέλεις να επιστρέφει σε βασικό ρόλο:
        // Role defaultRole = ...
        // agent.setRole(defaultRole);

        userRepository.save(agent);
    }

    private Integer getCurrentCompanyId(User currentUser) {
        if (currentUser.getCompany() == null) {
            throw new RuntimeException("Ο τρέχων χρήστης δεν ανήκει σε εταιρία");
        }
        return currentUser.getCompany().getId();
    }

    private void validateSameCompany(User agent, Integer companyId) {
        if (agent.getCompany() == null || !agent.getCompany().getId().equals(companyId)) {
            throw new RuntimeException("Δεν μπορείτε να διαχειριστείτε αυτόν τον agent");
        }
    }

    private PropertyAgentManagementResponse mapToResponse(User agent) {
        List<AgentBuildingResponse> assignedBuildings = new ArrayList<>();

        if (agent.getAssignedBuildings() != null) {
            assignedBuildings = agent.getAssignedBuildings().stream()
                    .map(building -> new AgentBuildingResponse(
                            building.getId(),
                            getBuildingDisplayName(building)
                    ))
                    .toList();
        }

        return new PropertyAgentManagementResponse(
                agent.getId(),
                agent.getFullName(),
                agent.getEmail(),
                assignedBuildings
        );
    }

    private String getBuildingDisplayName(Building building) {
        // Προσαρμόζεις ανάλογα με τα fields που έχει το Building σου
        // Αν έχει buildingName, βάλε το
        if (building.getName() != null && !building.getName().isBlank()) {
            return building.getName();
        }

        // fallback
        return "Building #" + building.getId();
    }
}
