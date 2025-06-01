package com.charleezy.maya.service.impl;

import com.charleezy.maya.config.GoogleCalendarConfig;
import com.charleezy.maya.service.GoogleCalendarService;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class GoogleCalendarServiceImpl implements GoogleCalendarService {
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private final GoogleCalendarConfig config;
    private Calendar service;

    private Calendar getService() throws GeneralSecurityException, IOException {
        if (service == null) {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            service = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                    .setApplicationName(config.getApplicationName())
                    .build();
        }
        return service;
    }

    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets
        InputStream in = GoogleCalendarServiceImpl.class.getResourceAsStream(config.getCredentialsPath());
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + config.getCredentialsPath());
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, Arrays.asList(config.getScopes()))
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(config.getTokensDirectoryPath())))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    @Override
    public Event createEvent(String summary, String description, String startTime, String endTime) {
        try {
            Event event = new Event()
                    .setSummary(summary)
                    .setDescription(description);

            DateTime start = new DateTime(startTime);
            event.setStart(new EventDateTime().setDateTime(start));

            DateTime end = new DateTime(endTime);
            event.setEnd(new EventDateTime().setDateTime(end));

            // Add default reminders
            EventReminder[] reminderOverrides = new EventReminder[] {
                new EventReminder().setMethod("popup").setMinutes(10)
            };
            event.setReminders(new Event.Reminders()
                    .setUseDefault(false)
                    .setOverrides(Arrays.asList(reminderOverrides)));

            return getService().events().insert("primary", event).execute();
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException("Failed to create calendar event", e);
        }
    }

    @Override
    public Event createReminder(String summary, String description, String reminderTime) {
        try {
            Event event = new Event()
                    .setSummary(summary)
                    .setDescription(description);

            DateTime start = new DateTime(reminderTime);
            event.setStart(new EventDateTime().setDateTime(start));

            // For reminders, set end time to same as start time
            event.setEnd(new EventDateTime().setDateTime(start));

            // Add immediate reminder
            EventReminder[] reminderOverrides = new EventReminder[] {
                new EventReminder().setMethod("popup").setMinutes(0)
            };
            event.setReminders(new Event.Reminders()
                    .setUseDefault(false)
                    .setOverrides(Arrays.asList(reminderOverrides)));

            return getService().events().insert("primary", event).execute();
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException("Failed to create reminder", e);
        }
    }

    @Override
    public void deleteEvent(String eventId) {
        try {
            getService().events().delete("primary", eventId).execute();
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException("Failed to delete calendar event", e);
        }
    }
} 