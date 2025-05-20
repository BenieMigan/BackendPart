package gestion.pac.gestionstagiairesbackend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.List;
import java.util.Arrays;

@Service
public class FileStorageService {

    private final Path rootLocation;
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("pdf", "jpg", "jpeg", "png");

    // Injection du chemin depuis application.properties
    public FileStorageService(@Value("${file.upload-dir}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de créer le dossier de stockage", e);
        }
    }

    public String storeFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide");
        }

        String fileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

        // Vérification de l'extension
        if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
            throw new IllegalArgumentException("Seuls les fichiers PDF, JPG, JPEG et PNG sont autorisés");
        }

        if (fileName.contains("..")) {
            throw new SecurityException("Le nom du fichier contient une séquence invalide");
        }

        String uniqueFileName = System.currentTimeMillis() + "_" + fileName;
        Path targetLocation = rootLocation.resolve(uniqueFileName);

        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        return uniqueFileName;
    }
    public Path loadFileAsPath(String fileName) {
        return rootLocation.resolve(fileName).normalize();
    }

}