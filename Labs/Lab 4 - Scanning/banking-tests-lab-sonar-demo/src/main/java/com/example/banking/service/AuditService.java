package com.example.banking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Audit log writer.
 *
 * This class is the "clean reference" inside this project. It is here so
 * students can compare the deliberately-flawed classes against an example
 * that follows good practices: dependency-injected Clock for testability,
 * SLF4J logging instead of System.out, defensive copies, null checks,
 * Objects.requireNonNull guards.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final Clock clock;
    private final List<AuditEntry> entries = new ArrayList<>();

    public AuditService(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public AuditEntry record(String actor, String action, String target) {
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(action, "action must not be null");

        AuditEntry entry = new AuditEntry(actor, action, target, Instant.now(clock));
        entries.add(entry);
        log.info("Audit: actor={} action={} target={}", actor, action, target);
        return entry;
    }

    public List<AuditEntry> recent(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("limit must be non-negative");
        }
        int from = Math.max(0, entries.size() - limit);
        return Collections.unmodifiableList(new ArrayList<>(entries.subList(from, entries.size())));
    }

    public int size() {
        return entries.size();
    }

    public record AuditEntry(String actor, String action, String target, Instant at) {
        public AuditEntry {
            Objects.requireNonNull(actor);
            Objects.requireNonNull(action);
            Objects.requireNonNull(at);
        }
    }
}
