package inpg.sn.lms.recrutement.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import inpg.sn.lms.recrutement.dto.DocumentAttenduDto;
import inpg.sn.lms.recrutement.dto.DocumentAttenduDto.Etat;
import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.PieceJustificative;
import inpg.sn.lms.recrutement.enums.EnumStatutVerificationPiece;
import inpg.sn.lms.recrutement.enums.EnumTypeDocument;

/**
 * Ce qu'un dossier de candidature doit contenir, et ce qui lui manque.
 *
 * <p><strong>Seule définition</strong> des pièces exigées. Elle était écrite trois fois — dans le formulaire, dans
 * la page de suivi et dans la relance des dossiers incomplets — et les copies avaient divergé : le candidat voyait
 * « tous les documents requis téléchargés » pendant que la relance lui réclamait son diplôme. Le formulaire, le suivi,
 * la fiche d'admission et la relance lisent désormais ce qui est calculé ici.
 */
public final class CompletudeDossier {

    /** Niveau d'accès des CPS : un candidat de ce niveau n'a pas de diplôme à présenter. */
    public static final String NIVEAU_SANS_DIPLOME = "4e secondaire";

    private static final List<EnumTypeDocument> PIECES_DE_BASE = List.of(
            EnumTypeDocument.PIECE_IDENTITE,
            EnumTypeDocument.CV,
            EnumTypeDocument.RELEVE_NOTES);

    private CompletudeDossier() {
    }

    /**
     * Pièces exigées de tout candidat, dans l'ordre où le formulaire les demande : pièce d'identité, CV, relevés de
     * notes — auxquelles s'ajoute le diplôme, sauf pour un candidat de niveau « 4e secondaire ».
     *
     * <p>L'INPG exige l'attestation de diplôme dans tout dossier (questionnaire de collecte du 2026-09-30) ; elle ne
     * peut pas l'être pour les CPS, ouverts dès la 4ᵉ secondaire. À garder identique à {@code diplomeRequis} du
     * frontend ({@code recrutement/niveaux-etudes.ts}) : un niveau absent ou inconnu n'exonère pas. La lettre de
     * motivation n'est plus collectée.
     */
    public static List<EnumTypeDocument> piecesRequises(Candidature c) {
        String niveau = c.getCandidat() != null ? c.getCandidat().getNiveauEtude() : null;
        if (NIVEAU_SANS_DIPLOME.equals(niveau)) {
            return PIECES_DE_BASE;
        }
        List<EnumTypeDocument> requises = new ArrayList<>(PIECES_DE_BASE);
        requises.add(EnumTypeDocument.DIPLOME);
        return requises;
    }

    /** Une entrée par pièce exigée, avec son état : ce que le suivi du candidat et la fiche d'admission affichent. */
    public static List<DocumentAttenduDto> documentsAttendus(Candidature c) {
        Map<EnumTypeDocument, List<PieceJustificative>> parType = piecesParType(c);
        List<DocumentAttenduDto> attendus = new ArrayList<>();
        for (EnumTypeDocument requis : piecesRequises(c)) {
            List<PieceJustificative> deposees = parType.get(requis);
            Etat etat;
            if (deposees == null || deposees.isEmpty()) {
                etat = Etat.A_FOURNIR;
            } else if (aUnePieceARemplacer(deposees)) {
                etat = Etat.A_REMPLACER;
            } else {
                etat = Etat.FOURNI;
            }
            attendus.add(new DocumentAttenduDto(requis, requis.libelle(), etat));
        }
        return attendus;
    }

    /**
     * Une entrée par pièce à fournir ou à remplacer, prête à être lue par le candidat (relance par e-mail).
     * Vide si le dossier est complet.
     */
    public static List<String> manques(Candidature c) {
        Map<EnumTypeDocument, List<PieceJustificative>> parType = piecesParType(c);
        List<EnumTypeDocument> piecesRequises = piecesRequises(c);

        List<String> manques = new ArrayList<>();
        for (EnumTypeDocument requis : piecesRequises) {
            List<PieceJustificative> deposees = parType.get(requis);
            if (deposees == null || deposees.isEmpty()) {
                manques.add(requis.libelle() + " (non fournie)");
            } else if (aUnePieceARemplacer(deposees)) {
                manques.add(requis.libelle() + " (à remplacer : non conforme)");
            }
        }
        // Une pièce facultative déposée puis refusée se signale aussi : le candidat l'a fournie, elle n'est pas
        // exploitable, il doit le savoir.
        parType.forEach((type, deposees) -> {
            if (!piecesRequises.contains(type) && aUnePieceARemplacer(deposees)) {
                manques.add(type.libelle() + " (à remplacer : non conforme)");
            }
        });
        return manques;
    }

    private static Map<EnumTypeDocument, List<PieceJustificative>> piecesParType(Candidature c) {
        return c.getPiecesJustificatives().stream()
                .filter(p -> p.getTypeDocument() != null)
                .collect(Collectors.groupingBy(PieceJustificative::getTypeDocument));
    }

    private static boolean aUnePieceARemplacer(List<PieceJustificative> pieces) {
        return !piecesRefuseesSansRemplacant(pieces).isEmpty();
    }

    /**
     * Les pièces refusées par l'admission qui n'ont pas encore été remplacées.
     *
     * <p>Une pièce refusée reste en base avec son statut {@code A_REMPLACER} : elle sert de preuve du refus et de
     * motif. Compter ce statut tel quel laissait le dossier « à remplacer » pour toujours, même une fois le
     * remplaçant déposé. Une pièce refusée est donc <em>remplacée</em> quand une autre pièce du même type, non
     * refusée, a été déposée <strong>après</strong> le refus. Chaque remplaçant ne couvre qu'un refus : trois
     * relevés refusés réclament trois dépôts, pas un seul.
     */
    static List<PieceJustificative> piecesRefuseesSansRemplacant(List<PieceJustificative> piecesDuType) {
        List<PieceJustificative> refusees = piecesDuType.stream()
                .filter(p -> p.getStatutVerification() == EnumStatutVerificationPiece.A_REMPLACER)
                .sorted(Comparator.comparing(CompletudeDossier::instantDuRefus, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        List<LocalDateTime> remplacants = new ArrayList<>(piecesDuType.stream()
                .filter(p -> p.getStatutVerification() != EnumStatutVerificationPiece.A_REMPLACER)
                .map(CompletudeDossier::instantDuDepot)
                .filter(java.util.Objects::nonNull)
                .sorted()
                .toList());

        List<PieceJustificative> sansRemplacant = new ArrayList<>();
        for (PieceJustificative refusee : refusees) {
            LocalDateTime refus = instantDuRefus(refusee);
            LocalDateTime remplacant = refus == null ? null
                    : remplacants.stream().filter(depot -> depot.isAfter(refus)).findFirst().orElse(null);
            if (remplacant == null) {
                sansRemplacant.add(refusee);
            } else {
                remplacants.remove(remplacant);
            }
        }
        return sansRemplacant;
    }

    private static LocalDateTime instantDuRefus(PieceJustificative p) {
        return p.getDateVerification();
    }

    private static LocalDateTime instantDuDepot(PieceJustificative p) {
        return p.getFichierStocke() != null ? p.getFichierStocke().getDateUpload() : null;
    }
}
