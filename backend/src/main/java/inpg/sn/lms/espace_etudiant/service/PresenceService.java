package inpg.sn.lms.espace_etudiant.service;

import java.util.List;

import org.springframework.stereotype.Service;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.espace_etudiant.entity.Presence;
import inpg.sn.lms.espace_etudiant.entity.Seance;
import inpg.sn.lms.espace_etudiant.repository.PresenceRepository;

@Service
public class PresenceService {

    private final PresenceRepository presenceRepository;

    public PresenceService(PresenceRepository presenceRepository) {
        this.presenceRepository = presenceRepository;
    }

    public List<Presence> getPresencesBySeanceId(Long seanceId) {
        return presenceRepository.findBySeanceId(seanceId);
    }

    public List<Presence> getPresencesByEtudiantId(Long etudiantId) {
        return presenceRepository.findByEtudiantIdOrderBySeance_DateHeureDesc(etudiantId);
    }

    /** Cree la presence si elle n'existe pas encore pour ce couple seance/etudiant, sinon la met a jour. */
    public Presence marquerPresence(Seance seance, Etudiant etudiant, boolean present) {
        Presence presence = presenceRepository.findBySeanceIdAndEtudiantId(seance.getId(), etudiant.getId())
                .orElseGet(Presence::new);
        presence.setSeance(seance);
        presence.setEtudiant(etudiant);
        presence.setPresent(present);
        return presenceRepository.save(presence);
    }
}
