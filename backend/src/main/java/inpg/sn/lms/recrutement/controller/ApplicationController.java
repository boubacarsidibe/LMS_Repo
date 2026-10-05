package inpg.sn.lms.recrutement.controller;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import inpg.sn.lms.recrutement.entity.PieceJustificative;
import inpg.sn.lms.recrutement.enums.EnumStatutCandidature;
import inpg.sn.lms.recrutement.enums.EnumStatutVerificationPiece;
import inpg.sn.lms.recrutement.enums.EnumTypeDocument;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.repository.CandidatureRepository;
import inpg.sn.lms.recrutement.service.NotificationCandidatService;
import inpg.sn.lms.recrutement.service.NotificationService;

/**
 * Relance des dossiers incomplets.
 *
 * <p>Ce contrôleur exposait aussi {@code GET /kpis}, {@code GET /}, {@code GET /{id}},
 * {@code PATCH /{id}/documents/status} et {@code GET /export} : cinq endpoints qu'aucun
 * écran n'appelait. Le {@code PATCH} faisait doublon avec
 * {@code PUT /api/admin/recrutement/pieces/{id}/conformite}, seul utilisé — deux chemins
 * concurrents vers le même champ {@code estConforme}, celui qui décide justement quels
 * dossiers sont relancés ici. Ils ont été retirés plutôt que maintenus à l'aveugle.
 */
@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private static final Logger log = LoggerFactory.getLogger(ApplicationController.class);

    private final CandidatureRepository candidatureRepo;
    private final NotificationService notificationService;
    private final NotificationCandidatService notificationCandidatService;

    public ApplicationController(CandidatureRepository candidatureRepo, NotificationService notificationService,
            NotificationCandidatService notificationCandidatService) {
        this.candidatureRepo = candidatureRepo;
        this.notificationService = notificationService;
        this.notificationCandidatService = notificationCandidatService;
    }

    /**
     * Relance réellement les candidats dont le dossier est incomplet (email + notification
     * in-app), en nommant les pièces manquantes ou non conformes — jusqu'ici l'endpoint ne
     * faisait que compter les dossiers concernés sans jamais rien envoyer ("Simulation
     * d'envoi"). Un dossier sans piece du tout ("aucune pièce déposée") est traité comme les
     * autres, pas ignoré.
     */
    @PostMapping("/reminder-incomplets")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    @Transactional
    public ResponseEntity<Map<String, Object>> sendReminder() {
        List<Candidature> incomplets = candidatureRepo.findAll().stream()
                .filter(c -> STATUTS_RELANCABLES.contains(c.getStatut()))
                .filter(this::isDossierIncomplet)
                .toList();

        int envoyes = 0;
        List<String> echecs = new ArrayList<>();
        for (Candidature c : incomplets) {
            String email = c.getCandidat() != null && c.getCandidat().getUser() != null
                    ? c.getCandidat().getUser().getEmail()
                    : null;
            if (email == null) {
                echecs.add(c.getNumeroDossier() + " (aucune adresse)");
                continue;
            }
            String piecesManquantes = piecesManquantes(c);
            try {
                notificationService.notifierRappelDossierIncomplet(email, c.getNumeroDossier(), piecesManquantes);
                notificationCandidatService.creerNotification(c.getCandidat().getId(),
                        "Votre dossier n°" + c.getNumeroDossier() + " est incomplet : " + piecesManquantes + ".",
                        "DOSSIER", "/recrutement/suivi");
                envoyes++;
            } catch (Exception e) {
                log.error("Échec de la relance de dossier incomplet à {} : {}", email, e.getMessage(), e);
                echecs.add(email);
            }
        }
        return ResponseEntity.ok(Map.of("envoyes", envoyes, "echecs", echecs));
    }

    /**
     * Pièces exigées de tout candidat, dans l'ordre où le formulaire les demande.
     *
     * <p>Le diplôme n'y figure pas : il reste facultatif, ajoutable après coup depuis
     * l'espace candidat. La lettre de motivation non plus, elle n'est plus collectée.
     */
    private static final List<EnumTypeDocument> PIECES_REQUISES = List.of(
            EnumTypeDocument.PIECE_IDENTITE,
            EnumTypeDocument.CV,
            EnumTypeDocument.RELEVE_NOTES);

    /**
     * Statuts pour lesquels réclamer une pièce a encore un sens.
     *
     * <p>Sans ce filtre, durcir la définition d'un dossier incomplet enverrait un rappel
     * « votre dossier est incomplet » à des candidats déjà admis ou déjà rejetés, pour un
     * document qui ne changera plus rien à leur situation.
     */
    private static final Set<EnumStatutCandidature> STATUTS_RELANCABLES = EnumSet.of(
            EnumStatutCandidature.SOUMIS,
            EnumStatutCandidature.PRESELECTIONNE,
            EnumStatutCandidature.PAIEMENT_EN_ATTENTE);

    /**
     * Ce qu'il reste exactement à fournir, nommé pièce par pièce.
     *
     * <p>Deux manques distincts, que la relance ne confondait pas seulement : elle en ignorait
     * un. Elle ne listait que les pièces <em>déposées puis jugées non conformes</em>, et
     * répondait « aucune pièce déposée » dès qu'il n'y en avait pas — y compris à un candidat
     * ayant déposé un CV conforme mais aucune pièce d'identité. Il recevait donc un rappel qui
     * lui donnait tort et ne lui disait pas quoi faire.
     */
    private String piecesManquantes(Candidature c) {
        return String.join(", ", manques(c));
    }

    /**
     * Un dossier est incomplet dès qu'il reste quelque chose à fournir ou à remplacer.
     *
     * <p>Le contrôle portait auparavant sur la seule conformité des pièces présentes : un
     * dossier auquel il manquait une pièce requise passait pour complet et n'était jamais
     * relancé.
     */
    private boolean isDossierIncomplet(Candidature c) {
        return !manques(c).isEmpty();
    }

    /** Une entrée par pièce à fournir ou à remplacer, prête à être lue par le candidat. */
    private List<String> manques(Candidature c) {
        Map<EnumTypeDocument, List<PieceJustificative>> parType = c.getPiecesJustificatives().stream()
                .filter(p -> p.getTypeDocument() != null)
                .collect(Collectors.groupingBy(PieceJustificative::getTypeDocument));

        List<String> manques = new ArrayList<>();
        for (EnumTypeDocument requis : PIECES_REQUISES) {
            List<PieceJustificative> deposees = parType.get(requis);
            if (deposees == null || deposees.isEmpty()) {
                manques.add(requis.libelle() + " (non fournie)");
            } else if (deposees.stream().anyMatch(
                    p -> p.getStatutVerification() == EnumStatutVerificationPiece.A_REMPLACER)) {
                manques.add(requis.libelle() + " (à remplacer : non conforme)");
            }
        }
        // Une pièce facultative déposée puis refusée se signale aussi : le candidat l'a
        // fournie, elle n'est pas exploitable, il doit le savoir.
        parType.forEach((type, deposees) -> {
            if (!PIECES_REQUISES.contains(type) && deposees.stream().anyMatch(
                    p -> p.getStatutVerification() == EnumStatutVerificationPiece.A_REMPLACER)) {
                manques.add(type.libelle() + " (à remplacer : non conforme)");
            }
        });
        return manques;
    }
}
