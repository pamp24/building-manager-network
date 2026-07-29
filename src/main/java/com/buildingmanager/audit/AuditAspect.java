package com.buildingmanager.audit;

import com.buildingmanager.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;

    @AfterReturning(value = "@annotation(auditable)", returning = "result")
    public void audit(JoinPoint joinPoint, Auditable auditable, Object result) {
        try {
            AuditLog auditLog = buildAuditLog(joinPoint, auditable, result);
            auditLogRepository.save(auditLog);
            log.debug("Audit log saved: {} {}#{}", auditLog.getAction(), auditLog.getEntityType(), auditLog.getEntityId());
        } catch (Exception e) {
            log.error("Failed to save audit log", e);
        }
    }

    private AuditLog buildAuditLog(JoinPoint joinPoint, Auditable auditable, Object result) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer actorId = null;
        String actorEmail = null;
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User user) {
            actorId = user.getId();
            actorEmail = user.getEmail();
        }

        String entityType = auditable.entityType();
        if (entityType.isEmpty()) {
            entityType = joinPoint.getTarget().getClass().getSimpleName();
        }

        String entityId = extractEntityId(result);

        String description = auditable.description();
        if (description.isEmpty()) {
            description = formatDescription(joinPoint, auditable.action(), entityType, entityId);
        }

        return AuditLog.builder()
                .actorId(actorId)
                .actorEmail(actorEmail)
                .action(auditable.action())
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .timestamp(LocalDateTime.now())
                .correlationId(UUID.randomUUID().toString().substring(0, 8))
                .build();
    }

    private String extractEntityId(Object result) {
        if (result == null) return null;
        if (result instanceof Integer id) return String.valueOf(id);
        if (result instanceof Long id) return String.valueOf(id);
        if (result instanceof String s) return s;
        try {
            var idMethod = result.getClass().getMethod("getId");
            Object id = idMethod.invoke(result);
            return id != null ? id.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String formatDescription(JoinPoint joinPoint, AuditAction action, String entityType, String entityId) {
        String target = entityType.replace("Service", "").replace("Controller", "");
        return switch (action) {
            case CREATE -> "Created " + target + (entityId != null ? " #" + entityId : "");
            case UPDATE -> "Updated " + target + (entityId != null ? " #" + entityId : "");
            case DELETE -> "Deleted " + target + (entityId != null ? " #" + entityId : "");
            case PERMISSION_CHANGE -> "Permission changed for " + target + (entityId != null ? " #" + entityId : "");
            case STATUS_CHANGE -> "Status changed for " + target + (entityId != null ? " #" + entityId : "");
            case PAYMENT -> "Payment processed for " + target + (entityId != null ? " #" + entityId : "");
            case INVITE -> "Invite sent for " + target + (entityId != null ? " #" + entityId : "");
            case ACTIVATE -> "Activated " + target + (entityId != null ? " #" + entityId : "");
        };
    }
}
