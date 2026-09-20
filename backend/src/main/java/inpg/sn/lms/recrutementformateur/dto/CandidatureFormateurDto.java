package inpg.sn.lms.recrutementformateur.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Candidature de formateur telle que servie au candidat (ses propres dossiers)
 * et au responsable admission (instruction). Porte l'offre visée, le suivi,
 * les pièces, et le dossier enrichi façon SuccessFactors (état civil,
 * adresse, formations, expériences, langues, certifications, domaines).
 */
public record CandidatureFormateurDto(
        Long id,
        Long offreId,
        String intituleOffre,
        String typeContratOffre,
        String nom,
        String prenom,
        String email,
        String telephone,
        String diplomePrincipal,
        Integer anneesExperience,
        String statut,
        String decision,
        String motifDecision,
        LocalDateTime dateSoumission,
        LocalDateTime dateDecision,
        boolean formateurCree,
        List<PieceCandidatureFormateurDto> pieces,

        String civilite,
        String genre,
        LocalDate dateNaissance,
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

        List<FormationDto> formations,
        List<ExperienceDto> experiences,
        List<LangueDto> langues,
        List<CertificationDto> certifications,
        Set<String> domainesExpertise) {

    public record FormationDto(
            Long id,
            String niveauEtude,
            String pays,
            String ville,
            String etablissement,
            String intituleDiplome,
            String matierePrincipale,
            LocalDate dateDebut,
            LocalDate dateObtention) {
    }

    public record ExperienceDto(
            Long id,
            String employeur,
            String pays,
            String ville,
            boolean posteActuel,
            String intitulePoste,
            LocalDate dateDebut,
            LocalDate dateFin,
            String nomSuperviseur,
            String titreSuperviseur,
            String telephoneSuperviseur,
            String emailSuperviseur,
            Boolean contactAutorise,
            String motifNonContact,
            String motifDepart) {
    }

    public record LangueDto(Long id, String langue, String niveau) {
    }

    public record CertificationDto(
            Long id,
            String intitule,
            String organisme,
            LocalDate dateObtention,
            LocalDate dateExpiration) {
    }
}
