package inpg.sn.lms.recrutement.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.auth.repository.CandidatRepository;
import inpg.sn.lms.recrutement.dto.EntretienRequest;
import inpg.sn.lms.recrutement.dto.EntretienResponse;
import inpg.sn.lms.recrutement.entity.Entretien;
import inpg.sn.lms.recrutement.repository.EntretienRepository;

@Service
@Transactional
public class EntretienService {

    private final EntretienRepository entretienRepository;
    private final CandidatRepository candidatRepository;

    public EntretienService(EntretienRepository entretienRepository, CandidatRepository candidatRepository) {
        this.entretienRepository = entretienRepository;
        this.candidatRepository = candidatRepository;
    }

    public EntretienResponse creerEntretien(EntretienRequest request) {
        Candidat candidat = candidatRepository.findById(request.getCandidatId())
                .orElseThrow(() -> new RuntimeException("Candidat non trouvé avec l'ID: " + request.getCandidatId()));

        Entretien entretien = new Entretien();
        entretien.setDateHeure(request.getDateHeure());
        entretien.setLieuOuLien(request.getLieuOuLien());
        entretien.setNoteEntretien(request.getNoteEntretien());
        entretien.setAppreciationGlobale(request.getAppreciationGlobale());
        entretien.setCandidat(candidat);

        Entretien saved = entretienRepository.save(entretien);
        return toResponse(saved);
    }

    public EntretienResponse modifierEntretien(Long id, EntretienRequest request) {
        Entretien entretien = entretienRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entretien non trouvé avec l'ID: " + id));

        entretien.setDateHeure(request.getDateHeure());
        entretien.setLieuOuLien(request.getLieuOuLien());
        entretien.setNoteEntretien(request.getNoteEntretien());
        entretien.setAppreciationGlobale(request.getAppreciationGlobale());

        Entretien saved = entretienRepository.save(entretien);
        return toResponse(saved);
    }

    public List<EntretienResponse> getEntretiensByCandidatId(Long candidatId) {
        return entretienRepository.findByCandidatId(candidatId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<EntretienResponse> getAllEntretiens() {
        return entretienRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public void supprimerEntretien(Long id) {
        if (!entretienRepository.existsById(id)) {
            throw new RuntimeException("Entretien non trouvé avec l'ID: " + id);
        }
        entretienRepository.deleteById(id);
    }

    private EntretienResponse toResponse(Entretien e) {
        return EntretienResponse.builder()
                .id(e.getId())
                .dateHeure(e.getDateHeure())
                .lieuOuLien(e.getLieuOuLien())
                .noteEntretien(e.getNoteEntretien())
                .appreciationGlobale(e.getAppreciationGlobale())
                .candidatId(e.getCandidat().getId())
                .build();
    }
}