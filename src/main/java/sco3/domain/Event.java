package sco3.domain;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * An append-only log event. The table lives in ClickHouse with a {@code MergeTree}
 * engine ordered by {@code (created_at, id)} — see {@code schema.sql}.
 *
 * <p>The id is a client-side generated UUID because ClickHouse has no sequences or
 * auto-increment columns.</p>
 */
@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "source", nullable = false, length = 64)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 16)
    private Severity severity;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Event() {
        // JPA
    }

    public Event(String source, Severity severity, String message, OffsetDateTime createdAt) {
        this.source = source;
        this.severity = severity;
        this.message = message;
        this.createdAt = createdAt;
    }

    @PrePersist
    void onPrePersist() {
        if (createdAt == null) {
            // truncated to micros because the column is DateTime64(6)
            createdAt = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
        }
    }

    public UUID getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
