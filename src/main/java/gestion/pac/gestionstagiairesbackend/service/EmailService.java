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

    public void sendValidationEmail(String to, String subject, String validationLink) {
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
            logger.info("Email de validation envoyé à " + to);
        } catch (MailException e) {
            logger.error("Erreur lors de l'envoi de l'email de validation", e);
            throw new RuntimeException("Erreur lors de l'envoi de l'email");
        }
    }
}
