package inpg.sn.lms.recrutement.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Programme de formation exposé par {@code /api/settings/programs}.
 *
 * <p>Ce DTO existe pour une raison précise : {@code Program} porte un
 * {@code @OneToMany List<SessionCandidature> sessions} en chargement paresseux,
 * et {@code SessionCandidature} porte en retour un {@code @ManyToOne Program}.
 * Sérialiser l'entité brute donnait donc deux défauts cumulés :
 *
 * <ul>
 *   <li>hors transaction ({@code open-in-view=false}), Jackson touchait la
 *       collection paresseuse une fois la session Hibernate fermée — l'endpoint
 *       répondait 500 « Failed to write request », sans même passer par
 *       {@code GlobalExceptionHandler} puisque l'échec survient pendant
 *       l'écriture de la réponse, après le retour du contrôleur ;</li>
 *   <li>à supposer la collection chargée, la référence retour
 *       {@code session.program} relançait la sérialisation du programme :
 *       récursion infinie.</li>
 * </ul>
 *
 * <p>Élargir la transaction aurait donc échangé une panne contre l'autre. Le
 * nombre de sessions rattachées n'est volontairement pas exposé ici : le
 * calculer exigerait de retoucher cette même collection. S'il devient utile à
 * un écran, il devra venir d'un {@code count} au niveau du dépôt.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProgramResponse {

    private Long id;
    private String title;
    private String level;
    private String department;
}
