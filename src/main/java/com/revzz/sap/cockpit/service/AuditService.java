package com.revzz.sap.cockpit.service;

import com.revzz.sap.cockpit.domain.AuditEntry;
import com.revzz.sap.cockpit.repo.PlatformRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * The audit trail.
 *
 * <p>Every action an operator takes - acknowledging an alert, queueing a
 * replay - is written here. In an integration operation the question
 * "who released this message at 03:00" has to have an answer.</p>
 */
@Service
public class AuditService {

    private final PlatformRepository platform;

    public AuditService(PlatformRepository platform) {
        this.platform = platform;
    }

    public void record(String username, String action, String detail) {
        AuditEntry entry = new AuditEntry();
        entry.setAuditId("AU" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        entry.setUsername(username);
        entry.setAction(action);
        entry.setDetail(detail);
        entry.setAt(LocalDateTime.now());
        platform.insertAudit(entry);
    }

    public List<AuditEntry> recent(int limit) {
        return platform.findRecentAudit(limit);
    }
}
