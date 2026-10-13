package inpg.sn.lms.recrutement.service;

import inpg.sn.lms.exception.BusinessException;
import inpg.sn.lms.exception.ResourceNotFoundException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.CandidatRepository;
import inpg.sn.lms.recrutement.dto.CandidatureRequest;
import inpg.sn.lms.recrutement.dto.CandidatureResponse;
import inpg.sn.lms.recrutement.dto.FiltreCriteresResponse;
import inpg.sn.lms.recrutement.dto.PaiementResponse;
import inpg.sn.lms.recrutement.dto.PieceJustificativeResponse;
import inpg.sn.lms.recrutement.dto.RapportRecrutementDto;
import inpg.sn.lms.recrutement.dto.ReponseCritereResponse;
import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.ReponseCritereCandidature;
import inpg.sn.lms.recrutement.entity.SessionCandidature;
import inpg.sn.lms.recrutement.enums.EnumEtapeQms;
import inpg.sn.lms.recrutement.enums.EnumStatutCandidature;
import inpg.sn.lms.recrutement.repository.CandidatureRepository;
import inpg.sn.lms.recrutement.repository.SessionCandidatureRepository;

@Service
@Transactional
public class CandidatureService {

    private final CandidatureRepository candidatureRepository;
    private final SessionCandidatureRepository sessionCandidatureRepository;
    private final CandidatRepository candidatRepository;
    private final JournalEtapeService journalEtapeService;

    public CandidatureService(CandidatureRepository candidatureRepository,
            SessionCandidatureRepository sessionCandidatureRepository,
            CandidatRepository candidatRepository,
            JournalEtapeService journalEtapeService) {
        this.candidatureRepository = candidatureRepository;
        this.sessionCandidatureRepository = sessionCandidatureRepository;
        this.candidatRepository = candidatRepository;
        this.journalEtapeService = journalEtapeService;
    }

