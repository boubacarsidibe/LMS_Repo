package inpg.sn.lms.espace_etudiant.dto;

import java.util.List;

/**
 * Bilan d'une délivrance collective : qui a reçu son certificat, et qui ne l'a pas reçu
 * avec la raison. Un refus individuel n'annule pas les autres délivrances.
 */
public record CertificatLotResultatDto(List<Delivre> delivres, List<Refuse> refuses) {

    public record Delivre(Long certificatId, String reference, Long etudiantId, String etudiantNom) {
    }

    public record Refuse(Long etudiantId, String etudiantNom, String raison, List<String> coursNonTermines) {
    }
}
