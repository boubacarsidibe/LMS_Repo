package inpg.sn.lms.espace_etudiant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.espace_etudiant.dto.CertificatApercuDto;
import inpg.sn.lms.espace_etudiant.dto.CertificatLotRequest;
import inpg.sn.lms.espace_etudiant.dto.CertificatLotRequest.Identite;
import inpg.sn.lms.espace_etudiant.dto.CertificatLotResultatDto;
import inpg.sn.lms.espace_etudiant.entity.Certificat;
import inpg.sn.lms.espace_etudiant.entity.Formation;
import inpg.sn.lms.espace_etudiant.entity.Promotion;
import inpg.sn.lms.espace_etudiant.repository.PromotionRepository;

/**
 * Délivrance collective : « tous les étudiants » / « tous sauf… », intitulé tiré de la
 * formation de la promotion, refus individuels sans blocage des autres.
 */
class CertificatLotServiceTest {

    private static final LocalDate DEBUT = LocalDate.of(2026, 2, 2);
    private static final LocalDate FIN = LocalDate.of(2026, 2, 6);

    private PromotionRepository promotionRepository;
    private EtudiantRepository etudiantRepository;
    private CompletionProgrammeService completion;
    private CertificatService certificatService;
    private CertificatLotService service;
    private Promotion promotion;
    private Etudiant awa;
    private Etudiant moussa;
    private Etudiant fatou;

    @BeforeEach
    void setUp() {
        promotionRepository = mock(PromotionRepository.class);
        etudiantRepository = mock(EtudiantRepository.class);
        completion = mock(CompletionProgrammeService.class);
        certificatService = mock(CertificatService.class);
        service = new CertificatLotService(promotionRepository, etudiantRepository, completion, certificatService);

        Formation formation = new Formation();
        formation.setIntitule("Comptage du gaz");
        promotion = new Promotion();
        ReflectionTestUtils.setField(promotion, "id", 5L);
        promotion.setLibelle("CG-2026");
        promotion.setFormation(formation);
        when(promotionRepository.findById(5L)).thenReturn(Optional.of(promotion));

        // Volontairement dans le désordre : le service les range par nom.
        moussa = etudiant(2L, "Moussa", "Ndiaye");
        fatou = etudiant(3L, "Fatou", "Diop");
        awa = etudiant(1L, "Awa", "Ba");
        when(etudiantRepository.findByPromotionId(5L)).thenReturn(List.of(moussa, fatou, awa));
        when(completion.coursNonTermines(any())).thenReturn(List.of());
        when(certificatService.getByEtudiant(any())).thenReturn(List.of());
        when(certificatService.save(any(Certificat.class))).thenAnswer(appel -> {
            Certificat certificat = appel.getArgument(0);
            certificat.assurerReferenceEtDate();
            return certificat;
        });
    }

    private static Etudiant etudiant(long id, String prenom, String nom) {
        Etudiant etudiant = new Etudiant(null, nom, prenom, "77" + id);
        etudiant.setId(id);
        return etudiant;
    }

    private static List<Identite> identitesPour(Etudiant... etudiants) {
        List<Identite> identites = new ArrayList<>();
        for (Etudiant e : etudiants) {
            identites.add(new Identite(e.getId(), "Madame", LocalDate.of(1990, 1, 1)));
        }
        return identites;
    }

    private CertificatLotRequest demande(String mode, List<Long> exclus, String code, List<Identite> identites) {
        return new CertificatLotRequest(5L, mode, exclus, null, DEBUT, FIN, code, identites);
    }

    @Test
    @DisplayName("Aperçu : la formation de la promotion, les étudiants par ordre alphabétique, avec leur éligibilité")
    void apercuDonneLaFormationEtLEligibilite() {
        when(completion.coursNonTermines(moussa)).thenReturn(List.of("Métrologie"));

        CertificatApercuDto apercu = service.apercu(5L).orElseThrow();

        assertThat(apercu.formationIntitule()).isEqualTo("Comptage du gaz");
        assertThat(apercu.etudiants()).extracting(CertificatApercuDto.EtudiantApercu::nom)
                .containsExactly("Awa Ba", "Fatou Diop", "Moussa Ndiaye");
        assertThat(apercu.etudiants()).extracting(CertificatApercuDto.EtudiantApercu::eligible)
                .containsExactly(true, true, false);
        assertThat(apercu.etudiants().get(2).coursNonTermines()).containsExactly("Métrologie");
    }

