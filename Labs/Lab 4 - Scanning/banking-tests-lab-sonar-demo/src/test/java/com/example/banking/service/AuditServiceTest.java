package com.example.banking.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuditServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-01-15T10:30:00Z");
    private final Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneId.of("UTC"));

    @Test
    void recordsAnEntryWithFixedTimestamp() {
        AuditService service = new AuditService(fixedClock);

        AuditService.AuditEntry entry = service.record("alice", "TRANSFER", "A001");

        assertNotNull(entry);
        assertEquals("alice", entry.actor());
        assertEquals("TRANSFER", entry.action());
        assertEquals("A001", entry.target());
        assertEquals(FIXED_NOW, entry.at());
        assertEquals(1, service.size());
    }

    @Test
    void recentReturnsTheLastNEntries() {
        AuditService service = new AuditService(fixedClock);

        for (int i = 0; i < 5; i++) {
            service.record("user" + i, "ACTION", "target" + i);
        }

        List<AuditService.AuditEntry> recent = service.recent(3);

        assertEquals(3, recent.size());
        assertEquals("user2", recent.get(0).actor());
        assertEquals("user3", recent.get(1).actor());
        assertEquals("user4", recent.get(2).actor());
    }

    @Test
    void recentRejectsNegativeLimit() {
        AuditService service = new AuditService(fixedClock);

        assertThrows(IllegalArgumentException.class, () -> service.recent(-1));
    }

    @Test
    void recordRejectsNullActor() {
        AuditService service = new AuditService(fixedClock);

        assertThrows(NullPointerException.class,
                () -> service.record(null, "ACTION", "target"));
    }
}
