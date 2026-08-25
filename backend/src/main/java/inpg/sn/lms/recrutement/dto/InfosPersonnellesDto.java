package inpg.sn.lms.recrutement.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * État civil saisi par le candidat, transmis par le frontend sous forme de chaîne
 * JSON dans {@code CandidatureRequest.informationsPersonnelles}.
 *
 * <p>Ce contenu était auparavant découpé à la main, en cherchant littéralement la
 * sous-chaîne {@code "champ":"} puis en lisant jusqu'au guillemet suivant. Cette
 * approche ne reconnaissait que les valeurs entre guillemets : un
 * {@code "anneesExperience": 5} — un nombre JSON, parfaitement légitime — était
 * ignoré sans la moindre erreur. Or ces champs alimentent
 * {@code SessionCandidature.respecteCriteres()}, le filtre automatique de
 * présélection : un champ perdu, c'est un dossier filtré sur des données
 * incomplètes.
 *
 * <p>{@code ignoreUnknown} est posé sur le DTO plutôt que sur l'ObjectMapper
 * partagé : le formulaire envoie des champs de confort (indicateurs d'étape,
 * cases à cocher) qui ne concernent pas l'état civil, et relâcher la règle
 * globalement masquerait de vraies fautes de frappe ailleurs dans l'application.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record InfosPersonnellesDto(
        String nom,
        String prenom,
        String telephone,
        String nationalite,
        LocalDate dateNaissance,
        String niveauEtude,
        String genre,
        String profession,
        Integer anneesExperience) {

    /** {@code true} si la valeur est utilisable, c'est-à-dire ni absente ni vide. */
    private static boolean renseigne(String valeur) {
        return valeur != null && !valeur.isBlank();
    }

    public boolean aNom() { return renseigne(nom); }

    public boolean aPrenom() { return renseigne(prenom); }

    public boolean aTelephone() { return renseigne(telephone); }

    public boolean aNationalite() { return renseigne(nationalite); }

    public boolean aNiveauEtude() { return renseigne(niveauEtude); }

    public boolean aGenre() { return renseigne(genre); }

    public boolean aProfession() { return renseigne(profession); }
}
