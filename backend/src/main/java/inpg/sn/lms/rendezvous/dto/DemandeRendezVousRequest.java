package inpg.sn.lms.rendezvous.dto;

/** {@code modalite} : EN_LIGNE ou PRESENTIEL. */
public record DemandeRendezVousRequest(
        Long creneauId,
        String modalite,
        String lieuOuLien,
        String motif) {
}
