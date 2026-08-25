package inpg.sn.lms.recrutement.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Demande de paiement des frais de dossier.
 *
 * <p>Le montant et la devise ne figurent volontairement pas ici : ce sont des
 * données métier, fixées par l'établissement et lues dans la configuration du
 * serveur. Les accepter depuis la requête laissait le client choisir le prix
 * qu'il paie — y compris zéro.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaiementRequest {

    private Long candidatureId;
    private String methodePaiement;
}