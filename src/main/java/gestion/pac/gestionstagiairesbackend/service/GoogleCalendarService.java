package gestion.pac.gestionstagiairesbackend.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import gestion.pac.gestionstagiairesbackend.entite.User;

import com.google.api.client.json.JsonFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Service
public class GoogleCalendarService {

    @Value("${google.calendar.credentials.path}")
    private String credentialsPath;

    @Value("${google.calendar.id}")
    private String calendarId;

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    public void addDemandeStageEvent(User stagiaire) throws IOException, GeneralSecurityException {
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        try (InputStream in = getCredentialsStream()) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(in)
                    .createScoped(Collections.singleton(CalendarScopes.CALENDAR));

            Calendar service = new Calendar.Builder(
                    httpTransport,
                    JSON_FACTORY,
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName("Gestion Stagiaires")
                    .build();

            String eventTitle = String.format("[%s] Demande de %s %s",
                    stagiaire.getStatut(),
                    stagiaire.getPrenom(),
                    stagiaire.getNom());

            Event event = new Event()
                    .setSummary(eventTitle)
                    .setDescription(buildEventDescription(stagiaire));

            EventDateTime start = new EventDateTime()
                    .setDateTime(new com.google.api.client.util.DateTime(stagiaire.getDateSoumission().toString()))
                    .setTimeZone("Africa/Casablanca");
            event.setStart(start);

            EventDateTime end = new EventDateTime()
                    .setDateTime(new com.google.api.client.util.DateTime(
                            stagiaire.getDateSoumission().plusHours(1).toString()))
                    .setTimeZone("Africa/Casablanca");
            event.setEnd(end);

            event = setEventColor(event, stagiaire.getStatut());

            service.events().insert(calendarId, event).execute();
        }
    }

    private InputStream getCredentialsStream() throws IOException {
        if (credentialsPath.startsWith("classpath:")) {
            String resourcePath = credentialsPath.substring("classpath:".length());
            return getClass().getClassLoader().getResourceAsStream(resourcePath);
        }
        return new FileInputStream(credentialsPath);
    }

    private String buildEventDescription(User stagiaire) {
        return String.format(
                "Type: %s\nFilière: %s\nÉtablissement: %s\nEmail: %s\nStatut: %s",
                stagiaire.getTypeStage(),
                stagiaire.getFiliere(),
                stagiaire.getNomEtablissement(),
                stagiaire.getEmail(),
                stagiaire.getStatut()
        );
    }

    private Event setEventColor(Event event, String statut) {
        switch (statut) {
            case "VALIDEE":
                return event.setColorId("2"); // Vert
            case "REJETEE":
                return event.setColorId("4"); // Rouge
            default:
                return event.setColorId("5"); // Jaune
        }
    }
}