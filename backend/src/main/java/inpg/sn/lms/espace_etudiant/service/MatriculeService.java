package inpg.sn.lms.espace_etudiant.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.espace_etudiant.entity.Promotion;

/**
 * Attribution de l'identifiant unique des étudiants.
 *
 * <p>Exigence du cahier des charges §3.2 : « créer automatiquement un identifiant
 * unique pour chaque étudiant ou apprenant ». Le format retenu,
 * {@code INPG-<année>-<séquence sur 4 chiffres>}, est lisible à l'œil et se
 * rattache à l'année d'entrée de la promotion.
 */
@Service
@Transactional
public class MatriculeService {

    private static final String PREFIXE = "INPG";
    /** Garde-fou : au-delà, c'est que la séquence ne converge pas. */
    private static final int TENTATIVES_MAX = 50;

    private final EtudiantRepository etudiantRepository;

    public MatriculeService(EtudiantRepository etudiantRepository) {
        this.etudiantRepository = etudiantRepository;
    }

    /**
     * Attribue un matricule à l'étudiant s'il n'en a pas déjà un, et le renvoie.
     * L'appel est idempotent : un étudiant déjà identifié conserve son matricule.
     */
    public String attribuerSiAbsent(Etudiant etudiant) {
        if (etudiant.getMatricule() != null && !etudiant.getMatricule().isBlank()) {
            return etudiant.getMatricule();
        }
        String matricule = genererPour(etudiant.getPromotion());
        etudiant.setMatricule(matricule);
        etudiantRepository.save(etudiant);
        return matricule;
    }

    /**
     * Rattrapage au démarrage : identifie les étudiants créés avant l'introduction
     * du matricule. Renvoie le nombre de matricules attribués.
     */
    public int attribuerAuxEtudiantsSansMatricule() {
        List<Etudiant> sansMatricule = etudiantRepository.findByMatriculeIsNull();
        for (Etudiant etudiant : sansMatricule) {
            attribuerSiAbsent(etudiant);
        }
        return sansMatricule.size();
    }

    /**
     * Construit un matricule libre pour l'année de la promotion. La séquence part
     * du nombre de matricules déjà émis pour cette année, puis avance jusqu'à
     * trouver une valeur disponible — ce qui absorbe les trous laissés par des
     * suppressions comme les collisions entre deux créations rapprochées.
     */
    private String genererPour(Promotion promotion) {
        int annee = anneeDe(promotion);
        String prefixeAnnee = PREFIXE + "-" + annee + "-";
        long sequence = etudiantRepository.countByMatriculeStartingWith(prefixeAnnee) + 1;

        for (int tentative = 0; tentative < TENTATIVES_MAX; tentative++) {
            String candidat = prefixeAnnee + String.format("%04d", sequence);
            if (etudiantRepository.findByMatricule(candidat).isEmpty()) {
                return candidat;
            }
            sequence++;
        }
        throw new IllegalStateException(
                "Impossible d'attribuer un matricule pour l'année " + annee + " après " + TENTATIVES_MAX
                        + " tentatives.");
    }

    private int anneeDe(Promotion promotion) {
        if (promotion != null && promotion.getDateDebut() != null) {
            return promotion.getDateDebut().getYear();
        }
        return LocalDate.now().getYear();
    }
}
