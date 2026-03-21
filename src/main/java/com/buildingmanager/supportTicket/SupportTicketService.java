package com.buildingmanager.supportTicket;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.apartment.ApartmentRepository;
import com.buildingmanager.building.Building;
import com.buildingmanager.building.BuildingRepository;
import com.buildingmanager.buildingMember.BuildingMember;
import com.buildingmanager.buildingMember.BuildingMemberRepository;
import com.buildingmanager.buildingMember.BuildingMemberStatus;
import com.buildingmanager.company.Company;
import com.buildingmanager.supportTicket.dto.SupportTicketRequest;
import com.buildingmanager.supportTicket.dto.SupportTicketResponse;
import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.Set;

@Service
public class SupportTicketService {

    private final SupportTicketRepository supportTicketRepository;
    private final BuildingRepository buildingRepository;
    private final ApartmentRepository apartmentRepository;
    private final BuildingMemberRepository buildingMemberRepository;
    private final UserRepository userRepository;
    private final TicketCommentRepository ticketCommentRepository;

    public SupportTicketService(
            SupportTicketRepository supportTicketRepository,
            BuildingRepository buildingRepository,
            ApartmentRepository apartmentRepository,
            BuildingMemberRepository buildingMemberRepository,
            UserRepository userRepository,
            TicketCommentRepository ticketCommentRepository
    ) {
        this.supportTicketRepository = supportTicketRepository;
        this.buildingRepository = buildingRepository;
        this.apartmentRepository = apartmentRepository;
        this.buildingMemberRepository = buildingMemberRepository;
        this.userRepository = userRepository;
        this.ticketCommentRepository = ticketCommentRepository;
    }

    private String generateTicketNumber() {
        Long sequenceValue = supportTicketRepository.getNextTicketSequenceValue();
        return "TCK-" + Year.now().getValue() + "-" + String.format("%06d", sequenceValue);
    }

    @Transactional
    public SupportTicketResponse createTicket(User currentUser, SupportTicketRequest request) {
        Building building = buildingRepository.findById(request.getBuildingId())
                .orElseThrow(() -> new EntityNotFoundException("Building not found"));

        BuildingMember member = buildingMemberRepository
                .findByUserIdAndBuildingIdAndStatus(
                        currentUser.getId(),
                        building.getId(),
                        BuildingMemberStatus.JOINED
                )
                .orElseThrow(() -> new IllegalStateException("User is not a joined member of this building"));

        validateTicketCreationRole(member);

        Apartment apartment = null;
        if (request.getApartmentId() != null) {
            apartment = apartmentRepository.findById(request.getApartmentId())
                    .orElseThrow(() -> new EntityNotFoundException("Apartment not found"));

            if (!apartment.getBuilding().getId().equals(building.getId())) {
                throw new IllegalStateException("Apartment does not belong to the selected building");
            }
        }

        Company company = building.getCompany();

        if (request.getTargetRole() == SupportTicketTargetRole.PROPERTY_MANAGER && company == null) {
            throw new IllegalStateException("Building is not linked to a Property Manager company");
        }

        validateTicketCreationRole(member);
        validateTargetRole(member, building, request.getTargetRole());

        SupportTicket ticket = SupportTicket.builder()
                .ticketNumber(generateTicketNumber())
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .category(request.getCategory())
                .priority(request.getPriority())
                .status(SupportTicketStatus.OPEN)
                .targetRole(request.getTargetRole())
                .createdBy(currentUser)
                .building(building)
                .apartment(apartment)
                .buildingMember(member)
                .company(company)
                .build();

        SupportTicket saved = supportTicketRepository.save(ticket);

        return mapToDto(saved);
    }

    private TicketCommentResponse mapCommentToDto(TicketComment comment) {
        TicketCommentResponse dto = new TicketCommentResponse();
        dto.setId(comment.getId());
        dto.setMessage(comment.getMessage());
        dto.setType(comment.getType());
        dto.setCreatedByUserId(comment.getCreatedBy().getId());
        dto.setCreatedByName(
                comment.getCreatedBy().getFirstName() + " " + comment.getCreatedBy().getLastName()
        );
        dto.setCreatedAt(comment.getCreatedAt());
        return dto;
    }

