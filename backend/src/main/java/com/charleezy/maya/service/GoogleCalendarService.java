package com.charleezy.maya.service;

import com.google.api.services.calendar.model.Event;

public interface GoogleCalendarService {
    /**
     * Create a calendar event
     * @param summary The event summary/title
     * @param description The event description
     * @param startTime The start time in ISO format
     * @param endTime The end time in ISO format (optional for reminders)
     * @return The created event
     */
    Event createEvent(String summary, String description, String startTime, String endTime);

    /**
     * Create a reminder (single-point-in-time event)
     * @param summary The reminder summary/title
     * @param description The reminder description
     * @param reminderTime The time for the reminder in ISO format
     * @return The created event
     */
    Event createReminder(String summary, String description, String reminderTime);

    /**
     * Delete a calendar event
     * @param eventId The ID of the event to delete
     */
    void deleteEvent(String eventId);
} 