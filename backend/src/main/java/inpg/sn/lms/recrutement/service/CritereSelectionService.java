package inpg.sn.lms.recrutement.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.recrutement.entity.CritereSelection;
import inpg.sn.lms.recrutement.repository.CritereSelectionRepository;

@Service
@Transactional
public class CritereSelectionService {

    /**
     * Ordre croissant des niveaux d'études tels que proposés dans le formulaire
     * de candidature (frontend). "Autre" n'est volontairement pas classé : un
     * candidat ayant choisi "Autre" ne peut pas être filtré automatiquement sur
     * ce critère, faute de pouvoir le situer sans ambiguïté.
     */
    private static final List<String> NIVEAUX_ORDONNES = List.of(
            "Baccalauréat", "Licence (L3)", "Master 1 (M1)", "Master 2 (M2)");

    private final CritereSelectionRepository repository;

    public CritereSelectionService(CritereSelectionRepository repository) {
        this.repository = repository;
    }

    public CritereSelection getConfig() {
        return repository.findById(1L).orElseGet(() -> repository.save(new CritereSelection()));
    }

    public CritereSelection mettreAJour(CritereSelection request) {
        CritereSelection config = getConfig();
        config.setActif(request.isActif());
        config.setNiveauEtudeMinimum(
                request.getNiveauEtudeMinimum() == null || request.getNiveauEtudeMinimum().isBlank()
                        ? null
                        : request.getNiveauEtudeMinimum());
        config.setAgeMinimum(request.getAgeMinimum());
        config.setAgeMaximum(request.getAgeMaximum());
        return repository.save(config);
    }

    /**
     * Évalue un candidat par rapport aux critères actifs. Retourne toujours
     * {@code true} si le filtre automatique est désactivé.
     */
    public boolean evaluer(Candidat candidat) {
        CritereSelection config = getConfig();
        if (!config.isActif()) {
            return true;
        }

        if (config.getNiveauEtudeMinimum() != null) {
            int seuil = NIVEAUX_ORDONNES.indexOf(config.getNiveauEtudeMinimum());
            int niveauCandidat = NIVEAUX_ORDONNES.indexOf(candidat.getNiveauEtude());
            // Niveau candidat non reconnu (ex: "Autre" ou valeur libre) : on ne bloque pas dessus.
            if (seuil >= 0 && niveauCandidat >= 0 && niveauCandidat < seuil) {
                return false;
            }
        }

        if (candidat.getDateNaissance() != null && (config.getAgeMinimum() != null || config.getAgeMaximum() != null)) {
            int age = Period.between(candidat.getDateNaissance(), LocalDate.now()).getYears();
            if (config.getAgeMinimum() != null && age < config.getAgeMinimum()) {
                return false;
            }
            if (config.getAgeMaximum() != null && age > config.getAgeMaximum()) {
                return false;
            }
        }

        return true;
    }
}