    @Transactional(readOnly = true)
    public List<SupportTicketResponse> getMyTickets(User currentUser) {
        return supportTicketRepository.findByCreatedByIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SupportTicketResponse> getCompanyTickets(User currentUser, Integer companyId) {
        boolean belongsToCompany = buildingMemberRepository
                .existsByUserIdAndBuildingCompanyIdAndStatus(
                        currentUser.getId(),
                        companyId,
                        BuildingMemberStatus.JOINED
                );

        if (!belongsToCompany) {
            throw new IllegalStateException("User is not allowed to view tickets for this company");
        }

        return supportTicketRepository.findByCompanyIdOrderByCreatedAtDesc(companyId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    private void validateTicketCreationRole(BuildingMember member) {
        if (member.getRole() == null || member.getRole().getName() == null) {
            throw new IllegalStateException("User has no role assigned for this building");
        }

        String roleName = member.getRole().getName().trim();
        String normalizedRole = roleName.toUpperCase().replace(" ", "_");

        Set<String> allowedRoles = Set.of(
                "RESIDENT",
                "OWNER",
                "BUILDINGMANAGER",
                "BUILDING_MANAGER",
                "PROPERTYMANAGER",
                "PROPERTY_MANAGER"
        );

        if (!allowedRoles.contains(normalizedRole)) {
            throw new IllegalStateException(
                    "User role '" + roleName + "' is not allowed to create support tickets"
            );
        }
    }

    private void validateTargetRole(BuildingMember member, Building building, SupportTicketTargetRole targetRole) {
        if (member.getRole() == null || member.getRole().getName() == null) {
            throw new IllegalStateException("User has no role assigned for this building");
        }

        String normalizedRole = member.getRole().getName().trim().toUpperCase().replace(" ", "_");

        boolean hasPropertyManager = building.getPropertyManager() != null;

        switch (normalizedRole) {
            case "PROPERTYMANAGER":
            case "PROPERTY_MANAGER":
                if (targetRole != SupportTicketTargetRole.ADMIN) {
                    throw new IllegalStateException("PropertyManager can create tickets only for Admin");
                }
                break;

            case "BUILDINGMANAGER":
            case "BUILDING_MANAGER":
                if (targetRole != SupportTicketTargetRole.ADMIN
                        && targetRole != SupportTicketTargetRole.PROPERTY_MANAGER) {
                    throw new IllegalStateException("BuildingManager can create tickets only for PropertyManager or Admin");
                }

                if (targetRole == SupportTicketTargetRole.PROPERTY_MANAGER && !hasPropertyManager) {
                    throw new IllegalStateException("This building has no PropertyManager");
                }
                break;

            case "OWNER":
            case "RESIDENT":
                if (targetRole != SupportTicketTargetRole.BUILDING_MANAGER
                        && targetRole != SupportTicketTargetRole.PROPERTY_MANAGER) {
                    throw new IllegalStateException("Owner/Resident can create tickets only for BuildingManager or PropertyManager");
                }

                if (targetRole == SupportTicketTargetRole.PROPERTY_MANAGER && !hasPropertyManager) {
                    throw new IllegalStateException("This building has no PropertyManager");
                }
                break;

            default:
                throw new IllegalStateException("User role is not allowed to create support tickets");
        }
    }

    private SupportTicketResponse mapToDto(SupportTicket ticket) {
        SupportTicketResponse dto = new SupportTicketResponse();
        dto.setId(ticket.getId());
        dto.setTicketNumber(ticket.getTicketNumber());
        dto.setTitle(ticket.getTitle());
        dto.setDescription(ticket.getDescription());
        dto.setStatus(ticket.getStatus());
        dto.setPriority(ticket.getPriority());
        dto.setCategory(ticket.getCategory());
        dto.setTargetRole(ticket.getTargetRole());
        dto.setBuildingId(ticket.getBuilding().getId());
        dto.setBuildingName(ticket.getBuilding().getName());
        dto.setApartmentId(ticket.getApartment() != null ? ticket.getApartment().getId() : null);
        dto.setApartmentLabel(ticket.getApartment() != null ? ticket.getApartment().getNumber() : null);
        dto.setCreatedByUserId(ticket.getCreatedBy().getId());
        dto.setCreatedByName(ticket.getCreatedBy().getFirstName() + " " + ticket.getCreatedBy().getLastName());
        dto.setAssignedAgentId(ticket.getAssignedAgent() != null ? ticket.getAssignedAgent().getId() : null);
        dto.setAssignedAgentName(
                ticket.getAssignedAgent() != null
                        ? ticket.getAssignedAgent().getFirstName() + " " + ticket.getAssignedAgent().getLastName()
                        : null
        );
        dto.setCreatedAt(ticket.getCreatedAt());
        dto.setUpdatedAt(ticket.getUpdatedAt());
        dto.setClosedAt(ticket.getClosedAt());
        return dto;
    }

    @Transactional(readOnly = true)
    public SupportTicketResponse getTicketById(User currentUser, Integer ticketId) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Support ticket not found"));

        validateTicketReadAccess(currentUser, ticket);

        return mapToDto(ticket);
    }

    private void validateTicketReadAccess(User currentUser, SupportTicket ticket) {
        if (ticket.getCreatedBy() != null && ticket.getCreatedBy().getId().equals(currentUser.getId())) {
            return;
        }

        if (ticket.getAssignedAgent() != null && ticket.getAssignedAgent().getId().equals(currentUser.getId())) {
            return;
        }

        String normalizedUserRole = normalizeRole(
                currentUser.getRole() != null ? currentUser.getRole().getName() : null
        );

        SupportTicketTargetRole readableTargetRole = mapUserRoleToTargetRole(normalizedUserRole);

        if (readableTargetRole != null && ticket.getTargetRole() == readableTargetRole) {
            return;
        }

        throw new IllegalStateException("User is not allowed to view this ticket");
    }
    private String normalizeRole(String roleName) {
        if (roleName == null) {
            return null;
        }
        return roleName.trim().toUpperCase().replace(" ", "_");
    }

    private SupportTicketTargetRole mapUserRoleToTargetRole(String normalizedRole) {
        if (normalizedRole == null) {
            return null;
        }

        return switch (normalizedRole) {
            case "PROPERTYMANAGER", "PROPERTY_MANAGER" -> SupportTicketTargetRole.PROPERTY_MANAGER;
            case "BUILDINGMANAGER", "BUILDING_MANAGER" -> SupportTicketTargetRole.BUILDING_MANAGER;
            case "ADMIN" -> SupportTicketTargetRole.ADMIN;
            default -> null;
        };
    }

    @Transactional
    public SupportTicketResponse updateStatus(User currentUser, Integer ticketId, SupportTicketStatus status) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Support ticket not found"));

        if (ticket.getCreatedBy() != null && ticket.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("The creator of the ticket is not allowed to change its status");
        }

        String normalizedRole = normalizeRole(
                currentUser.getRole() != null ? currentUser.getRole().getName() : null
        );

        boolean allowed = false;

        if ("BUILDINGMANAGER".equals(normalizedRole) || "BUILDING_MANAGER".equals(normalizedRole)) {
            allowed = ticket.getTargetRole() == SupportTicketTargetRole.BUILDING_MANAGER;
        } else if ("PROPERTYMANAGER".equals(normalizedRole) || "PROPERTY_MANAGER".equals(normalizedRole)) {
            allowed = ticket.getTargetRole() == SupportTicketTargetRole.PROPERTY_MANAGER;
        } else if ("ADMIN".equals(normalizedRole)) {
            allowed = ticket.getTargetRole() == SupportTicketTargetRole.ADMIN;
        } else if ("PROPERTYAGENT".equals(normalizedRole) || "PROPERTY_AGENT".equals(normalizedRole)) {
            allowed = ticket.getAssignedAgent() != null
                    && ticket.getAssignedAgent().getId().equals(currentUser.getId());
        }

        if (!allowed) {
            throw new IllegalStateException("User is not allowed to change the ticket status");
        }

        ticket.setStatus(status);

        if (status == SupportTicketStatus.CLOSED || status == SupportTicketStatus.RESOLVED) {
            if (ticket.getClosedAt() == null) {
                ticket.setClosedAt(java.time.LocalDateTime.now());
            }
        } else {
            ticket.setClosedAt(null);
        }

        SupportTicket saved = supportTicketRepository.save(ticket);
        return mapToDto(saved);
    }

    @Transactional
    public SupportTicketResponse assignAgent(User currentUser, Integer ticketId, Integer agentId) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Support ticket not found"));

        validateTicketReadAccess(currentUser, ticket);

        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new EntityNotFoundException("Agent user not found"));

