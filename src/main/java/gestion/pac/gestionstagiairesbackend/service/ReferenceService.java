package gestion.pac.gestionstagiairesbackend.service;

import org.springframework.stereotype.Service;

@Service
public class ReferenceService {
    private int currentSequence = 10; // Lire depuis DB ou fichier

    public synchronized int getNextSequenceNumber() {
        currentSequence++;
        // Ici vous pourriez sauvegarder en DB/fichier
        return currentSequence;
    }
}