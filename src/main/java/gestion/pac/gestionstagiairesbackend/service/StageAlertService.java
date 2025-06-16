package gestion.pac.gestionstagiairesbackend.service;

import gestion.pac.gestionstagiairesbackend.entite.User;
import gestion.pac.gestionstagiairesbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class StageAlertService {
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${stage.alert.days:7}") // 7 jours avant par défaut
    private int alertDaysBefore;

    public StageAlertService(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Scheduled(cron = "0 0 9 * * ?") // Tous les jours à 9h
    public void checkStageEndDates() {
        LocalDate today = LocalDate.now();
        LocalDate alertDate = today.plusDays(alertDaysBefore);

        // Stagiaires actifs dont la date de fin approche
        List<User> endingSoon = userRepository.findByDateFinBetweenAndStageStatus(
                today, alertDate, User.StageStatus.ACTIVE);

        endingSoon.forEach(stagiaire -> {
            long daysLeft = ChronoUnit.DAYS.between(today, stagiaire.getDateFin());
            emailService.sendAlertEmail(
                    stagiaire.getEmail(),
                    "Fin de stage approchant",
                    "Votre stage se termine dans " + daysLeft + " jours"
            );

            // Notification RH
            emailService.sendAlertEmail(
                    "honfodavid29@gmail.com", // Email RH
                    "Fin de stage approchant - " + stagiaire.getNom(),
                    "Le stage de " + stagiaire.getNom() + " " + stagiaire.getPrenom() +
                            " se termine dans " + daysLeft + " jours"
            );
        });

        // Stagiaires dont le stage se termine aujourd'hui
        List<User> endingToday = userRepository.findByDateFinAndStageStatus(
                today, User.StageStatus.ACTIVE);

        endingToday.forEach(stagiaire -> {
            // Mise à jour du statut
            stagiaire.setStageStatus(User.StageStatus.COMPLETED);
            userRepository.save(stagiaire);

            emailService.sendAlertEmail(
                    stagiaire.getEmail(),
                    "Fin de stage",
                    "Votre stage est maintenant terminé. Merci pour votre travail!"
            );

            // Notification RH
            emailService.sendAlertEmail(
                    "honfodavid29@gmail.com",
                    "Stage terminé - " + stagiaire.getNom(),
                    "Le stage de " + stagiaire.getNom() + " " + stagiaire.getPrenom() +
                            " est maintenant terminé"
            );
        });
    }
}