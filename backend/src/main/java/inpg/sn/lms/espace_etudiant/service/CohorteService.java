package inpg.sn.lms.espace_etudiant.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import inpg.sn.lms.espace_etudiant.entity.Cohorte;
import inpg.sn.lms.espace_etudiant.repository.CohorteRepository;

@Service
public class CohorteService {

    private final CohorteRepository cohorteRepository;

    public CohorteService(CohorteRepository cohorteRepository) {
        this.cohorteRepository = cohorteRepository;
    }

    public List<Cohorte> getAll() {
        return cohorteRepository.findAll();
    }

    public Optional<Cohorte> findById(Long id) {
        return cohorteRepository.findById(id);
    }
}