    @Test
    @DisplayName("Aperçu : promotion sans formation → intitulé null, pour que l'écran puisse le signaler")
    void apercuSansFormation() {
        promotion.setFormation(null);

        assertThat(service.apercu(5L).orElseThrow().formationIntitule()).isNull();
    }

    @Test
    @DisplayName("Tous : un certificat par étudiant, intitulé de la formation, jamais celui saisi")
    void tousDelivreATouteLaPromotion() {
        CertificatLotResultatDto resultat = service.delivrer(demande("TOUS", null, null,
                identitesPour(awa, moussa, fatou)));

        assertThat(resultat.delivres()).extracting(CertificatLotResultatDto.Delivre::etudiantNom)
                .containsExactly("Awa Ba", "Fatou Diop", "Moussa Ndiaye");
        assertThat(resultat.refuses()).isEmpty();
        ArgumentCaptor<Certificat> captor = ArgumentCaptor.forClass(Certificat.class);
        verify(certificatService, org.mockito.Mockito.times(3)).save(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(c -> {
            assertThat(c.getIntituleFormation()).isEqualTo("Comptage du gaz");
            assertThat(c.getPromotion()).isSameAs(promotion);
            assertThat(c.getDateDebut()).isEqualTo(DEBUT);
            assertThat(c.getDateFin()).isEqualTo(FIN);
            assertThat(c.getFormateurNom()).isEqualTo("INPG — Direction pédagogique");
            assertThat(c.getCodeCertificat()).isNull();
        });
    }

    @Test
    @DisplayName("Tous sauf : l'étudiant exclu ne reçoit rien")
    void tousSaufExclutLesEtudiantsChoisis() {
        // Identité fournie pour les trois : l'exclusion est la seule raison de ne rien délivrer à Moussa.
        CertificatLotResultatDto resultat = service.delivrer(demande("TOUS_SAUF", List.of(2L), null,
                identitesPour(awa, moussa, fatou)));

        assertThat(resultat.delivres()).extracting(CertificatLotResultatDto.Delivre::etudiantId)
                .containsExactly(1L, 3L);
        assertThat(resultat.refuses()).isEmpty();
        verify(certificatService, never()).save(org.mockito.ArgumentMatchers.argThat(
                c -> c.getEtudiant() == moussa));
    }

    @Test
    @DisplayName("Tous sauf sans personne à exclure (ou un identifiant étranger à la promotion) : refusé")
    void tousSaufExigeUneExclusionDeLaPromotion() {
        assertThatThrownBy(() -> service.delivrer(demande("TOUS_SAUF", List.of(), null, identitesPour(awa))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("au moins un étudiant");
        assertThatThrownBy(() -> service.delivrer(demande("TOUS_SAUF", List.of(999L), null, identitesPour(awa))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("au moins un étudiant");
        verify(certificatService, never()).save(any());
    }

    @Test
    @DisplayName("Tous : une liste d'exclus envoyée par erreur est ignorée")
    void tousIgnoreLesExclus() {
        CertificatLotResultatDto resultat = service.delivrer(demande("TOUS", List.of(1L, 2L), null,
                identitesPour(awa, moussa, fatou)));

        assertThat(resultat.delivres()).hasSize(3);
    }

    @Test
    @DisplayName("Un étudiant au programme inachevé est écarté avec ses cours, les autres sont délivrés")
    void unRetardataireNeBloquePasLaPromotion() {
        when(completion.coursNonTermines(moussa)).thenReturn(List.of("Métrologie", "Sécurité"));

        CertificatLotResultatDto resultat = service.delivrer(demande("TOUS", null, null,
                identitesPour(awa, moussa, fatou)));

        assertThat(resultat.delivres()).extracting(CertificatLotResultatDto.Delivre::etudiantId)
                .containsExactly(1L, 3L);
        assertThat(resultat.refuses()).singleElement().satisfies(refus -> {
            assertThat(refus.etudiantId()).isEqualTo(2L);
            assertThat(refus.raison()).contains("Programme non terminé");
            assertThat(refus.coursNonTermines()).containsExactly("Métrologie", "Sécurité");
        });
    }

    @Test
    @DisplayName("Sans civilité (ni saisie ni fiche) : écarté, jamais un certificat sans civilité")
    void civiliteManquante() {
        CertificatLotResultatDto resultat = service.delivrer(demande("TOUS", null, null,
                identitesPour(awa, fatou)));

        assertThat(resultat.delivres()).extracting(CertificatLotResultatDto.Delivre::etudiantId)
                .containsExactly(1L, 3L);
        assertThat(resultat.refuses()).singleElement().satisfies(refus -> {
            assertThat(refus.etudiantId()).isEqualTo(2L);
            assertThat(refus.raison()).contains("Civilité manquante");
        });
    }

    @Test
    @DisplayName("La civilité de la fiche sert quand l'écran n'en envoie pas ; une civilité inconnue est écartée")
    void civiliteDeLaFicheEtCiviliteInconnue() {
        moussa.setCivilite("Monsieur");
        fatou.setCivilite("Docteur");

        CertificatLotResultatDto resultat = service.delivrer(demande("TOUS", null, null, identitesPour(awa)));

        assertThat(resultat.delivres()).extracting(CertificatLotResultatDto.Delivre::etudiantId)
                .containsExactly(1L, 2L);
        assertThat(resultat.refuses()).singleElement().satisfies(refus -> {
            assertThat(refus.etudiantId()).isEqualTo(3L);
            assertThat(refus.raison()).contains("Civilité inconnue");
        });
    }

    @Test
    @DisplayName("Un certificat identique (même formation, même période) n'est pas délivré une seconde fois")
    void pasDeDoublon() {
        Certificat existant = new Certificat();
        existant.setIntituleFormation("comptage du gaz");
        existant.setDateDebut(DEBUT);
        existant.setDateFin(FIN);
        when(certificatService.getByEtudiant(1L)).thenReturn(List.of(existant));

        CertificatLotResultatDto resultat = service.delivrer(demande("TOUS", null, null,
                identitesPour(awa, moussa, fatou)));

        assertThat(resultat.delivres()).extracting(CertificatLotResultatDto.Delivre::etudiantId)
                .containsExactly(3L, 2L);
        assertThat(resultat.refuses()).singleElement().satisfies(refus -> {
            assertThat(refus.etudiantId()).isEqualTo(1L);
            assertThat(refus.raison()).contains("déjà été délivré");
        });
    }

    @Test
    @DisplayName("Le code de session est suivi d'un numéro d'ordre, sans trou quand un étudiant est écarté")
    void codeNumerote() {
        when(completion.coursNonTermines(fatou)).thenReturn(List.of("Métrologie"));

        service.delivrer(demande("TOUS", null, " CG1001260206 ", identitesPour(awa, moussa, fatou)));

        ArgumentCaptor<Certificat> captor = ArgumentCaptor.forClass(Certificat.class);
        verify(certificatService, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(Certificat::getCodeCertificat)
                .containsExactly("CG1001260206/01", "CG1001260206/02");
    }

    @Test
    @DisplayName("Civilité et naissance saisies à l'écran sont retenues sur la fiche pour la fois suivante")
    void identiteMemoriseeSurLaFiche() {
        service.delivrer(demande("TOUS_SAUF", List.of(2L, 3L), null, List.of(
                new Identite(1L, "Madame", LocalDate.of(1992, 5, 17)))));

        assertThat(awa.getCivilite()).isEqualTo("Madame");
        assertThat(awa.getDateNaissance()).isEqualTo(LocalDate.of(1992, 5, 17));
        verify(etudiantRepository).save(awa);
    }

    private CertificatLotRequest demandeUnSeul(Long etudiantId, String code, List<Identite> identites) {
        return new CertificatLotRequest(5L, "UN_SEUL", List.of(), etudiantId, DEBUT, FIN, code, identites);
    }

    @Test
    @DisplayName("Un seul étudiant : lui seul reçoit son certificat, les autres de la promotion rien")
    void unSeulEtudiant() {
        CertificatLotResultatDto resultat = service.delivrer(demandeUnSeul(2L, null, identitesPour(awa, moussa, fatou)));

        assertThat(resultat.delivres()).extracting(CertificatLotResultatDto.Delivre::etudiantId).containsExactly(2L);
        assertThat(resultat.refuses()).isEmpty();
        ArgumentCaptor<Certificat> captor = ArgumentCaptor.forClass(Certificat.class);
        verify(certificatService).save(captor.capture());
        assertThat(captor.getValue().getEtudiant()).isSameAs(moussa);
        assertThat(captor.getValue().getIntituleFormation()).isEqualTo("Comptage du gaz");
    }

    @Test
    @DisplayName("Un seul étudiant : le code est imprimé tel quel, sans numéro d'ordre ajouté")
    void unSeulEtudiantGardeSonCodeTelQuel() {
        service.delivrer(demandeUnSeul(2L, " CG1001260206/03 ", identitesPour(moussa)));

        ArgumentCaptor<Certificat> captor = ArgumentCaptor.forClass(Certificat.class);
        verify(certificatService).save(captor.capture());
        assertThat(captor.getValue().getCodeCertificat()).isEqualTo("CG1001260206/03");
    }

    @Test
    @DisplayName("Un seul étudiant : aucun étudiant choisi, ou un étudiant d'une autre promotion → refusé")
    void unSeulEtudiantDoitEtreDeLaPromotion() {
        assertThatThrownBy(() -> service.delivrer(demandeUnSeul(null, null, identitesPour(moussa))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Choisissez l'étudiant");
        assertThatThrownBy(() -> service.delivrer(demandeUnSeul(999L, null, identitesPour(moussa))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("n'appartient pas");
        verify(certificatService, never()).save(any());
    }

    @Test
    @DisplayName("Un seul étudiant au programme inachevé : écarté avec ses cours, rien n'est délivré")
    void unSeulEtudiantInacheve() {
        when(completion.coursNonTermines(moussa)).thenReturn(List.of("Métrologie"));

        CertificatLotResultatDto resultat = service.delivrer(demandeUnSeul(2L, null, identitesPour(moussa)));

        assertThat(resultat.delivres()).isEmpty();
        assertThat(resultat.refuses()).singleElement().satisfies(refus -> {
            assertThat(refus.etudiantId()).isEqualTo(2L);
            assertThat(refus.coursNonTermines()).containsExactly("Métrologie");
        });
    }

    @Test
    @DisplayName("Un seul étudiant : le code peut faire 60 caractères (pas de numéro d'ordre à y ajouter)")
    void unSeulEtudiantCodeLong() {
        service.delivrer(demandeUnSeul(2L, "x".repeat(60), identitesPour(moussa)));
        assertThatThrownBy(() -> service.delivrer(demandeUnSeul(2L, "x".repeat(61), identitesPour(moussa))))
                .hasMessageContaining("trop long");
    }

    @Test
    @DisplayName("Demandes invalides : rien n'est délivré")
    void demandesInvalides() {
        Promotion sansFormation = new Promotion();
        ReflectionTestUtils.setField(sansFormation, "id", 6L);
        when(promotionRepository.findById(6L)).thenReturn(Optional.of(sansFormation));

        assertThatThrownBy(() -> service.delivrer(new CertificatLotRequest(null, "TOUS", null, null, DEBUT, FIN, null, null)))
                .hasMessageContaining("promotion est obligatoire");
        assertThatThrownBy(() -> service.delivrer(new CertificatLotRequest(404L, "TOUS", null, null, DEBUT, FIN, null, null)))
                .hasMessageContaining("introuvable");
        assertThatThrownBy(() -> service.delivrer(demande("PERSONNE", null, null, null)))
                .hasMessageContaining("tous les étudiants");
        assertThatThrownBy(() -> service.delivrer(new CertificatLotRequest(6L, "TOUS", null, null, DEBUT, FIN, null, null)))
                .hasMessageContaining("aucune formation");
        assertThatThrownBy(() -> service.delivrer(new CertificatLotRequest(5L, "TOUS", null, null, FIN, DEBUT, null, null)))
                .hasMessageContaining("précède");
        assertThatThrownBy(() -> service.delivrer(new CertificatLotRequest(5L, "TOUS", null, null, null, FIN, null, null)))
                .hasMessageContaining("obligatoires");
        assertThatThrownBy(() -> service.delivrer(demande("TOUS", null, "x".repeat(58), null)))
                .hasMessageContaining("trop long");
        verify(certificatService, never()).save(any());
    }
}
