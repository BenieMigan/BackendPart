package gestion.pac.gestionstagiairesbackend.service;

import gestion.pac.gestionstagiairesbackend.entite.DemandeAbsence;
import gestion.pac.gestionstagiairesbackend.entite.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final JwtTokenService jwtTokenService;

    @Value("${spring.mail.username}") // Assurez-vous que cette propriété est bien dans application.properties
    private String fromEmail;

    public EmailService(JavaMailSender mailSender, JwtTokenService jwtTokenService) {
        this.mailSender = mailSender;
        this.jwtTokenService = jwtTokenService;
    }

    public void sendValidationEmail(String to, String subject) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(
                    "Bonjour,\n\n" +
                            "Nous avons le plaisir de vous informer que votre demande de stage a été acceptée.\n\n" +
                            "Pour finaliser votre inscription, veuillez vous connecter à la plateforme et accéder à la section 'Finalisation'.\n\n" +
                            "Cordialement,\n" +
                            "Le service des Ressources Humaines"
            );

            mailSender.send(message);
            logger.info("Email de validation envoyé à " + to);
        } catch (MailException e) {
            logger.error("Erreur lors de l'envoi de l'email de validation", e);
            throw new RuntimeException("Erreur lors de l'envoi de l'email");
        }
    }

    public void ForgotPassword(String to, String subject, String validationLink) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(
                    "Bonjour,\n\n" +
                            "Nous avons le plaisir de vous informer que votre demande de stage a été acceptée.\n\n" +
                            "Pour finaliser votre inscription, veuillez cliquer sur le lien suivant :\n" +
                            validationLink + "\n\n" +
                            "Ce lien est valable pendant 7 jours. Passé ce délai, votre demande sera annulée.\n\n" +
                            "Cordialement,\n" +
                            "Le service des Ressources Humaines"
            );

            mailSender.send(message);
            logger.info("Email de mot de passe oublier envoyé à " + to);
        } catch (MailException e) {
            logger.error("Erreur lors de l'envoi du mot de passe oublier", e);
            throw new RuntimeException("Erreur lors de l'envoi de l'email");
        }
    }

   

        public void sendAssuranceValidationEmail(String to, String subject) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(
                    "Bonjour,\n\n" +
                            "Votre fiche d'assurance a été validée par le service RH.\n\n" +
                            "Veuillez vous connecter à la plateforme pour télécharger les documents " +
                            "administratifs nécessaires pour commencer votre stage.\n\n" +
                            "Cordialement,\n" +
                            "Le service des Ressources Humaines"
            );

            mailSender.send(message);
            logger.info("Email de validation d'assurance envoyé à " + to);
        } catch (MailException e) {
            logger.error("Erreur lors de l'envoi de l'email de validation d'assurance", e);
            throw new RuntimeException("Erreur lors de l'envoi de l'email");
        }
    }

    public void sendRejetAssuranceEmail(String to, String subject) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(
                    "Bonjour,\n\n" +
                            "Votre demande de stage a été annulée car vous n'avez pas fourni une fiche d'assurance valide.\n\n" +
                            "Cordialement,\n" +
                            "Le service des Ressources Humaines"
            );

            mailSender.send(message);
            logger.info("Email de rejet d'assurance envoyé à " + to);
        } catch (MailException e) {
            logger.error("Erreur lors de l'envoi de l'email de rejet d'assurance", e);
            throw new RuntimeException("Erreur lors de l'envoi de l'email");
        }
    }

    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
            logger.info("Email envoyé à " + to + " avec le sujet: " + subject);
        } catch (MailException e) {
            logger.error("Erreur lors de l'envoi de l'email", e);
            throw new RuntimeException("Erreur lors de l'envoi de l'email");
        }
    }

    public void sendAssuranceValidationEmail(String to, String subject, LocalDate dateDebut, LocalDate dateFin) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText("Votre fiche d'assurance a été validée. Vous pouvez maintenant télécharger les documents pour commencer votre stage.\n\n"
                + "Votre stage commence :\n"
                + "Du " + dateDebut.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "\n"
                + "Au " + dateFin.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        mailSender.send(message);
    }



    public void sendOtpEmail(String to, String subject, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(
                    "Bonjour,\n\n" +
                            "Votre code OTP pour vous connecter en tant que RH est : " + otp + "\n\n" +
                            "Ce code est valable pendant 10 minutes.\n\n" +
                            "Cordialement,\n" +
                            "Le service des Ressources Humaines"
            );

            mailSender.send(message);
            logger.info("Email OTP envoyé à " + to);
        } catch (MailException e) {
            logger.error("Erreur lors de l'envoi de l'email OTP", e);
            throw new RuntimeException("Erreur lors de l'envoi de l'email OTP");
        }
    }

    public void sendOTPEmail(String to, String subject, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(
                    "Bonjour,\n\n" +
                            "Voici votre code de réinitialisation de mot de passe : " + otp + "\n\n" +
                            "Ce code est valable pendant 15 minutes.\n\n" +
                            "Cordialement,\n" +
                            "Le service des Ressources Humaines"
            );

            mailSender.send(message);
            logger.info("Email OTP envoyé à " + to);
        } catch (MailException e) {
            logger.error("Erreur lors de l'envoi de l'email OTP", e);
            throw new RuntimeException("Erreur lors de l'envoi de l'email OTP");
        }
    }

    public void sendAccountCreationEmail(String to, String subject, String email, String password) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(
                    "Bonjour,\n\n" +
                            "Votre compte a été créé avec les identifiants suivants :\n" +
                            "Email: " + email + "\n" +
                            "Mot de passe: " + password + "\n\n" +
                            "Vous pouvez vous connecter à l'adresse : http://votreplateforme.com/login\n\n" +
                            "Cordialement,\n" +
                            "Le service des Ressources Humaines"
            );

            mailSender.send(message);
            logger.info("Email de création de compte envoyé à " + to);
        } catch (MailException e) {
            logger.error("Erreur lors de l'envoi de l'email de création de compte", e);
            throw new RuntimeException("Erreur lors de l'envoi de l'email");
        }
    }


    public void sendAlertEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
            logger.info("Email d'alerte envoyé à " + to);
        } catch (MailException e) {
            logger.error("Erreur lors de l'envoi de l'email d'alerte", e);
            throw new RuntimeException("Erreur lors de l'envoi de l'email d'alerte");
        }
    }

    public void sendNotificationDemandeAbsence(User encadreur, User stagiaire, DemandeAbsence demande) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(encadreur.getEmail());
            message.setSubject("Nouvelle demande d'absence");
            message.setText("Bonjour " + encadreur.getPrenom() + ",\n\n" +
                    "Vous avez reçu une nouvelle demande d'absence de " +
                    stagiaire.getPrenom() + " " + stagiaire.getNom() + ".\n" +
                    "Période: du " + demande.getDateDebut() + " au " + demande.getDateFin() + "\n" +
                    "Motif: " + demande.getMotif() + "\n\n" +
                    "Connectez-vous à la plateforme pour traiter cette demande.\n\n" +
                    "Cordialement,\n" +
                    "L'équipe RH");

            mailSender.send(message);
            logger.info("Notification de demande d'absence envoyée à " + encadreur.getEmail());
        } catch (MailException e) {
            logger.error("Erreur lors de l'envoi de la notification de demande d'absence", e);
        }
    }

    public void sendNotificationReponseAbsence(User stagiaire, DemandeAbsence demande) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(stagiaire.getEmail());
            message.setSubject("Réponse à votre demande d'absence");
            message.setText("Bonjour " + stagiaire.getPrenom() + ",\n\n" +
                    "Votre demande d'absence du " + demande.getDateDebut() + " au " + demande.getDateFin() +
                    " a été " + demande.getStatut() + ".\n" +
                    "Commentaire de votre encadreur: " +
                    (demande.getCommentaire() != null ? demande.getCommentaire() : "Aucun commentaire") + "\n\n" +
                    "Connectez-vous à la plateforme pour plus de détails.\n\n" +
                    "Cordialement,\n" +
                    "L'équipe RH");

            mailSender.send(message);
            logger.info("Notification de réponse à la demande d'absence envoyée à " + stagiaire.getEmail());
        } catch (MailException e) {
            logger.error("Erreur lors de l'envoi de la notification de réponse à la demande d'absence", e);
        }
    }


}