    /**
     * Consigne une étape de la procédure QMS-009 sur un dossier. Passe par ce service
     * plutôt que directement par le journal : les appelants n'ont ainsi pas besoin du
     * dépôt des candidatures pour retrouver l'entité.
     */
    public void journaliser(Long candidatureId, EnumEtapeQms etape, EnumStatutCandidature statutResultant,
            User auteur, String commentaire) {
        Candidature candidature = candidatureRepository.findById(candidatureId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature non trouvée avec l'ID: " + candidatureId));
        journalEtapeService.enregistrer(candidature, etape, statutResultant, auteur, commentaire);
    }

    /**
     * Filtre automatique de premier niveau (TDR 3.1.1) : évalue chaque dossier SOUMIS
     * de la session par rapport à ses critères (de base + personnalisés) et bascule en
     * présélection ou rejette en conséquence. Déclenché à la main par le responsable
     * admission (jamais un effet de bord de la soumission — voir CandidatController),
     * afin que chaque dossier reste visible à l'état SOUMIS le temps de la revue.
     */
    public FiltreCriteresResponse appliquerFiltreCriteres(Long sessionId, User auteur) {
        SessionCandidature session = sessionCandidatureRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session non trouvée avec l'ID: " + sessionId));
        if (!session.isCriteresActifs()) {
            throw new BusinessException("Le filtre de critères n'est pas activé pour cette session.");
        }

        List<Candidature> soumis = candidatureRepository.findBySessionCandidatureId(sessionId).stream()
                .filter(c -> c.getStatut() == EnumStatutCandidature.SOUMIS)
                .toList();

        int preselectionnes = 0;
        int rejetes = 0;
        int dossiersVides = 0;
        int dossiersIncomplets = 0;
        for (Candidature candidature : soumis) {
            if (!CompletudeDossier.manques(candidature).isEmpty()) {
                // Dossier vide ou incomplet : exclu du filtre, reste SOUMIS pour que le candidat puisse
                // encore joindre ses pièces (TDR 3.1.A : "dossier incomplet peut revenir
                // plus tard pour le finaliser"). Le filtre ne juge que des dossiers complets : présélectionner
                // un dossier auquel il manque son diplôme, ou le rejeter alors que le candidat peut encore
                // l'ajouter, serait décider sur une partie du dossier.
                if (candidature.getPiecesJustificatives().isEmpty()) {
                    dossiersVides++;
                } else {
                    dossiersIncomplets++;
                }
                continue;
            }
            Map<Long, String> reponses = candidature.getReponsesCriteres().stream()
                    .collect(Collectors.toMap(r -> r.getCritere().getId(), ReponseCritereCandidature::getValeur));
            String resultat;
            if (session.respecteCriteres(candidature.getCandidat(), reponses)) {
                candidature.setStatut(EnumStatutCandidature.PRESELECTIONNE);
                resultat = "Critères remplis : dossier retenu pour la présélection.";
                preselectionnes++;
            } else {
                candidature.setStatut(EnumStatutCandidature.DOSSIER_REJETE);
                candidature.setMotifRejet("Critères de présélection de la session non remplis (filtre automatique).");
                resultat = "Critères non remplis : dossier rejeté par le filtre automatique.";
                rejetes++;
            }
            candidatureRepository.save(candidature);
            journalEtapeService.enregistrer(candidature, EnumEtapeQms.FILTRE_INITIAL,
                    candidature.getStatut(), auteur, resultat);
        }

        return FiltreCriteresResponse.builder()
                .total(soumis.size())
                .preselectionnes(preselectionnes)
                .rejetes(rejetes)
                .dossiersVides(dossiersVides)
                .dossiersIncomplets(dossiersIncomplets)
                .build();
    }

    // creerBrouillon() et soumettreCandidature() vivaient ici sans aucun appelant :
    // CandidatController fait tout lui-même sur /candidature/soumettre. Elles étaient
    // les seules à produire le statut BROUILLON, resté de ce fait inatteignable — d'où
    // une branche de estTransitionValide() ci-dessous que rien ne peut emprunter. Le
    // brouillon réel est géré par BrouillonCandidature, une entité distincte, ce qui
    // rendait ce code d'autant plus trompeur à la lecture.

    /**
     * Met à jour le statut d'une candidature (utilisé par l'admin)
     */
    public CandidatureResponse changerStatut(Long candidatureId, EnumStatutCandidature nouveauStatut, String motif) {
        Candidature candidature = candidatureRepository.findById(candidatureId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Candidature non trouvée avec l'ID: " + candidatureId));

        // Validation des transitions autorisées
        if (!estTransitionValide(candidature.getStatut(), nouveauStatut)) {
            throw new BusinessException("Transition de statut non autorisée: "
                    + candidature.getStatut() + " → " + nouveauStatut);
        }
        // Un dossier n'est présélectionné ou validé que complet : le jury examine un dossier qu'il a pu lire en entier.
        // Jusqu'ici seul le dossier totalement vide était refusé ; un dossier sans diplôme passait. Le rejet, lui, reste
        // possible à tout moment (candidat inéligible, dossier abandonné) : seule la décision favorable est gardée.
        if (nouveauStatut == EnumStatutCandidature.PRESELECTIONNE
                || nouveauStatut == EnumStatutCandidature.DOSSIER_VALIDE) {
            List<String> manques = CompletudeDossier.manques(candidature);
            if (!manques.isEmpty()) {
                throw new BusinessException("Dossier incomplet : " + String.join(" ; ", manques)
                        + ". Demandez un complément au candidat avant de le présélectionner.");
            }
        }

        candidature.setStatutPrecedent(candidature.getStatut());
        candidature.setStatut(nouveauStatut);
        if ((nouveauStatut == EnumStatutCandidature.DOSSIER_REJETE
                || nouveauStatut == EnumStatutCandidature.NON_ADMIS) && (motif == null || motif.isBlank())) {
            throw new BusinessException("Un motif est obligatoire pour refuser une candidature.");
        }
        if (nouveauStatut == EnumStatutCandidature.DOSSIER_REJETE
                || nouveauStatut == EnumStatutCandidature.NON_ADMIS) {
            candidature.setMotifRejet(motif);
        }

        Candidature saved = candidatureRepository.save(candidature);
        return toResponse(saved);
    }

    /**
     * Demande au candidat de compléter un dossier soumis auquel il manque des pièces (SOUMIS → INCOMPLET).
     *
     * <p>C'est le geste qui remplace la présélection d'un dossier incomplet : le dossier sort de la pile « à examiner »
     * et attend le candidat. Il y revient seul dès que la dernière pièce est fournie (voir
     * {@code CandidatController#televerserPiece}). Refusé pour un dossier complet : une pièce illisible ou non
     * conforme se traite pièce par pièce, par « demander le remplacement ».
     */
    public CandidatureResponse demanderComplement(Long candidatureId) {
        Candidature candidature = candidatureRepository.findById(candidatureId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature non trouvée avec l'ID: " + candidatureId));
        if (candidature.getStatut() != EnumStatutCandidature.SOUMIS) {
            throw new BusinessException("Un complément ne se demande que pour un dossier soumis, en attente d'examen.");
        }
        if (CompletudeDossier.manques(candidature).isEmpty()) {
            throw new BusinessException("Ce dossier est complet : aucun document à réclamer. Pour une pièce illisible "
                    + "ou non conforme, demandez son remplacement depuis la liste des pièces.");
        }
        return changerStatut(candidatureId, EnumStatutCandidature.INCOMPLET, null);
    }

    /**
     * Enregistre l'issue du test académique d'un candidat convoqué.
     *
     * <p>Aucune route ne posait jamais TEST_REUSSI / TEST_ECHOUE / ABSENT_TEST : un dossier convoqué au test ne
     * pouvait ni avancer ni être refusé (la table des transitions les exigeait, personne ne les écrivait). C'est un
     * geste du responsable, et non un effet de la saisie des notes : la moyenne aide à décider, le jury décide.
     *
     * @param resultat {@code REUSSI}, {@code ECHOUE} ou {@code ABSENT}
     */
    public CandidatureResponse enregistrerResultatTest(Long candidatureId, String resultat) {
        EnumStatutCandidature cible = switch (normaliserResultat(resultat)) {
            case "REUSSI" -> EnumStatutCandidature.TEST_REUSSI;
            case "ECHOUE" -> EnumStatutCandidature.TEST_ECHOUE;
            case "ABSENT" -> EnumStatutCandidature.ABSENT_TEST;
            default -> throw new BusinessException(
                    "Résultat de test inconnu : « " + resultat + " » (attendu : REUSSI, ECHOUE ou ABSENT).");
        };
        return changerStatut(candidatureId, cible, null);
    }

    /**
     * Enregistre l'issue de l'entretien d'un candidat convoqué : effectué (le jury délibère ensuite) ou absent.
     *
     * @param resultat {@code EFFECTUE} ou {@code ABSENT}
     */
    public CandidatureResponse enregistrerResultatEntretien(Long candidatureId, String resultat) {
        EnumStatutCandidature cible = switch (normaliserResultat(resultat)) {
            case "EFFECTUE" -> EnumStatutCandidature.ENTRETIEN_EFFECTUE;
            case "ABSENT" -> EnumStatutCandidature.ABSENT_ENTRETIEN;
            default -> throw new BusinessException(
                    "Résultat d'entretien inconnu : « " + resultat + " » (attendu : EFFECTUE ou ABSENT).");
        };
        return changerStatut(candidatureId, cible, null);
    }

    private static String normaliserResultat(String resultat) {
        return resultat == null ? "" : resultat.trim().toUpperCase(java.util.Locale.ROOT);
    }

    /**
     * Annule la dernière décision prise sur cette candidature (retour au statut
     * précédent). Un seul niveau — pas d'historique complet : annuler une deuxième
     * fois sans nouvelle décision entre-temps n'a plus d'effet, ce qui correspond au
     * comportement attendu d'un « annuler ma dernière action ».
     */
    public CandidatureResponse annulerDerniereAction(Long candidatureId) {
        Candidature candidature = candidatureRepository.findById(candidatureId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Candidature non trouvée avec l'ID: " + candidatureId));

        if (candidature.getStatut() == EnumStatutCandidature.INSCRIPTION_FINALISEE) {
            throw new BusinessException(
                    "L'admission finale a déjà ouvert l'accès étudiant du candidat : ce statut ne peut plus être annulé automatiquement.");
        }
        if (candidature.getStatutPrecedent() == null) {
            throw new BusinessException("Aucune décision récente à annuler pour ce dossier.");
        }

        candidature.setStatut(candidature.getStatutPrecedent());
        candidature.setStatutPrecedent(null);
        if (candidature.getStatut() != EnumStatutCandidature.DOSSIER_REJETE
                && candidature.getStatut() != EnumStatutCandidature.NON_ADMIS) {
            candidature.setMotifRejet(null);
        }

        return toResponse(candidatureRepository.save(candidature));
    }

    /**
     * Fait reculer explicitement une candidature vers une étape antérieure du
     * parcours, à tout moment (pas seulement juste après la décision à défaire).
     * Contrairement à {@link #annulerDerniereAction}, ne dépend d'aucun historique :
     * la cible doit juste être strictement antérieure au statut actuel dans l'ordre
     * du parcours, ou le statut actuel doit être REJETE (« dé-rejeter »).
     *
     * <p>BROUILLON est exclu des cibles : voir le commentaire plus haut sur
     * estTransitionValide, ce statut n'est jamais atteint par une vraie
     * {@link Candidature} (le brouillon réel est {@link inpg.sn.lms.recrutement.entity.BrouillonCandidature}).
     * ADMIS3 et REJETE sont exclus des cibles : ce sont des décisions dédiées
     * (admission finale, rejet motivé) avec leurs propres effets de bord.
     */
    public CandidatureResponse reculerStatut(Long candidatureId, EnumStatutCandidature cible) {
        Candidature candidature = candidatureRepository.findById(candidatureId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Candidature non trouvée avec l'ID: " + candidatureId));

        if (rangParcours(cible) == null) {
            throw new BusinessException(
                    "Impossible de reculer vers " + cible + " — utiliser l'action dédiée pour ce statut.");
        }
        EnumStatutCandidature actuel = candidature.getStatut();
        if (actuel == EnumStatutCandidature.INSCRIPTION_FINALISEE) {
            throw new BusinessException(
                    "L'admission finale a déjà ouvert l'accès étudiant du candidat : son statut ne peut plus être modifié automatiquement.");
        }
        if (!peutReculerVers(actuel, cible)) {
            throw new BusinessException("On ne peut reculer que vers un statut antérieur dans le parcours.");
        }

        candidature.setStatut(cible);
        candidature.setStatutPrecedent(null);
        candidature.setMotifRejet(null);

        return toResponse(candidatureRepository.save(candidature));
    }

    /**
     * La règle du recul, écrite une seule fois : {@link #reculerStatut} l'applique, {@link #reculsPossibles} la
     * propose aux écrans.
     *
     * <p>REJETE, NON_ADMIS et LISTE_ATTENTE sortent du parcours linéaire : on autorise à les défaire vers n'importe
     * quelle étape (« dé-rejeter », « dé-lister »), au-delà de la fenêtre d'annulation immédiate qu'offre
     * {@link #annulerDerniereAction}. Un dossier dont l'inscription est finalisée ne recule plus : l'accès étudiant
     * est ouvert.
     */
    private boolean peutReculerVers(EnumStatutCandidature actuel, EnumStatutCandidature cible) {
        if (cible == actuel || rangParcours(cible) == null || actuel == EnumStatutCandidature.INSCRIPTION_FINALISEE) {
            return false;
        }
        if (actuel == EnumStatutCandidature.DOSSIER_REJETE
                || actuel == EnumStatutCandidature.NON_ADMIS
                || actuel == EnumStatutCandidature.LISTE_ATTENTE) {
            return true;
        }
        Integer rangActuel = rangParcours(actuel);
        return rangActuel != null && rangParcours(cible) < rangActuel;
    }

    /**
     * Les statuts vers lesquels l'admission peut ramener ce dossier. {@code DOSSIER_VALIDE}, statut intermédiaire
     * qu'aucune action ne pose, n'est pas proposé : il ne mènerait nulle part que le candidat comprenne.
     */
    public List<EnumStatutCandidature> reculsPossibles(Candidature c) {
        return Arrays.stream(EnumStatutCandidature.values())
                .filter(cible -> cible != EnumStatutCandidature.DOSSIER_VALIDE)
                .filter(cible -> peutReculerVers(c.getStatut(), cible))
                .toList();
    }

    /**
     * Ce que l'admission peut faire de ce dossier maintenant — une seule source pour tous les écrans.
     *
     * <p>Les écrans recopiaient chacun la table des transitions avec l'ancien vocabulaire (ADMIS1/2/3,
     * EN_COURS_DE_PRESELECTION) : à partir de la présélection, aucun bouton ne s'affichait plus. Le serveur, qui
     * décide de toute façon de ce qui est permis ({@link #estTransitionValide}), le dit désormais lui-même.
     *
     * <p>Un dossier soumis auquel il manque un document n'offre pas la présélection mais « demander un complément ».
     * Le rejet reste proposé tant que la transition l'autorise.
     */
    public List<String> actionsPossibles(Candidature c) {
        List<String> actions = new ArrayList<>();
        EnumStatutCandidature statut = c.getStatut();
        boolean complet = CompletudeDossier.manques(c).isEmpty();
        switch (statut) {
            case SOUMIS -> actions.add(complet ? "PRESELECTIONNER" : "DEMANDER_COMPLEMENT");
            case DOSSIER_VALIDE -> {
                if (complet) actions.add("PRESELECTIONNER");
            }
            case PRESELECTIONNE, PAIEMENT_EN_ATTENTE -> actions.add("ENREGISTRER_REGLEMENT");
            case PAIEMENT_CONFIRME, ABSENT_TEST -> actions.add("CONVOQUER_TEST");
            case CONVOQUE_TEST -> actions.add("ENREGISTRER_RESULTAT_TEST");
            case TEST_REUSSI, ABSENT_ENTRETIEN -> actions.add("ADMETTRE_ENTRETIEN");
            case CONVOQUE_ENTRETIEN -> actions.add("ENREGISTRER_RESULTAT_ENTRETIEN");
            case ENTRETIEN_EFFECTUE -> {
                actions.add("ADMETTRE_FINAL");
                actions.add("PLACER_LISTE_ATTENTE");
            }
            case LISTE_ATTENTE -> actions.add("ADMETTRE_FINAL");
            case ADMIS -> actions.add("FINALISER_INSCRIPTION");
            default -> { }
        }
        if (estTransitionValide(statut, EnumStatutCandidature.DOSSIER_REJETE)
                || estTransitionValide(statut, EnumStatutCandidature.NON_ADMIS)) {
            actions.add("REJETER");
        }
        return actions;
    }

    /**
     * Position dans le parcours normal, {@code null} pour un statut qui n'y a pas sa
     * place comme cible de recul.
     *
     * <p>LISTE_ATTENTE est exclu des cibles : y placer un candidat est une décision de
     * jury, pas un « retour en arrière » — elle passe par l'action dédiée.
     */
    private Integer rangParcours(EnumStatutCandidature statut) {
        return switch (statut) {
            case SOUMIS -> 1;
            case INCOMPLET -> 2;
            case DOSSIER_VALIDE -> 3;
            case PRESELECTIONNE -> 4;
            case PAIEMENT_EN_ATTENTE -> 5;
            case PAIEMENT_CONFIRME -> 6;
            case CONVOQUE_TEST -> 7;
            case TEST_REUSSI, TEST_ECHOUE, ABSENT_TEST -> 8;
            case CONVOQUE_ENTRETIEN -> 9;
            case ENTRETIEN_EFFECTUE, ABSENT_ENTRETIEN -> 10;
            case BROUILLON, DOSSIER_REJETE, LISTE_ATTENTE, ADMIS, NON_ADMIS,
                    INSCRIPTION_FINALISEE, DESISTEMENT -> null;
        };
    }

    /**
     * Récupère toutes les candidatures d'un candidat, de la plus ancienne à la plus récente.
     *
     * <p>Le tri est explicite : la requête n'en porte aucun, et le suivi du candidat lit la <em>dernière</em>
     * entrée comme « son dossier actuel ». Sans cela, l'ordre de retour de la base décidait de l'écran.
     */
    public List<CandidatureResponse> getCandidaturesByCandidatId(Long candidatId) {
        return candidatureRepository.findByCandidatId(candidatId)
                .stream()
                .sorted(java.util.Comparator.comparing(Candidature::getId,
                        java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder())))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère toutes les candidatures d'une session
     */
    public List<CandidatureResponse> getCandidaturesBySessionId(Long sessionId) {
        return candidatureRepository.findBySessionCandidatureId(sessionId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère toutes les candidatures par statut
     */
    public List<CandidatureResponse> getCandidaturesByStatut(EnumStatutCandidature statut) {
        return candidatureRepository.findByStatut(statut)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère une candidature par son ID
     */
    public CandidatureResponse getCandidatureById(Long id) {
        Candidature candidature = candidatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature non trouvée avec l'ID: " + id));
        return toResponse(candidature);
    }

    /**
     * Récupère une candidature par numéro de dossier
     */
    public CandidatureResponse getCandidatureByNumeroDossier(String numeroDossier) {
        Candidature candidature = candidatureRepository.findByNumeroDossier(numeroDossier)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Candidature non trouvée avec le numéro: " + numeroDossier));
        return toResponse(candidature);
    }

    /**
     * Get toutes les candidatures
     */
    public List<CandidatureResponse> getAllCandidatures() {
        return candidatureRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Page de dossiers triée par dépôt récent pour les tableaux d'administration. */
    @Transactional(readOnly = true)
    public Page<CandidatureResponse> getCandidaturesPage(int page, int taille) {
        int pageSecurisee = Math.max(0, page);
        int tailleSecurisee = Math.min(100, Math.max(1, taille));
        return candidatureRepository.findAllBy(PageRequest.of(pageSecurisee, tailleSecurisee,
                Sort.by(Sort.Direction.DESC, "dateSoumission")))
                .map(this::toResponse);
    }

    private static final List<EnumStatutCandidature> ETAPES_WORKFLOW = List.of(
            EnumStatutCandidature.SOUMIS,
            EnumStatutCandidature.PRESELECTIONNE,
            EnumStatutCandidature.PAIEMENT_EN_ATTENTE,
            EnumStatutCandidature.PAIEMENT_CONFIRME,
            EnumStatutCandidature.CONVOQUE_TEST,
            EnumStatutCandidature.TEST_REUSSI,
            EnumStatutCandidature.CONVOQUE_ENTRETIEN,
            EnumStatutCandidature.ENTRETIEN_EFFECTUE,
            EnumStatutCandidature.ADMIS,
            EnumStatutCandidature.LISTE_ATTENTE,
            EnumStatutCandidature.NON_ADMIS);

    /** Libellé lisible d'un statut, dans l'ordre du parcours : exports et rapports ne montrent plus de nom technique. */
    public static final Map<EnumStatutCandidature, String> LIBELLES_STATUTS;
    static {
        Map<EnumStatutCandidature, String> m = new java.util.LinkedHashMap<>();
        m.put(EnumStatutCandidature.BROUILLON, "Brouillon");
        m.put(EnumStatutCandidature.SOUMIS, "Soumis");
        m.put(EnumStatutCandidature.INCOMPLET, "À compléter");
        m.put(EnumStatutCandidature.DOSSIER_VALIDE, "Dossier validé");
        m.put(EnumStatutCandidature.PRESELECTIONNE, "Présélectionné");
        m.put(EnumStatutCandidature.PAIEMENT_EN_ATTENTE, "Paiement en attente");
        m.put(EnumStatutCandidature.PAIEMENT_CONFIRME, "Paiement confirmé");
        m.put(EnumStatutCandidature.CONVOQUE_TEST, "Convoqué au test");
        m.put(EnumStatutCandidature.TEST_REUSSI, "Test réussi");
        m.put(EnumStatutCandidature.TEST_ECHOUE, "Test échoué");
        m.put(EnumStatutCandidature.ABSENT_TEST, "Absent au test");
        m.put(EnumStatutCandidature.CONVOQUE_ENTRETIEN, "Convoqué à l'entretien");
        m.put(EnumStatutCandidature.ENTRETIEN_EFFECTUE, "Entretien effectué");
        m.put(EnumStatutCandidature.ABSENT_ENTRETIEN, "Absent à l'entretien");
        m.put(EnumStatutCandidature.ADMIS, "Admis");
        m.put(EnumStatutCandidature.LISTE_ATTENTE, "Liste complémentaire");
        m.put(EnumStatutCandidature.INSCRIPTION_FINALISEE, "Inscription finalisée");
        m.put(EnumStatutCandidature.DOSSIER_REJETE, "Dossier rejeté");
        m.put(EnumStatutCandidature.NON_ADMIS, "Non admis");
        m.put(EnumStatutCandidature.DESISTEMENT, "Désistement");
        LIBELLES_STATUTS = java.util.Collections.unmodifiableMap(m);
    }

    private static final Map<EnumStatutCandidature, String> LIBELLES_ETAPES = Map.ofEntries(
            Map.entry(EnumStatutCandidature.SOUMIS, "Soumis"),
            Map.entry(EnumStatutCandidature.PRESELECTIONNE, "Présélectionné"),
            Map.entry(EnumStatutCandidature.PAIEMENT_EN_ATTENTE, "Paiement en attente"),
            Map.entry(EnumStatutCandidature.PAIEMENT_CONFIRME, "Paiement confirmé"),
            Map.entry(EnumStatutCandidature.CONVOQUE_TEST, "Convoqué au test"),
            Map.entry(EnumStatutCandidature.TEST_REUSSI, "Test réussi"),
            Map.entry(EnumStatutCandidature.CONVOQUE_ENTRETIEN, "Convoqué à l'entretien"),
            Map.entry(EnumStatutCandidature.ENTRETIEN_EFFECTUE, "Entretien effectué"),
            Map.entry(EnumStatutCandidature.ADMIS, "Admis"),
            Map.entry(EnumStatutCandidature.LISTE_ATTENTE, "Liste d'attente"),
            Map.entry(EnumStatutCandidature.NON_ADMIS, "Non admis"));

    /**
     * Rapport qualité recrutement (demande responsable admission) : répartition des
     * candidatures par étape du workflow, avec la liste nominative de chaque étape.
     * Ne recalcule rien : s'appuie sur getAllCandidatures()/toResponse() déjà en place.
     */
    public RapportRecrutementDto genererRapportParEtape() {
        List<CandidatureResponse> toutes = getAllCandidatures();

        List<RapportRecrutementDto.EtapeRapport> etapes = ETAPES_WORKFLOW.stream()
                .map(statut -> {
                    List<RapportRecrutementDto.CandidatRapport> candidats = toutes.stream()
                            .filter(c -> c.getStatut() == statut)
                            .sorted((a, b) -> {
                                LocalDateTime da = a.getDateSoumission();
                                LocalDateTime db = b.getDateSoumission();
                                if (da == null || db == null) return 0;
                                return da.compareTo(db);
                            })
                            .map(c -> new RapportRecrutementDto.CandidatRapport(
                                    c.getNumeroDossier(),
                                    c.getCandidatNom(),
                                    c.getCandidatPrenom(),
                                    c.getCandidatEmail(),
                                    c.getSessionCandidatureLibelle(),
                                    c.getDateSoumission(),
                                    statut == EnumStatutCandidature.NON_ADMIS ? c.getMotifRejet() : null))
                            .toList();
                    return new RapportRecrutementDto.EtapeRapport(
                            statut.name(), LIBELLES_ETAPES.get(statut), candidats.size(), candidats);
                })
                .toList();

        return new RapportRecrutementDto(toutes.size(), etapes);
    }

    // --- Méthodes privées ---

    private String genererNumeroDossier() {
        return "INPG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private boolean estTransitionValide(EnumStatutCandidature actuel, EnumStatutCandidature nouveau) {
        return switch (actuel) {
            case BROUILLON -> nouveau == EnumStatutCandidature.SOUMIS || nouveau == EnumStatutCandidature.BROUILLON;
            case SOUMIS -> nouveau == EnumStatutCandidature.INCOMPLET
                    || nouveau == EnumStatutCandidature.DOSSIER_VALIDE
                    || nouveau == EnumStatutCandidature.PRESELECTIONNE
                    || nouveau == EnumStatutCandidature.DOSSIER_REJETE;
            case INCOMPLET -> nouveau == EnumStatutCandidature.SOUMIS
                    || nouveau == EnumStatutCandidature.DOSSIER_REJETE;
            case DOSSIER_VALIDE -> nouveau == EnumStatutCandidature.PRESELECTIONNE
                    || nouveau == EnumStatutCandidature.DOSSIER_REJETE;
            case PRESELECTIONNE -> nouveau == EnumStatutCandidature.PAIEMENT_EN_ATTENTE
                    || nouveau == EnumStatutCandidature.PAIEMENT_CONFIRME
                    || nouveau == EnumStatutCandidature.DOSSIER_REJETE;
            case PAIEMENT_EN_ATTENTE -> nouveau == EnumStatutCandidature.PAIEMENT_CONFIRME
                    || nouveau == EnumStatutCandidature.PRESELECTIONNE;
            case PAIEMENT_CONFIRME -> nouveau == EnumStatutCandidature.CONVOQUE_TEST;
            // À l'issue de l'entretien : admis, placé en liste complémentaire, ou rejeté.
            case CONVOQUE_TEST -> nouveau == EnumStatutCandidature.TEST_REUSSI
                    || nouveau == EnumStatutCandidature.TEST_ECHOUE
                    || nouveau == EnumStatutCandidature.ABSENT_TEST;
            case ABSENT_TEST -> nouveau == EnumStatutCandidature.CONVOQUE_TEST
                    || nouveau == EnumStatutCandidature.NON_ADMIS;
            case TEST_REUSSI -> nouveau == EnumStatutCandidature.CONVOQUE_ENTRETIEN
                    || nouveau == EnumStatutCandidature.NON_ADMIS;
            case TEST_ECHOUE -> nouveau == EnumStatutCandidature.NON_ADMIS;
            case CONVOQUE_ENTRETIEN -> nouveau == EnumStatutCandidature.ENTRETIEN_EFFECTUE
                    || nouveau == EnumStatutCandidature.ABSENT_ENTRETIEN;
            case ABSENT_ENTRETIEN -> nouveau == EnumStatutCandidature.CONVOQUE_ENTRETIEN
                    || nouveau == EnumStatutCandidature.NON_ADMIS;
            case ENTRETIEN_EFFECTUE -> nouveau == EnumStatutCandidature.ADMIS
                    || nouveau == EnumStatutCandidature.LISTE_ATTENTE
                    || nouveau == EnumStatutCandidature.NON_ADMIS;
            // Liste complémentaire : on n'en sort que si une place se libère (ADMIS3)
            // ou si la campagne se solde sans place (REJETE).
            case LISTE_ATTENTE -> nouveau == EnumStatutCandidature.ADMIS
                    || nouveau == EnumStatutCandidature.NON_ADMIS;
            case ADMIS -> nouveau == EnumStatutCandidature.INSCRIPTION_FINALISEE
                    || nouveau == EnumStatutCandidature.DESISTEMENT;
            case DOSSIER_REJETE, NON_ADMIS, INSCRIPTION_FINALISEE, DESISTEMENT -> false;
        };
    }

    private CandidatureResponse toResponse(Candidature c) {
        List<PieceJustificativeResponse> pieces = c.getPiecesJustificatives().stream()
                .map(p -> PieceJustificativeResponse.builder()
                        .id(p.getId())
                        .typeDocument(p.getTypeDocument())
                        .estConforme(p.isEstConforme())
                        .statutVerification(p.getStatutVerification())
                        .motifNonConformite(p.getMotifNonConformite())
                        .dateVerification(p.getDateVerification())
                        .description(p.getDescription())
                        .fichierStockeId(p.getFichierStocke() != null ? p.getFichierStocke().getId() : null)
                        .fichierNomOriginal(
                                p.getFichierStocke() != null ? p.getFichierStocke().getNomOriginal() : null)
                        .moyenneDeclaree(p.getMoyenneDeclaree())
                        .build())
                .collect(Collectors.toList());

        PaiementResponse paiementResponse = null;
        if (c.getPaiement() != null) {
            paiementResponse = PaiementResponse.builder()
                    .id(c.getPaiement().getId())
                    .referenceTransaction(c.getPaiement().getReferenceTransaction())
                    .montant(c.getPaiement().getMontant())
                    .devise(c.getPaiement().getDevise())
                    .statut(c.getPaiement().getStatut())
                    .datePaiement(c.getPaiement().getDatePaiement())
                    .methodePaiement(c.getPaiement().getMethodePaiement())
                    .build();
        }

        return CandidatureResponse.builder()
                .id(c.getId())
                .numeroDossier(c.getNumeroDossier())
                .statut(c.getStatut())
                .statutPrecedent(c.getStatutPrecedent())
                .dateSoumission(c.getDateSoumission())
                .motifRejet(c.getMotifRejet())
                .rangListeAttente(c.getRangListeAttente())
                .candidatId(c.getCandidat().getId())
                .candidatNom(c.getCandidat().getNom())
                .candidatPrenom(c.getCandidat().getPrenom())
                .candidatEmail(c.getCandidat().getUser() != null ? c.getCandidat().getUser().getEmail() : null)
                .candidatGenre(c.getCandidat().getGenre())
                .candidatProfession(c.getCandidat().getProfession())
                .candidatAnneesExperience(c.getCandidat().getAnneesExperience())
                .candidatDateNaissance(c.getCandidat().getDateNaissance())
                .candidatNiveauEtude(c.getCandidat().getNiveauEtude())
                .candidatNationalite(c.getCandidat().getNationalite())
                .candidatTelephone(c.getCandidat().getTelephone())
                .candidatAdresse(c.getCandidat().getAdresse())
                .consentementLe(c.getConsentementLe())
                .sessionCandidatureId(c.getSessionCandidature().getId())
                .sessionCandidatureLibelle(c.getSessionCandidature().getLibelle())
                .sessionInfoFraisScolarite(c.getSessionCandidature().getInfoFraisScolarite())
                .sessionInfoCarteEtudiante(c.getSessionCandidature().getInfoCarteEtudiante())
                .sessionInfoRentree(c.getSessionCandidature().getInfoRentree())
                .informationsAcademiques(c.getInformationsAcademiques())
                .informationsProfessionnelles(c.getInformationsProfessionnelles())
                .piecesJustificatives(pieces)
                .documentsAttendus(CompletudeDossier.documentsAttendus(c))
                .paiement(paiementResponse)
                .actionsPossibles(actionsPossibles(c))
                .reculsPossibles(reculsPossibles(c))
                .reponsesCriteres(c.getReponsesCriteres().stream()
                        .map(r -> ReponseCritereResponse.builder()
                                .critereId(r.getCritere().getId())
                                .libelle(r.getCritere().getLibelle())
                                .valeur(r.getValeur())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
