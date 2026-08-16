package inpg.sn.lms.espace_etudiant.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import inpg.sn.lms.espace_etudiant.entity.Seance;
import inpg.sn.lms.espace_etudiant.repository.SeanceRepository;

@Service
public class SeanceService {
    private final SeanceRepository repository;
    public SeanceService(SeanceRepository repository) { this.repository = repository; }
    public List<Seance> getByFormateur(Long formateurId) { return repository.findByCours_Formateurs_IdOrderByDateHeureAsc(formateurId); }
    public List<Seance> getByCoursId(Long coursId) { return repository.findByCoursIdInOrderByDateHeureAsc(List.of(coursId)); }
    public Optional<Seance> findById(Long id) { return repository.findById(id); }
    public Seance save(Seance seance) { return repository.save(seance); }
    public void delete(Seance seance) { repository.delete(seance); }
}