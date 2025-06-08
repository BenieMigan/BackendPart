package gestion.pac.gestionstagiairesbackend.service;

import gestion.pac.gestionstagiairesbackend.entite.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

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
}
