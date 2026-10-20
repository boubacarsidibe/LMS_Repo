package inpg.sn.lms.recrutement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.exception.BusinessException;
import inpg.sn.lms.exception.DuplicateResourceException;
import inpg.sn.lms.recrutement.dto.PaiementRequest;
import inpg.sn.lms.recrutement.dto.PaiementResponse;
import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.Paiement;
import inpg.sn.lms.recrutement.enums.EnumStatutCandidature;
import inpg.sn.lms.recrutement.enums.EnumStatutPaiement;
import inpg.sn.lms.recrutement.repository.CandidatureRepository;
import inpg.sn.lms.recrutement.repository.PaiementRepository;

/**
 * Cycle de vie d'un paiement de frais de dossier.
 *
 * <p>Le règlement reste simulé — aucun agrégateur n'est branché — mais le workflow doit
 * être celui d'un paiement réel, sans quoi le brancher plus tard imposerait de reprendre
 * tout le parcours. Trois propriétés sont figées ici :
 *
 * <ul>
 *   <li>l'initiation et la confirmation sont deux étapes distinctes, la seconde étant le
 *       point d'entrée du futur webhook ;</li>
 *   <li>la confirmation est <strong>idempotente</strong> : une passerelle réémet ses
 *       notifications jusqu'à obtenir un accusé, et rejouer ne doit pas relancer les
 *       courriels ni réencaisser ;</li>
 *   <li>une tentative échouée peut être <strong>rejouée</strong> : la règle précédente
 *       enfermait le candidat dans une impasse définitive.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class PaiementServiceWorkflowTest {

    private static final long ID_CANDIDATURE = 11L;
    private static final long ID_PAIEMENT = 1L;

    @Mock private PaiementRepository paiementRepository;
    @Mock private CandidatureRepository candidatureRepository;
    @Mock private NotificationService notificationService;
    @Mock private NotificationCandidatService notificationCandidatService;
    @Mock private RecuPaiementService recuPaiementService;

    @InjectMocks private PaiementService paiementService;

    private Candidature candidature;
    private PaiementRequest requete;

    @BeforeEach
    void preparer() {
        ReflectionTestUtils.setField(paiementService, "fraisDossier", "25000");
        ReflectionTestUtils.setField(paiementService, "deviseFraisDossier", "XOF");
        ReflectionTestUtils.setField(paiementService, "emailAdmission", "admin@inpg.sn");

        User user = new User();
        user.setId(46L);
        user.setEmail("candidat@example.sn");
        Candidat candidat = new Candidat();
        candidat.setId(46L);
        candidat.setUser(user);

        candidature = new Candidature();
        candidature.setId(ID_CANDIDATURE);
        candidature.setNumeroDossier("INPG-TEST0002");
        candidature.setCandidat(candidat);
        // Le règlement n'est ouvert qu'une fois le dossier présélectionné (voir le test de garde ci-dessous).
        candidature.setStatut(EnumStatutCandidature.PRESELECTIONNE);

        requete = new PaiementRequest();
        requete.setCandidatureId(ID_CANDIDATURE);
        requete.setMethodePaiement("Wave");

        lenient().when(paiementRepository.save(any(Paiement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Paiement paiement(EnumStatutPaiement statut) {
        Paiement p = new Paiement();
        p.setId(ID_PAIEMENT);
        p.setReferenceTransaction("PAY-INITIALE");
        p.setStatut(statut);
        p.setCandidature(candidature);
        return p;
    }

    // ===================== Initiation =====================

    /**
     * Le candidat doit connaître le montant AVANT de déclarer son règlement : la page affichait « Montant communiqué à
     * la validation ». Le montant annoncé doit être celui qui sera facturé — celui de la session, sinon le tarif de repli.
     */
    @Test
    void les_frais_annonces_sont_ceux_de_la_session_sinon_le_tarif_de_repli() {
        when(candidatureRepository.findById(ID_CANDIDATURE)).thenReturn(Optional.of(candidature));

        PaiementService.FraisDossier parDefaut = paiementService.fraisDossierPour(ID_CANDIDATURE);
        assertEquals(0, parDefaut.montant().compareTo(new java.math.BigDecimal("25000")));
        assertEquals("XOF", parDefaut.devise());

        inpg.sn.lms.recrutement.entity.SessionCandidature session = new inpg.sn.lms.recrutement.entity.SessionCandidature();
        session.setMontantFraisDossier(new java.math.BigDecimal("40000"));
        candidature.setSessionCandidature(session);
        assertEquals(0, paiementService.fraisDossierPour(ID_CANDIDATURE).montant().compareTo(new java.math.BigDecimal("40000")));
    }

    @Test
    void le_montant_annonce_est_celui_qui_sera_enregistre() {
        when(candidatureRepository.findById(ID_CANDIDATURE)).thenReturn(Optional.of(candidature));
        when(candidatureRepository.findByIdForPayment(ID_CANDIDATURE)).thenReturn(Optional.of(candidature));
        when(paiementRepository.findByCandidatureId(ID_CANDIDATURE)).thenReturn(Optional.empty());

        PaiementService.FraisDossier annonce = paiementService.fraisDossierPour(ID_CANDIDATURE);
        PaiementResponse enregistre = paiementService.initierPaiement(requete);

        assertEquals(0, annonce.montant().compareTo(enregistre.getMontant()));
        assertEquals(annonce.devise(), enregistre.getDevise());
    }

    /** Le tarif est une donnée métier : il ne doit jamais provenir de la requête. */
    @Test
    void le_montant_vient_de_la_configuration_pas_du_client() {
        when(candidatureRepository.findByIdForPayment(ID_CANDIDATURE)).thenReturn(Optional.of(candidature));
        when(paiementRepository.findByCandidatureId(ID_CANDIDATURE)).thenReturn(Optional.empty());

        PaiementResponse reponse = paiementService.initierPaiement(requete);

        assertEquals(0, reponse.getMontant().compareTo(new java.math.BigDecimal("25000")));
        assertEquals("XOF", reponse.getDevise());
        // Une tentative naît toujours en attente : la confirmation est une étape séparée.
        assertEquals(EnumStatutPaiement.EN_ATTENTE, reponse.getStatut());
    }

    /**
     * Le paiement n'a pas de sens avant la présélection : un dossier simplement soumis ou
     * en cours d'examen n'a pas à régler de frais, et l'accepter ferait sauter le statut
     * vers PAIEMENT_EN_ATTENTE en court-circuitant l'examen du dossier par les admissions.
     */
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = EnumStatutCandidature.class,
            names = {"SOUMIS", "INCOMPLET", "DOSSIER_REJETE", "ADMIS"})
    void le_paiement_est_refuse_avant_la_preselection(EnumStatutCandidature statut) {
        candidature.setStatut(statut);
        when(candidatureRepository.findByIdForPayment(ID_CANDIDATURE)).thenReturn(Optional.of(candidature));

        assertThrows(BusinessException.class, () -> paiementService.initierPaiement(requete));
        verify(paiementRepository, never()).save(any(Paiement.class));
        verify(candidatureRepository, never()).save(any(Candidature.class));
    }

    @Test
    void un_dossier_deja_regle_ne_peut_pas_etre_repaye() {
        when(candidatureRepository.findByIdForPayment(ID_CANDIDATURE)).thenReturn(Optional.of(candidature));
        when(paiementRepository.findByCandidatureId(ID_CANDIDATURE))
                .thenReturn(Optional.of(paiement(EnumStatutPaiement.SUCCES)));

        assertThrows(DuplicateResourceException.class, () -> paiementService.initierPaiement(requete));
    }

    /**
     * La règle précédente refusait toute candidature possédant un paiement, quel que soit
     * son statut : un candidat dont la transaction avait échoué ne pouvait plus jamais
     * régler ses frais.
     */
    @Test
    void une_tentative_en_echec_peut_etre_rejouee() {
        Paiement echoue = paiement(EnumStatutPaiement.ECHEC);
        when(candidatureRepository.findByIdForPayment(ID_CANDIDATURE)).thenReturn(Optional.of(candidature));
        when(paiementRepository.findByCandidatureId(ID_CANDIDATURE)).thenReturn(Optional.of(echoue));

        PaiementResponse reponse = paiementService.initierPaiement(requete);

        assertEquals(EnumStatutPaiement.EN_ATTENTE, reponse.getStatut());
        assertNull(reponse.getDatePaiement());
        // Référence renouvelée : l'ancienne tentative peut encore aboutir chez l'agrégateur.
        assertNotEquals("PAY-INITIALE", reponse.getReferenceTransaction());
    }

    @Test
    void une_tentative_restee_en_attente_peut_etre_relancee() {
        when(candidatureRepository.findByIdForPayment(ID_CANDIDATURE)).thenReturn(Optional.of(candidature));
        when(paiementRepository.findByCandidatureId(ID_CANDIDATURE))
                .thenReturn(Optional.of(paiement(EnumStatutPaiement.EN_ATTENTE)));

        assertEquals(EnumStatutPaiement.EN_ATTENTE, paiementService.initierPaiement(requete).getStatut());
    }

    // ===================== Confirmation =====================

    @Test
    void la_confirmation_valide_le_paiement_et_previent_le_candidat() {
        when(paiementRepository.findById(ID_PAIEMENT))
                .thenReturn(Optional.of(paiement(EnumStatutPaiement.EN_ATTENTE)));

        PaiementResponse reponse = paiementService.confirmerPaiement(ID_PAIEMENT);

        assertEquals(EnumStatutPaiement.SUCCES, reponse.getStatut());
        verify(notificationService).notifierPaiementSucces("candidat@example.sn", "INPG-TEST0002");
    }

    /**
     * Une passerelle réémet sa notification jusqu'à obtenir un accusé de réception.
     * Rejouer la confirmation ne doit ni changer l'état, ni renvoyer les courriels.
     */
    @Test
    void la_confirmation_est_idempotente() {
        Paiement deja = paiement(EnumStatutPaiement.SUCCES);
        when(paiementRepository.findById(ID_PAIEMENT)).thenReturn(Optional.of(deja));

        PaiementResponse reponse = paiementService.confirmerPaiement(ID_PAIEMENT);

        assertEquals(EnumStatutPaiement.SUCCES, reponse.getStatut());
        verify(notificationService, never()).notifierPaiementSucces(anyString(), anyString());
        verify(notificationCandidatService, never()).creerNotification(anyLong(), anyString(), anyString(), anyString());
        verify(paiementRepository, never()).save(any());
    }

    @Test
    void un_paiement_en_echec_ne_peut_pas_etre_confirme_sans_nouvelle_tentative() {
        when(paiementRepository.findById(ID_PAIEMENT))
                .thenReturn(Optional.of(paiement(EnumStatutPaiement.ECHEC)));

        assertThrows(BusinessException.class, () -> paiementService.confirmerPaiement(ID_PAIEMENT));
    }

    // ===================== Échec =====================

    @Test
    void l_echec_est_enregistre_et_notifie_au_candidat() {
        when(paiementRepository.findById(ID_PAIEMENT))
                .thenReturn(Optional.of(paiement(EnumStatutPaiement.EN_ATTENTE)));

        PaiementResponse reponse = paiementService.echouerPaiement(ID_PAIEMENT, "Solde insuffisant");

        assertEquals(EnumStatutPaiement.ECHEC, reponse.getStatut());
        verify(notificationCandidatService, times(1))
                .creerNotification(anyLong(), anyString(), anyString(), anyString());
    }

    /** Un encaissement constaté ne se dédit pas : seul un remboursement le ferait. */
    @Test
    void un_paiement_valide_ne_peut_pas_etre_marque_en_echec() {
        when(paiementRepository.findById(ID_PAIEMENT))
                .thenReturn(Optional.of(paiement(EnumStatutPaiement.SUCCES)));

        assertThrows(BusinessException.class,
                () -> paiementService.echouerPaiement(ID_PAIEMENT, "erreur de saisie"));
    }

    // ===================== Enregistrement hors ligne =====================

    /**
     * Un candidat n'ayant jamais initié de tentative en ligne n'a aucun {@link Paiement}
     * à confirmer — {@link PaiementService#confirmerPaiement} ne peut rien pour lui.
     * C'était le trou : le frontend appelait déjà cet endpoint, jamais implémenté côté
     * serveur.
     */
    @Test
    void un_dossier_sans_aucune_tentative_peut_etre_confirme_directement() {
        when(candidatureRepository.findById(ID_CANDIDATURE)).thenReturn(Optional.of(candidature));
        when(paiementRepository.findByCandidatureId(ID_CANDIDATURE)).thenReturn(Optional.empty());

        PaiementResponse reponse = paiementService.enregistrerPaiementHorsLigne(ID_CANDIDATURE, "Virement");

        assertEquals(EnumStatutPaiement.SUCCES, reponse.getStatut());
        assertEquals("Virement", reponse.getMethodePaiement());
        verify(notificationService).notifierPaiementSucces("candidat@example.sn", "INPG-TEST0002");
    }

    /** Sans méthode précisée, la saisie manuelle reste identifiable dans l'historique. */
    @Test
    void la_methode_hors_ligne_par_defaut_est_utilisee_si_absente() {
        when(candidatureRepository.findById(ID_CANDIDATURE)).thenReturn(Optional.of(candidature));
        when(paiementRepository.findByCandidatureId(ID_CANDIDATURE)).thenReturn(Optional.empty());

        PaiementResponse reponse = paiementService.enregistrerPaiementHorsLigne(ID_CANDIDATURE, null);

        assertEquals("Hors ligne", reponse.getMethodePaiement());
    }

    /**
     * Contrairement à {@link PaiementService#confirmerPaiement}, une tentative en échec
     * doit pouvoir être soldée directement : c'est un constat humain (l'administration a
     * vérifié l'encaissement), pas la confirmation d'une tentative en ligne soumise à la
     * règle EN_ATTENTE → SUCCES.
     */
    @Test
    void une_tentative_en_echec_peut_etre_confirmee_directement() {
        when(candidatureRepository.findById(ID_CANDIDATURE)).thenReturn(Optional.of(candidature));
        when(paiementRepository.findByCandidatureId(ID_CANDIDATURE))
                .thenReturn(Optional.of(paiement(EnumStatutPaiement.ECHEC)));

        PaiementResponse reponse = paiementService.enregistrerPaiementHorsLigne(ID_CANDIDATURE, "Espèces");

        assertEquals(EnumStatutPaiement.SUCCES, reponse.getStatut());
    }

    @Test
    void un_dossier_deja_regle_ne_peut_pas_etre_reconfirme_hors_ligne() {
        when(candidatureRepository.findById(ID_CANDIDATURE)).thenReturn(Optional.of(candidature));
        when(paiementRepository.findByCandidatureId(ID_CANDIDATURE))
                .thenReturn(Optional.of(paiement(EnumStatutPaiement.SUCCES)));

        assertThrows(DuplicateResourceException.class,
                () -> paiementService.enregistrerPaiementHorsLigne(ID_CANDIDATURE, "Espèces"));
    }
}
