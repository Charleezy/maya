package com.charleezy.maya.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "calendar_items")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class CalendarItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Column(nullable = false)
    private String userId;

    @Column
    private String googleCalendarId;  // ID from Google Calendar

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ItemStatus status = ItemStatus.PENDING;

    @Column(nullable = false)
    private ItemType type;  // EVENT or TASK

    @Column
    private LocalDateTime completedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 