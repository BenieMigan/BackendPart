package gestion.pac.gestionstagiairesbackend.service;


import org.springframework.stereotype.Service;
import gestion.pac.gestionstagiairesbackend.entite.User;
import gestion.pac.gestionstagiairesbackend.repository.UserRepository;

@Service
public class NotificationService {

    private final EmailService emailService;
    private final UserRepository userRepository;

    public NotificationService(EmailService emailService, UserRepository userRepository) {
        this.emailService = emailService;
        this.userRepository = userRepository;
    }

    public void sendAffectationEncadreurNotification(Long encadreurId, Long stagiaireId) {
        User encadreur = userRepository.findById(encadreurId)
                .orElseThrow(() -> new RuntimeException("Encadreur non trouvé"));
        User stagiaire = userRepository.findById(stagiaireId)
                .orElseThrow(() -> new RuntimeException("Stagiaire non trouvé"));

        String subject = "Nouveau stagiaire assigné";
        String message = "Bonjour " + encadreur.getPrenom() + ",\n\n" +
                "Un nouveau stagiaire vous a été assigné pour encadrement:\n\n" +
                "Nom: " + stagiaire.getNom() + " " + stagiaire.getPrenom() + "\n" +
                "Email: " + stagiaire.getEmail() + "\n" +
                "Filière: " + stagiaire.getFiliere() + "\n" +
                "Période de stage: " + stagiaire.getDateDebut() + " - " + stagiaire.getDateFin() + "\n\n" +
                "Veuillez prendre contact avec le stagiaire dans les plus brefs délais.\n\n" +
                "Cordialement,\n" +
                "Le service de gestion des stagiaires";

        emailService.sendSimpleEmail(encadreur.getEmail(), subject, message);
    }

    public void sendAffectationEncadreurCancellationNotification(Long encadreurId, Long stagiaireId) {
        User encadreur = userRepository.findById(encadreurId)
                .orElseThrow(() -> new RuntimeException("Encadreur non trouvé"));
        User stagiaire = userRepository.findById(stagiaireId)
                .orElseThrow(() -> new RuntimeException("Stagiaire non trouvé"));

        String subject = "Annulation d'affectation de stagiaire";
        String message = "Bonjour " + encadreur.getPrenom() + ",\n\n" +
                "Nous vous informons que l'affectation du stagiaire suivant a été annulée:\n\n" +
                "Nom: " + stagiaire.getNom() + " " + stagiaire.getPrenom() + "\n" +
                "Email: " + stagiaire.getEmail() + "\n\n" +
                "Vous n'êtes plus responsable de l'encadrement de ce stagiaire.\n\n" +
                "Cordialement,\n" +
                "Le service de gestion des stagiaires";

        emailService.sendSimpleEmail(encadreur.getEmail(), subject, message);
    }

    public void sendAffectationNotification(Long secretaireId, Long stagiaireId) {
        User secretaire = userRepository.findById(secretaireId)
                .orElseThrow(() -> new RuntimeException("Secrétaire non trouvée"));
        User stagiaire = userRepository.findById(stagiaireId)
                .orElseThrow(() -> new RuntimeException("Stagiaire non trouvé"));

        String subject = "Nouveau stagiaire affecté";
        String message = "Bonjour " + secretaire.getPrenom() + ",\n\n" +
                "Un nouveau stagiaire vous a été affecté:\n\n" +
                "Nom: " + stagiaire.getNom() + " " + stagiaire.getPrenom() + "\n" +
                "Email: " + stagiaire.getEmail() + "\n" +
                "Filière: " + stagiaire.getFiliere() + "\n" +
                "Période de stage: " + stagiaire.getDateDebut() + " - " + stagiaire.getDateFin() + "\n\n" +
                "Cordialement,\n" +
                "Le service des Ressources Humaines";

        emailService.sendSimpleEmail(secretaire.getEmail(), subject, message);
    }

