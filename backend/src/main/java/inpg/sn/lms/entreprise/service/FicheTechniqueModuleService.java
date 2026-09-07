package inpg.sn.lms.entreprise.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.springframework.stereotype.Service;

import inpg.sn.lms.entreprise.dto.FicheModuleJourDto;
import inpg.sn.lms.entreprise.dto.FicheTechniqueModuleDto;
import inpg.sn.lms.entreprise.dto.FicheTechniqueModuleRequest;
import inpg.sn.lms.entreprise.entity.FicheModuleJour;
import inpg.sn.lms.entreprise.entity.FicheTechniqueModule;

/**
 * Fabrique et met à jour une {@link FicheTechniqueModule} — logique de recopie
 * partagée entre l'espace pédagogie (édition) et l'espace entreprise (lecture),
 * pour ne pas dupliquer la trentaine de champs du gabarit dans deux contrôleurs.
 */
@Service
public class FicheTechniqueModuleService {

    /** Recopie les champs de la requête dans la fiche et reconstruit le planning jour par jour. */
    public void appliquer(FicheTechniqueModule fiche, FicheTechniqueModuleRequest r) {
        fiche.setCode(vide(r.code()));
        fiche.setIntitule(vide(r.intitule()));
        fiche.setVersion(vide(r.version()));
        fiche.setDateRevision(parseDate(r.dateRevision()));
        fiche.setResponsableModule(vide(r.responsableModule()));

        fiche.setDepartement(vide(r.departement()));
        fiche.setAtelierService(vide(r.atelierService()));
        fiche.setFonctionsCibles(vide(r.fonctionsCibles()));
        fiche.setThematiques(vide(r.thematiques()));
        fiche.setPrerequis(vide(r.prerequis()));
        fiche.setLangue(r.langue() == null || r.langue().isBlank() ? "Français" : r.langue().trim());
        fiche.setNiveau(vide(r.niveau()));
        fiche.setEffectif(r.effectif());
        fiche.setDureeHeures(r.dureeHeures());
        fiche.setDureeJours(r.dureeJours());
        fiche.setPeriodeProbable(vide(r.periodeProbable()));

        fiche.setObjectifGeneral(vide(r.objectifGeneral()));
        fiche.setObjectifsSpecifiques(vide(r.objectifsSpecifiques()));
        fiche.setModalitesTestPrerequis(vide(r.modalitesTestPrerequis()));
        fiche.setFormationSurSite(vide(r.formationSurSite()));
        fiche.setTypesEvaluation(vide(r.typesEvaluation()));
        fiche.setRessourcesPedagogiques(vide(r.ressourcesPedagogiques()));
        fiche.setBibliographie(vide(r.bibliographie()));
        fiche.setProfilFormateur(vide(r.profilFormateur()));

        fiche.setCoutTotal(vide(r.coutTotal()));
        fiche.setModalitesFinancieres(vide(r.modalitesFinancieres()));

        fiche.getJours().clear();
        if (r.jours() != null) {
            int rang = 0;
            for (FicheTechniqueModuleRequest.JourRequest j : r.jours()) {
                rang++;
                int numero = j.numero() > 0 ? j.numero() : rang;
                fiche.getJours().add(new FicheModuleJour(fiche, numero, vide(j.contenus()), j.dureeHeures(),
                        vide(j.typeEnseignement()), vide(j.modalites())));
            }
        }
    }

    /**
     * Une fiche est « instruisable » — donc transmissible à l'entreprise — dès
     * qu'elle porte un intitulé, un objectif général et au moins une journée de
     * programme.
     */
    public boolean estTransmissible(FicheTechniqueModule fiche) {
        return fiche.getIntitule() != null && !fiche.getIntitule().isBlank()
                && fiche.getObjectifGeneral() != null && !fiche.getObjectifGeneral().isBlank()
                && fiche.getJours() != null && !fiche.getJours().isEmpty();
    }

    public FicheTechniqueModuleDto toDto(FicheTechniqueModule f) {
        return new FicheTechniqueModuleDto(
                f.getId(),
                f.getDemande() != null ? f.getDemande().getId() : null,
                f.getStatut().name(),
                f.getDateTransmission(),
                f.getCommentaireEntreprise(),
                f.getDateDecisionEntreprise(),
                f.getCode(),
                f.getIntitule(),
                f.getVersion(),
                f.getDateRevision(),
                f.getResponsableModule(),
                f.getDepartement(),
                f.getAtelierService(),
                f.getFonctionsCibles(),
                f.getThematiques(),
                f.getPrerequis(),
                f.getLangue(),
                f.getNiveau(),
                f.getEffectif(),
                f.getDureeHeures(),
                f.getDureeJours(),
                f.getPeriodeProbable(),
                f.getObjectifGeneral(),
                f.getObjectifsSpecifiques(),
                f.getModalitesTestPrerequis(),
                f.getFormationSurSite(),
                f.getTypesEvaluation(),
                f.getRessourcesPedagogiques(),
                f.getBibliographie(),
                f.getProfilFormateur(),
                f.getCoutTotal(),
                f.getModalitesFinancieres(),
                f.getJours().stream()
                        .map(j -> new FicheModuleJourDto(j.getId(), j.getNumero(), j.getContenus(), j.getDureeHeures(),
                                j.getTypeEnseignement(), j.getModalites()))
                        .toList());
    }

    private static String vide(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    private static LocalDate parseDate(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(v.trim());
        } catch (DateTimeParseException invalide) {
            return null;
        }
    }
}
