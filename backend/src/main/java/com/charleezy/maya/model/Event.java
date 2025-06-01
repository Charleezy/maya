package com.charleezy.maya.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "events")
@EqualsAndHashCode(callSuper = true)
public class Event extends CalendarItem {
    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column
    private String location;

    @Column
    private Boolean isAllDay = false;

    @Column
    private String meetingLink;  // For virtual meetings

    @Column
    @ElementCollection
    private java.util.Set<String> attendees = new java.util.HashSet<>();

    public Event() {
        setType(ItemType.EVENT);
    }
} 