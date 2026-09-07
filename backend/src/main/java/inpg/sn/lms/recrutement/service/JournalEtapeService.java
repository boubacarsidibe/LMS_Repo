package inpg.sn.lms.recrutement.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.recrutement.dto.EtapeDossierResponse;
import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.EtapeDossier;
import inpg.sn.lms.recrutement.enums.EnumEtapeQms;
import inpg.sn.lms.recrutement.enums.EnumStatutCandidature;
import inpg.sn.lms.recrutement.repository.EtapeDossierRepository;

/**
 * Journal des étapes d'un dossier, aligné sur la procédure qualité QMS-009.
 *
 * <p>Une seule règle, mais elle commande tout : on <strong>ajoute</strong> une ligne,
 * jamais on n'en modifie ni n'en supprime une. Le journal ne se recalcule pas depuis
 * l'état courant du dossier — sans quoi il ne prouverait rien qu'on ne sache déjà.
 */
@Service
@Transactional
public class JournalEtapeService {

    /** Nom porté par les lignes qu'aucun agent n'a déclenchées (dépôt candidat, traitement automatique). */
    private static final String AUTEUR_SYSTEME = "Système";

    private final EtapeDossierRepository repository;

    public JournalEtapeService(EtapeDossierRepository repository) {
        this.repository = repository;
    }

    /**
     * Consigne une étape franchie.
     *
     * @param auteur agent à l'origine de l'étape, ou {@code null} pour une étape
     *               non déclenchée par un agent (dépôt du candidat, filtre automatique)
     */
    public EtapeDossier enregistrer(Candidature candidature, EnumEtapeQms etape,
            EnumStatutCandidature statutResultant, User auteur, String commentaire) {
        EtapeDossier ligne = new EtapeDossier();
        ligne.setCandidature(candidature);
        ligne.setEtape(etape);
        ligne.setStatutResultant(statutResultant);
        ligne.setAuteur(auteur);
        ligne.setAuteurNom(nomLisible(auteur));
        ligne.setCommentaire(commentaire);
        return repository.save(ligne);
    }

    public List<EtapeDossierResponse> journalDe(Long candidatureId) {
        return repository.findByCandidatureIdOrderByDateEtapeAsc(candidatureId).stream()
                .map(e -> EtapeDossierResponse.builder()
                        .id(e.getId())
                        .etape(e.getEtape())
                        .statutResultant(e.getStatutResultant())
                        .auteurNom(e.getAuteurNom())
                        .dateEtape(e.getDateEtape())
                        .commentaire(e.getCommentaire())
                        .build())
                .collect(Collectors.toList());
    }

    /** Une étape déjà franchie ne se refranchit pas : sert à savoir si l'inscription a déjà eu lieu. */
    public boolean etapeFranchie(Long candidatureId, EnumEtapeQms etape) {
        return repository.existsByCandidatureIdAndEtape(candidatureId, etape);
    }

    /**
     * Cliché du nom, figé à l'écriture : la suppression ou le renommage d'un compte ne doit
     * pas réécrire l'historique (même principe que {@code releve_ligne}, migration V9).
     */
    private String nomLisible(User auteur) {
        if (auteur == null) {
            return AUTEUR_SYSTEME;
        }
        String nom = auteur.getUsername();
        if (nom == null || nom.isBlank()) {
            nom = auteur.getEmail();
        }
        return nom == null || nom.isBlank() ? AUTEUR_SYSTEME : nom;
    }
}
