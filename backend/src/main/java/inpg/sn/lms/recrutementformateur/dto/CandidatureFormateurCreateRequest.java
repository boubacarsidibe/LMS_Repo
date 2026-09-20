package inpg.sn.lms.recrutementformateur.dto;

import java.util.List;
import java.util.Set;

/**
 * Dépôt d'une candidature à une offre de recrutement de formateur.
 * {@code offreId}, {@code nom}, {@code prenom} et {@code telephone} sont
 * obligatoires (validés dans le contrôleur) ; l'e-mail est repris du compte
 * connecté. Tout le reste — état civil, adresse, formations, expériences,
 * langues, certifications, domaines d'expertise — est facultatif : un dossier
 * enrichi façon SuccessFactors, sans en imposer le formalisme. Les dates sont
 * des chaînes AAAA-MM-JJ (patron {@code OffreFormateurRequest}), ignorées
 * silencieusement si mal formées plutôt que de faire échouer tout le dépôt.
 */
public record CandidatureFormateurCreateRequest(
        Long offreId,
        String nom,
        String prenom,
        String telephone,
        String diplomePrincipal,
        Integer anneesExperience,

        String civilite,
        String genre,
        String dateNaissance,
        String paysNaissance,
        String lieuNaissance,
        String nationalite,
        String nationaliteSecondaire,
        String niveauEtude,
        String telephoneSecondaire,
        String emailAlternatif,
        String adresseLigne1,
        String adresseLigne2,
        String ville,
        String paysResidence,

        List<FormationRequest> formations,
        List<ExperienceRequest> experiences,
        List<LangueRequest> langues,
        List<CertificationRequest> certifications,
        Set<String> domainesExpertise) {

    public record FormationRequest(
            String niveauEtude,
            String pays,
            String ville,
            String etablissement,
            String intituleDiplome,
            String matierePrincipale,
            String dateDebut,
            String dateObtention) {
    }

    public record ExperienceRequest(
            String employeur,
            String pays,
            String ville,
            boolean posteActuel,
            String intitulePoste,
            String dateDebut,
            String dateFin,
            String nomSuperviseur,
            String titreSuperviseur,
            String telephoneSuperviseur,
            String emailSuperviseur,
            Boolean contactAutorise,
            String motifNonContact,
            String motifDepart) {
    }

    public record LangueRequest(String langue, String niveau) {
    }

    public record CertificationRequest(
            String intitule,
            String organisme,
            String dateObtention,
            String dateExpiration) {
    }
}
