package inpg.sn.lms.espace_etudiant.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import inpg.sn.lms.espace_etudiant.entity.Evenement;
import inpg.sn.lms.espace_etudiant.repository.EvenementRepository;

@Service
public class EvenementService {

    private final EvenementRepository evenementRepository;

    public EvenementService(EvenementRepository evenementRepository) {
        this.evenementRepository = evenementRepository;
    }

    public List<Evenement> getAll() {
        return evenementRepository.findAll();
    }

    public Optional<Evenement> findById(Long id) {
        return evenementRepository.findById(id);
    }

    public Evenement save(Evenement evenement) {
        return evenementRepository.save(evenement);
    }
}
