package com.buildingmanager.propertyAgent;

import com.buildingmanager.building.Building;
import com.buildingmanager.building.BuildingRepository;
import com.buildingmanager.permission.BuildingPermissionLevel;
import com.buildingmanager.permission.UserBuildingPermission;
import com.buildingmanager.permission.UserBuildingPermissionRepository;
import com.buildingmanager.role.Role;
import com.buildingmanager.role.RoleRepository;
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
    private final RoleRepository roleRepository;
    private final UserBuildingPermissionRepository permissionRepository;

    @Override
    public List<PropertyAgentManagementResponse> getAgents() {
        User currentUser = propertyAgentSecurityHelper.getCurrentUser();
        String role = normalizeRole(currentUser);

        if ("ADMIN".equals(role)) {
            return userRepository.findByRole_Name("AdminAgent")
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        if ("PROPERTYMANAGER".equals(role) || "PROPERTY_MANAGER".equals(role)) {
            if (currentUser.getCompany() == null) {
                throw new RuntimeException("Ο τρέχων χρήστης δεν ανήκει σε εταιρία");
            }

            return userRepository.findByCompanyIdAndRole_Name(
                            currentUser.getCompany().getId(),
                            "PropertyAgent"
                    )
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        throw new RuntimeException("Δεν έχετε δικαίωμα προβολής agents");
    }

    private String normalizeRole(User user) {
        if (user.getRole() == null || user.getRole().getName() == null) {
            return "";
        }

        return user.getRole().getName().trim().toUpperCase().replace(" ", "_");
    }

    @Override
    public List<PropertyAgentManagementResponse> getMyCompanyAgents() {

        User currentUser = propertyAgentSecurityHelper.getCurrentUser();
        String role = normalizeRole(currentUser);

        List<User> agents;

        if ("ADMIN".equals(role)) {
            agents = userRepository.findByRole_Name("AdminAgent");
        } else if ("PROPERTYMANAGER".equals(role)) {
            if (currentUser.getCompany() == null) {
                throw new RuntimeException("Ο χρήστης δεν ανήκει σε εταιρία");
            }

            agents = userRepository.findByCompanyIdAndRole_Name(
                    currentUser.getCompany().getId(),
                    "PropertyAgent"
            );
        } else {
            throw new RuntimeException("Δεν έχεις πρόσβαση");
        }

        return agents.stream().map(this::mapToResponse).toList();
    }

    @Override
    public PropertyAgentManagementResponse updateAgentBuildings(Integer agentId, UpdatePropertyAgentBuildingsRequest request) {
        User currentUser = propertyAgentSecurityHelper.getCurrentUser();
        String currentRole = normalizeRole(currentUser);

        String expectedAgentRole = "ADMIN".equals(currentRole)
                ? "AdminAgent"
                : "PropertyAgent";

        User agent = userRepository.findByIdAndRole_Name(agentId, expectedAgentRole)
                .orElseThrow(() -> new RuntimeException(expectedAgentRole + " not found"));

        boolean isAdmin = "ADMIN".equals(currentRole);

        Integer companyId = null;

        if (!isAdmin) {
            companyId = getCurrentCompanyId(currentUser);
            validateSameCompany(agent, companyId);
        }

        agent.getAssignedBuildings().clear();
        permissionRepository.deleteByUserId(agent.getId());

        if (request.getBuildingIds() != null && !request.getBuildingIds().isEmpty()) {
            List<Building> buildings = buildingRepository.findByIdIn(request.getBuildingIds());

            List<Building> validBuildings;

            if (isAdmin) {
                validBuildings = buildings;
            } else {
                Integer finalCompanyId = companyId;
                validBuildings = buildings.stream()
                        .filter(building -> building.getCompany() != null && building.getCompany().getId().equals(finalCompanyId))
                        .toList();
            }

            agent.getAssignedBuildings().addAll(validBuildings);
            for (Building building : validBuildings) {
                permissionRepository.save(
                        UserBuildingPermission.builder()
                                .user(agent)
                                .building(building)
                                .permissionLevel(BuildingPermissionLevel.MANAGE)
                                .build()
                );
            }
        }

        User saved = userRepository.save(agent);
        return mapToResponse(saved);
    }

    @Override
    public void removeAgent(Integer agentId) {
        User currentUser = propertyAgentSecurityHelper.getCurrentUser();
        String currentRole = normalizeRole(currentUser);

        boolean isAdmin = "ADMIN".equals(currentRole);

        String expectedAgentRole = isAdmin
                ? "AdminAgent"
                : "PropertyAgent";

        User agent = userRepository.findByIdAndRole_Name(agentId, expectedAgentRole)
                .orElseThrow(() -> new RuntimeException(expectedAgentRole + " not found"));

        if (!isAdmin) {
            Integer companyId = getCurrentCompanyId(currentUser);
            validateSameCompany(agent, companyId);
        }

        agent.getAssignedBuildings().clear();
        permissionRepository.deleteByUserId(agent.getId());
        agent.setCompany(null);
        resetToUserRole(agent);

        userRepository.save(agent);
    }

    private void resetToUserRole(User user) {
        Role defaultRole = roleRepository.findByName("User")
                .orElseThrow(() -> new RuntimeException("Default role not found"));

        user.setRole(defaultRole);
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
