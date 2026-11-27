package net.bp4k.locationlens.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import net.bp4k.locationlens.enums.CalendarRole;

@Entity
@Table(
        name = "calendar_memberships",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_calendar_membership_user",
                columnNames = {"calendar_id", "user_id"}))
public class CalendarMembershipEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "calendar_id", nullable = false, updatable = false)
    private CalendarEntity calendar;

    @Column(name = "user_id", nullable = false, updatable = false, length = 255)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CalendarRole role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected CalendarMembershipEntity() {
    }

    public CalendarMembershipEntity(
            CalendarEntity calendar, String userId, CalendarRole role) {
        if (role == CalendarRole.OWNER) {
            throw new IllegalArgumentException(
                    "Calendar owners are represented by the calendar record");
        }
        this.calendar = calendar;
        this.userId = userId;
        this.role = role;
    }

    @PrePersist
    void setCreationTimestamp() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public String getId() {
        return id;
    }

    public CalendarEntity getCalendar() {
        return calendar;
    }

    public String getUserId() {
        return userId;
    }

    public CalendarRole getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