    public void sendAffectationCancellationNotification(Long secretaireId, Long stagiaireId) {
        User secretaire = userRepository.findById(secretaireId)
                .orElseThrow(() -> new RuntimeException("Secrétaire non trouvée"));
        User stagiaire = userRepository.findById(stagiaireId)
                .orElseThrow(() -> new RuntimeException("Stagiaire non trouvé"));

        String subject = "Annulation d'affectation de stagiaire";
        String message = "Bonjour " + secretaire.getPrenom() + ",\n\n" +
                "Nous vous informons que l'affectation du stagiaire suivant a été annulée:\n\n" +
                "Nom: " + stagiaire.getNom() + " " + stagiaire.getPrenom() + "\n" +
                "Email: " + stagiaire.getEmail() + "\n\n" +
                "Cette décision fait suite à une erreur dans le processus d'affectation initial.\n" +
                "Nous nous excusons pour la gêne occasionnée.\n\n" +
                "Cordialement,\n" +
                "Le service des Ressources Humaines";

        emailService.sendSimpleEmail(secretaire.getEmail(), subject, message);
    }

    public void sendReaffectationNotification(Long ancienneSecretaireId, Long nouvelleSecretaireId, Long stagiaireId) {
        User ancienneSecretaire = userRepository.findById(ancienneSecretaireId)
                .orElseThrow(() -> new RuntimeException("Ancienne secrétaire non trouvée"));
        User nouvelleSecretaire = userRepository.findById(nouvelleSecretaireId)
                .orElseThrow(() -> new RuntimeException("Nouvelle secrétaire non trouvée"));
        User stagiaire = userRepository.findById(stagiaireId)
                .orElseThrow(() -> new RuntimeException("Stagiaire non trouvé"));

        // Notification à l'ancienne secrétaire
        String subjectAncienne = "Modification d'affectation de stagiaire";
        String messageAncienne = "Bonjour " + ancienneSecretaire.getPrenom() + ",\n\n" +
                "Nous vous informons que le stagiaire suivant a été réaffecté:\n\n" +
                "Nom: " + stagiaire.getNom() + " " + stagiaire.getPrenom() + "\n" +
                "Email: " + stagiaire.getEmail() + "\n\n" +
                "Cette décision fait suite à une erreur dans le processus d'affectation initial.\n" +
                "Nous nous excusons pour la gêne occasionnée.\n\n" +
                "Cordialement,\n" +
                "Le service des Ressources Humaines";

        // Notification à la nouvelle secrétaire
        String subjectNouvelle = "Nouveau stagiaire affecté";
        String messageNouvelle = "Bonjour " + nouvelleSecretaire.getPrenom() + ",\n\n" +
                "Un nouveau stagiaire vous a été affecté:\n\n" +
                "Nom: " + stagiaire.getNom() + " " + stagiaire.getPrenom() + "\n" +
                "Email: " + stagiaire.getEmail() + "\n" +
                "Filière: " + stagiaire.getFiliere() + "\n" +
                "Période de stage: " + stagiaire.getDateDebut() + " - " + stagiaire.getDateFin() + "\n\n" +
                "Cordialement,\n" +
                "Le service des Ressources Humaines";

        emailService.sendSimpleEmail(ancienneSecretaire.getEmail(), subjectAncienne, messageAncienne);
        emailService.sendSimpleEmail(nouvelleSecretaire.getEmail(), subjectNouvelle, messageNouvelle);
    }

