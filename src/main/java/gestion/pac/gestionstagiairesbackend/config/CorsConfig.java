package gestion.pac.gestionstagiairesbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")  // Autoriser toutes les routes
                .allowedOrigins("http://localhost:3000") // Autoriser seulement ton front React
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Autoriser ces méthodes
                .allowedHeaders("*") // Autoriser tous les headers
                .allowCredentials(true); // Autoriser les cookies (si besoin)
    }
}