        String agentRole = agent.getRole() != null ? agent.getRole().getName() : null;

        if (agentRole == null || !agentRole.equalsIgnoreCase("PropertyAgent")) {
            throw new IllegalStateException("Selected user is not a PropertyAgent");
        }

        ticket.setAssignedAgent(agent);

        SupportTicket saved = supportTicketRepository.save(ticket);
        return mapToDto(saved);
    }

    @Transactional
    public void deleteTicket(User currentUser, Integer ticketId) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Support ticket not found"));

        validateTicketReadAccess(currentUser, ticket);

        ticketCommentRepository.deleteByTicketId(ticketId);
        supportTicketRepository.delete(ticket);
    }

    @Transactional(readOnly = true)
    public List<TicketAgentResponse> getAvailableAgents(User currentUser) {
        return userRepository.findByRoleName("PropertyAgent")
                .stream()
                .map(user -> new TicketAgentResponse(
                        user.getId(),
                        user.getFirstName() + " " + user.getLastName()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SupportTicketResponse> getInboxTickets(User currentUser) {
        String normalizedRole = currentUser.getRole().getName().trim().toUpperCase().replace(" ", "_");

        if ("PROPERTYAGENT".equals(normalizedRole) || "PROPERTY_AGENT".equals(normalizedRole)) {
            return supportTicketRepository.findByAssignedAgentIdOrderByCreatedAtDesc(currentUser.getId())
                    .stream()
                    .map(this::mapToDto)
                    .toList();
        }

        SupportTicketTargetRole targetRole;

        switch (normalizedRole) {
            case "PROPERTYMANAGER":
            case "PROPERTY_MANAGER":
                targetRole = SupportTicketTargetRole.PROPERTY_MANAGER;
                break;
            case "BUILDINGMANAGER":
            case "BUILDING_MANAGER":
                targetRole = SupportTicketTargetRole.BUILDING_MANAGER;
                break;
            case "ADMIN":
                targetRole = SupportTicketTargetRole.ADMIN;
                break;
            default:
                throw new IllegalStateException("User role is not allowed to receive inbox tickets");
        }

        return supportTicketRepository.findByTargetRoleOrderByCreatedAtDesc(targetRole)
                .stream()
                .map(this::mapToDto)
                .toList();
    }
    @Transactional
    public TicketCommentResponse addComment(User currentUser, Integer ticketId, TicketCommentRequest request) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Support ticket not found"));

        validateTicketReadAccess(currentUser, ticket);

        TicketComment comment = new TicketComment();
        comment.setTicket(ticket);
        comment.setCreatedBy(currentUser);
        comment.setMessage(request.getMessage().trim());
        comment.setType(request.getType());

        TicketComment saved = ticketCommentRepository.save(comment);

        return mapCommentToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<TicketCommentResponse> getComments(User currentUser, Integer ticketId) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Support ticket not found"));

        validateTicketReadAccess(currentUser, ticket);

        return ticketCommentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream()
                .map(this::mapCommentToDto)
                .toList();
    }
    @Transactional(readOnly = true)
    public List<SupportTicketResponse> getListViewTickets(User currentUser) {
        String normalizedRole = normalizeRole(
                currentUser.getRole() != null ? currentUser.getRole().getName() : null
        );

        List<SupportTicket> result;

        switch (normalizedRole) {
            case "RESIDENT":
            case "OWNER":
                result = supportTicketRepository.findByCreatedByIdOrderByCreatedAtDesc(currentUser.getId());
                break;

            case "BUILDINGMANAGER":
            case "BUILDING_MANAGER":
            case "PROPERTYMANAGER":
            case "PROPERTY_MANAGER":
            case "ADMIN":
                result = supportTicketRepository.findVisibleTicketsForUser(currentUser.getId(), mapUserRoleToTargetRole(normalizedRole));
                break;

            case "PROPERTYAGENT":
            case "PROPERTY_AGENT":
                result = supportTicketRepository.findVisibleTicketsForAgent(currentUser.getId());
                break;

            default:
                throw new IllegalStateException("User role is not allowed to view ticket list");
        }

        return result.stream()
                .map(this::mapToDto)
                .toList();
    }


}