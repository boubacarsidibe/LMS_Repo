package inpg.sn.lms.recrutement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.auth.repository.CandidatRepository;
import inpg.sn.lms.exception.BusinessException;
import inpg.sn.lms.exception.ResourceNotFoundException;
import inpg.sn.lms.recrutement.dto.FiltreCriteresResponse;
import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.PieceJustificative;
import inpg.sn.lms.recrutement.entity.SessionCandidature;
import inpg.sn.lms.recrutement.enums.EnumStatutCandidature;
import inpg.sn.lms.recrutement.repository.CandidatureRepository;
import inpg.sn.lms.recrutement.repository.SessionCandidatureRepository;

/**
 * Filtre automatique de premier niveau (TDR 3.1.1), déclenché à la main par le
 * responsable admission — la seule action qui invoque réellement
 * {@link inpg.sn.lms.recrutement.entity.SessionCandidature#respecteCriteres}.
 */
@ExtendWith(MockitoExtension.class)
class CandidatureServiceFiltreCriteresTest {

    private static final long ID_SESSION = 7L;

    @Mock private CandidatureRepository candidatureRepository;
    @Mock private SessionCandidatureRepository sessionCandidatureRepository;
    @Mock private CandidatRepository candidatRepository;
    @Mock private JournalEtapeService journalEtapeService;

    @InjectMocks private CandidatureService service;

    private SessionCandidature session;

    @BeforeEach
    void setUp() {
        session = new SessionCandidature();
        session.setId(ID_SESSION);
        session.setCriteresActifs(true);
        session.setCriteresAgeMinimum(18);
        lenient().when(sessionCandidatureRepository.findById(ID_SESSION)).thenReturn(Optional.of(session));
        lenient().when(candidatureRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void session_introuvable_est_signalee() {
        when(sessionCandidatureRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.appliquerFiltreCriteres(99L, null));
    }

    @Test
    void refuse_de_filtrer_une_session_sans_filtre_actif() {
        session.setCriteresActifs(false);

        assertThrows(BusinessException.class, () -> service.appliquerFiltreCriteres(ID_SESSION, null));
    }

    @Test
    void seuls_les_dossiers_soumis_sont_evalues() {
        Candidature soumisRespecte = candidature(EnumStatutCandidature.SOUMIS, 25);
        Candidature soumisRejete = candidature(EnumStatutCandidature.SOUMIS, 10);
        Candidature dejaPreselectionne = candidature(EnumStatutCandidature.PRESELECTIONNE, 5);
        when(candidatureRepository.findBySessionCandidatureId(ID_SESSION))
                .thenReturn(List.of(soumisRespecte, soumisRejete, dejaPreselectionne));

        FiltreCriteresResponse resultat = service.appliquerFiltreCriteres(ID_SESSION, null);

        assertEquals(2, resultat.getTotal(), "seuls les dossiers SOUMIS comptent");
        assertEquals(1, resultat.getPreselectionnes());
        assertEquals(1, resultat.getRejetes());
        assertEquals(EnumStatutCandidature.PRESELECTIONNE, soumisRespecte.getStatut());
        assertEquals(EnumStatutCandidature.DOSSIER_REJETE, soumisRejete.getStatut());
        // Le dossier déjà en présélection n'est pas retouché.
        assertEquals(EnumStatutCandidature.PRESELECTIONNE, dejaPreselectionne.getStatut());
    }

    @Test
    void un_dossier_sans_piece_jointe_est_exclu_du_filtre() {
        Candidature soumisRespecte = candidature(EnumStatutCandidature.SOUMIS, 25);
        Candidature soumisVide = candidature(EnumStatutCandidature.SOUMIS, 25);
        soumisVide.getPiecesJustificatives().clear();
        when(candidatureRepository.findBySessionCandidatureId(ID_SESSION))
                .thenReturn(List.of(soumisRespecte, soumisVide));

        FiltreCriteresResponse resultat = service.appliquerFiltreCriteres(ID_SESSION, null);

        assertEquals(2, resultat.getTotal());
        assertEquals(1, resultat.getPreselectionnes());
        assertEquals(0, resultat.getRejetes());
        assertEquals(1, resultat.getDossiersVides());
        // Ni présélectionné ni rejeté : reste SOUMIS pour que le candidat puisse compléter son dossier.
        assertEquals(EnumStatutCandidature.SOUMIS, soumisVide.getStatut());
    }

    @Test
    void un_dossier_incomplet_n_est_ni_preselectionne_ni_rejete_par_le_filtre() {
        Candidature complet = candidature(EnumStatutCandidature.SOUMIS, 25);
        Candidature sansDiplome = candidature(EnumStatutCandidature.SOUMIS, 25);
        sansDiplome.getPiecesJustificatives()
                .removeIf(p -> p.getTypeDocument() == inpg.sn.lms.recrutement.enums.EnumTypeDocument.DIPLOME);
        Candidature incompletEtTropJeune = candidature(EnumStatutCandidature.SOUMIS, 10);
        incompletEtTropJeune.getPiecesJustificatives().removeIf(
                p -> p.getTypeDocument() == inpg.sn.lms.recrutement.enums.EnumTypeDocument.CV);
        when(candidatureRepository.findBySessionCandidatureId(ID_SESSION))
                .thenReturn(List.of(complet, sansDiplome, incompletEtTropJeune));

        FiltreCriteresResponse resultat = service.appliquerFiltreCriteres(ID_SESSION, null);

        assertEquals(1, resultat.getPreselectionnes());
        assertEquals(0, resultat.getRejetes(), "on ne rejette pas un dossier que le candidat peut encore compléter");
        assertEquals(2, resultat.getDossiersIncomplets());
        assertEquals(0, resultat.getDossiersVides());
        assertEquals(EnumStatutCandidature.SOUMIS, sansDiplome.getStatut());
        assertEquals(EnumStatutCandidature.SOUMIS, incompletEtTropJeune.getStatut());
    }

    private Candidature candidature(EnumStatutCandidature statut, int ageCandidat) {
        Candidat candidat = new Candidat();
        candidat.setDateNaissance(java.time.LocalDate.now().minusYears(ageCandidat));

        Candidature candidature = new Candidature();
        candidature.setStatut(statut);
        candidature.setCandidat(candidat);
        candidature.setSessionCandidature(session);
        // Seul un dossier complet est évalué par le filtre (CandidatureService#appliquerFiltreCriteres) :
        // sans ses quatre pièces, aucune candidature de ce fixture ne serait jamais évaluée.
        return DossiersDeTest.completer(candidature);
    }
}
