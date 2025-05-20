package gestion.pac.gestionstagiairesbackend;

import gestion.pac.gestionstagiairesbackend.entite.User;
import gestion.pac.gestionstagiairesbackend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class GestionstagiairesbackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(GestionstagiairesbackendApplication.class, args);
	}

	// Initialiser l'utilisateur RH
	@Bean
	public CommandLineRunner initRH(UserRepository userRepository, PasswordEncoder encoder) {
		return args -> {
			if (userRepository.findByEmail("resourcehumaine@pac.bj").isEmpty()) {
				User rh = new User();
				rh.setEmail("resourcehumaine@pac.bj");
				rh.setPassword(encoder.encode("Admin123@azertyuiopppppmlkj@12"));
				rh.setNom("Responsable");
				rh.setPrenom("RH");
				rh.setRole("RH");
				userRepository.save(rh);
				System.out.println("✅ Utilisateur RH initialisé avec succès.");
			}
		};
	}
}
