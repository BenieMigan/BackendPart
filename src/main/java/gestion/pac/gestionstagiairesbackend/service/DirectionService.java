package gestion.pac.gestionstagiairesbackend.service;

import gestion.pac.gestionstagiairesbackend.entite.Direction;
import gestion.pac.gestionstagiairesbackend.repository.DirectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DirectionService {

    private final DirectionRepository directionRepository;

    @Autowired
    public DirectionService(DirectionRepository directionRepository) {
        this.directionRepository = directionRepository;
    }

    public Direction findById(Long id) {
        return directionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Direction non trouvée"));
    }
}