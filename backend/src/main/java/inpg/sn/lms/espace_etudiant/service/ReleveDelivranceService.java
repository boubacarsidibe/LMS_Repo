package inpg.sn.lms.espace_etudiant.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.espace_etudiant.dto.ReleveNotesDto;
import inpg.sn.lms.espace_etudiant.entity.ReleveLigne;
import inpg.sn.lms.espace_etudiant.entity.ReleveNotes;
import inpg.sn.lms.espace_etudiant.repository.ReleveNotesRepository;

/**
 * Délivrance d'un relevé de notes.
 *
 * <p>Le geste consiste à <strong>figer</strong> l'état courant : les moyennes sont
 * recopiées, pas référencées. C'est ce qui distingue un document d'un écran — un
 * relevé remis en janvier ne doit pas changer parce qu'un formateur corrige une note
 * en février.
 *
 * <p>Le document est délivré au nom de l'établissement, jamais de la personne
 * connectée : c'est l'INPG qu'il engage. Même règle que pour le certificat, où le nom
 * du formateur avait dû être retiré.
 */
@Service
public class ReleveDelivranceService {

    /** Le relevé engage l'établissement, pas l'agent qui l'édite. */
    public static final String AUTORITE = "INPG — Direction pédagogique";

    private final ReleveNotesRepository releveRepository;
    private final ReleveNotesService releveNotesService;

    public ReleveDelivranceService(ReleveNotesRepository releveRepository,
            ReleveNotesService releveNotesService) {
        this.releveRepository = releveRepository;
        this.releveNotesService = releveNotesService;
    }

    /**
     * Délivre le relevé d'un étudiant pour la période donnée.
     *
     * <p>Les cours sans note sont écartés des lignes : les faire figurer avec un tiret
     * laisserait croire à un échec là où le formateur n'a simplement rien corrigé.
     */
    @Transactional
    public ReleveNotes delivrer(Etudiant etudiant, String periode) {
        ReleveNotesDto courant = releveNotesService.duEtudiant(etudiant.getId());

        ReleveNotes releve = new ReleveNotes();
        releve.setEtudiant(etudiant);
        releve.setPromotion(etudiant.getPromotion());
        releve.setPeriode(periode);
        releve.setDelivrePar(AUTORITE);
        releve.setMoyenneGenerale(courant.moyenneGenerale());
        releve.setNombreNotes(courant.nombreNotes());

        for (ReleveNotesDto.NotesCoursDto groupe : courant.parCours()) {
            if (groupe.moyenne() == null) {
                continue;
            }
            ReleveLigne ligne = new ReleveLigne();
            ligne.setCoursCode(groupe.coursCode());
            ligne.setCoursTitre(groupe.coursTitre());
            ligne.setCoefficient(groupe.coefficient());
            ligne.setMoyenne(groupe.moyenne());
            releve.ajouterLigne(ligne);
        }

        return releveRepository.save(releve);
    }

    @Transactional(readOnly = true)
    public List<ReleveNotes> tous() {
        return releveRepository.findAllByOrderByDateEmissionDesc();
    }

    @Transactional(readOnly = true)
    public List<ReleveNotes> deLEtudiant(Long etudiantId) {
        return releveRepository.findByEtudiant_IdOrderByDateEmissionDesc(etudiantId);
    }
}
