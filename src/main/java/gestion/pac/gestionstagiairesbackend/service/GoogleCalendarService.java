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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;

@Service
public class GoogleCalendarService {

    @Value("${google.calendar.credentials.path}")
    private String credentialsPath;

    @Value("${google.calendar.id}")
    private String calendarId;

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    public void addDemandeStageEvent(User stagiaire) throws IOException, GeneralSecurityException {
        // Ne créer l'événement que si le dossier est finalisé
        if (!"DOCUMENT_COMPLET".equals(stagiaire.getStatut())) {
            return;
        }

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

            String eventTitle = String.format("Stage de %s %s (%s)",
                    stagiaire.getPrenom(),
                    stagiaire.getNom(),
                    stagiaire.getTypeStage());

            Event event = new Event()
                    .setSummary(eventTitle)
                    .setDescription(buildEventDescription(stagiaire));

            // Convertir LocalDate en Date pour Google Calendar
            Date startDate = Date.from(stagiaire.getDateDebut()
                    .atStartOfDay(ZoneId.of("Africa/Casablanca")).toInstant());
            Date endDate = Date.from(stagiaire.getDateFin()
                    .plusDays(1) // Ajouter un jour pour inclure le dernier jour
                    .atStartOfDay(ZoneId.of("Africa/Casablanca")).toInstant());

            EventDateTime start = new EventDateTime()
                    .setDate(new com.google.api.client.util.DateTime(startDate))
                    .setTimeZone("Africa/Casablanca");
            event.setStart(start);

            EventDateTime end = new EventDateTime()
                    .setDate(new com.google.api.client.util.DateTime(endDate))
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
                "Stagiaire: %s %s\n" +
                        "Type: %s\n" +
                        "Filière: %s\n" +
                        "Établissement: %s\n" +
                        "Email: %s\n" +
                        "Téléphone: %s\n" +
                        "Période: %s au %s",
                stagiaire.getPrenom(),
                stagiaire.getNom(),
                stagiaire.getTypeStage(),
                stagiaire.getFiliere(),
                stagiaire.getNomEtablissement(),
                stagiaire.getEmail(),
                stagiaire.getTelephone(),
                stagiaire.getDateDebut(),
                stagiaire.getDateFin()
        );
    }

    private Event setEventColor(Event event, String statut) {
        switch (statut) {
            case "DOCUMENT_COMPLET":
                return event.setColorId("2"); // Vert
            case "VALIDEE":
                return event.setColorId("5"); // Jaune
            case "REJETEE":
                return event.setColorId("4"); // Rouge
            default:
                return event.setColorId("11"); // Gris
        }
    }
}