    public void sendAffectationChefServiceNotification(Long chefServiceId, Long stagiaireId) {
        User chefService = userRepository.findById(chefServiceId)
                .orElseThrow(() -> new RuntimeException("Chef de service non trouvé"));
        User stagiaire = userRepository.findById(stagiaireId)
                .orElseThrow(() -> new RuntimeException("Stagiaire non trouvé"));

        String subject = "Nouveau stagiaire affecté à votre service";
        String message = "Bonjour " + chefService.getPrenom() + ",\n\n" +
                "Un nouveau stagiaire a été affecté à votre service:\n\n" +
                "Nom: " + stagiaire.getNom() + " " + stagiaire.getPrenom() + "\n" +
                "Email: " + stagiaire.getEmail() + "\n" +
                "Filière: " + stagiaire.getFiliere() + "\n" +
                "Période de stage: " + stagiaire.getDateDebut() + " - " + stagiaire.getDateFin() + "\n\n" +
                "Cordialement,\n" +
                "Le service de secrétariat";

        emailService.sendSimpleEmail(chefService.getEmail(), subject, message);
    }

    public void sendAffectationChefServiceCancellationNotification(Long chefServiceId, Long stagiaireId) {
        User chefService = userRepository.findById(chefServiceId)
                .orElseThrow(() -> new RuntimeException("Chef de service non trouvé"));
        User stagiaire = userRepository.findById(stagiaireId)
                .orElseThrow(() -> new RuntimeException("Stagiaire non trouvé"));

        String subject = "Annulation d'affectation de stagiaire";
        String message = "Bonjour " + chefService.getPrenom() + ",\n\n" +
                "Nous vous informons que l'affectation du stagiaire suivant a été annulée:\n\n" +
                "Nom: " + stagiaire.getNom() + " " + stagiaire.getPrenom() + "\n" +
                "Email: " + stagiaire.getEmail() + "\n\n" +
                "Cette décision fait suite à une erreur dans le processus d'affectation initial.\n" +
                "Nous nous excusons pour la gêne occasionnée.\n\n" +
                "Cordialement,\n" +
                "Le service de secrétariat";

        emailService.sendSimpleEmail(chefService.getEmail(), subject, message);
    }


    public void sendReaffectationChefServiceNotification(Long ancienChefServiceId, Long nouveauChefServiceId, Long stagiaireId) {
        User ancienChefService = userRepository.findById(ancienChefServiceId)
                .orElseThrow(() -> new RuntimeException("Ancien chef de service non trouvé"));
        User nouveauChefService = userRepository.findById(nouveauChefServiceId)
                .orElseThrow(() -> new RuntimeException("Nouveau chef de service non trouvé"));
        User stagiaire = userRepository.findById(stagiaireId)
                .orElseThrow(() -> new RuntimeException("Stagiaire non trouvé"));

        // Notification à l'ancien chef de service
        String subjectAncien = "Modification d'affectation de stagiaire";
        String messageAncien = "Bonjour " + ancienChefService.getPrenom() + ",\n\n" +
                "Nous vous informons que le stagiaire suivant a été réaffecté:\n\n" +
                "Nom: " + stagiaire.getNom() + " " + stagiaire.getPrenom() + "\n" +
                "Email: " + stagiaire.getEmail() + "\n\n" +
                "Cette décision fait suite à une erreur dans le processus d'affectation initial.\n" +
                "Nous nous excusons pour la gêne occasionnée.\n\n" +
                "Cordialement,\n" +
                "Le service de secrétariat";

        // Notification au nouveau chef de service
        String subjectNouveau = "Nouveau stagiaire affecté à votre service";
        String messageNouveau = "Bonjour " + nouveauChefService.getPrenom() + ",\n\n" +
                "Un nouveau stagiaire a été affecté à votre service:\n\n" +
                "Nom: " + stagiaire.getNom() + " " + stagiaire.getPrenom() + "\n" +
                "Email: " + stagiaire.getEmail() + "\n" +
                "Filière: " + stagiaire.getFiliere() + "\n" +
                "Période de stage: " + stagiaire.getDateDebut() + " - " + stagiaire.getDateFin() + "\n\n" +
                "Cordialement,\n" +
                "Le service de secrétariat";

        emailService.sendSimpleEmail(ancienChefService.getEmail(), subjectAncien, messageAncien);
        emailService.sendSimpleEmail(nouveauChefService.getEmail(), subjectNouveau, messageNouveau);
    }
}