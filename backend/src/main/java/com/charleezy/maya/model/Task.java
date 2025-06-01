package com.charleezy.maya.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tasks")
@EqualsAndHashCode(callSuper = true)
public class Task extends CalendarItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Column(nullable = false)
    private LocalDateTime scheduledTime;

    @Column(nullable = false)
    private String userId;

    @Column
    private String calendarEventId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ItemStatus status = ItemStatus.PENDING;

    @Column
    private LocalDateTime completedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime dueDate;

    @Column
    private Integer estimatedMinutes;  // Estimated time to complete the task

    @Column
    private Boolean isRecurring = false;

    @Column
    private String recurrencePattern;  // For recurring tasks (daily, weekly, etc.)

    @Column
    private Integer priority = 0;  // Higher number = higher priority

    public Task() {
        setType(ItemType.TASK);
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 