package inpg.sn.lms.espace_alumni.dto;

/**
 * Temps forts du parcours d'un alumni (profil enrichi), agrégés à partir de données
 * qui existent déjà ailleurs dans le dépôt — relevé délivré, historique de postes,
 * salon de forum alumni, documents partagés, enquêtes de suivi — plutôt qu'une
 * nouvelle table de métriques à tenir à jour en parallèle.
 *
 * <p>{@code moyenneGeneraleDernierReleve} et {@code periodeDernierReleve} restent
 * {@code null} tant qu'aucun relevé n'a été délivré : ce n'est pas la même chose
 * qu'une moyenne de zéro, cf. la distinction relevé courant/délivré de CLAUDE.md §3.
 *
 * <p>{@code nombreDocumentsPartages} compte ce que l'alumni a déposé (contribution),
 * {@code nombreDocumentsConsultes} ce qu'il a réellement ouvert (engagement,
 * migration V29) — les deux se lisent différemment et aucun ne remplace l'autre.
 */
public record AlumniMetriquesDto(
        int anneesDepuisDiplome,
        String promotionLibelle,
        Double moyenneGeneraleDernierReleve,
        String periodeDernierReleve,
        int nombreRelevesDelivres,
        int nombrePostesParcours,
        boolean posteActuelRenseigne,
        long nombreSujetsForumAlumni,
        long nombreMessagesForumAlumni,
        long nombreDocumentsPartages,
        long nombreDocumentsConsultes,
        long nombreEnquetesRepondues,
        long nombreEnquetesActives) {
}
