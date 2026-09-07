package inpg.sn.lms.recrutement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.EtapeDossier;
import inpg.sn.lms.recrutement.enums.EnumEtapeQms;
import inpg.sn.lms.recrutement.enums.EnumStatutCandidature;
import inpg.sn.lms.recrutement.repository.EtapeDossierRepository;

/**
 * Journal d'étapes QMS-009. Ce qui est figé ici, c'est le <strong>cliché du nom</strong> :
 * une ligne du journal doit rester lisible même si le compte de l'agent disparaît ou est
 * renommé, sans quoi elle ne prouve plus rien (même principe que {@code releve_ligne},
 * migration V9).
 */
@ExtendWith(MockitoExtension.class)
class JournalEtapeServiceTest {

    @Mock private EtapeDossierRepository repository;

    @InjectMocks private JournalEtapeService service;

    private Candidature candidature;

    @BeforeEach
    void setUp() {
        candidature = new Candidature();
        when(repository.save(any(EtapeDossier.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void une_etape_sans_agent_est_signee_systeme() {
        EtapeDossier ligne = service.enregistrer(candidature, EnumEtapeQms.RECEPTION,
                EnumStatutCandidature.SOUMIS, null, "Dossier déposé par le candidat.");

        assertEquals("Système", ligne.getAuteurNom());
        assertNull(ligne.getAuteur(), "aucun compte n'est à l'origine de cette étape");
        assertEquals(EnumEtapeQms.RECEPTION, ligne.getEtape());
        assertEquals(EnumStatutCandidature.SOUMIS, ligne.getStatutResultant());
    }

    @Test
    void le_nom_de_l_agent_est_recopie_dans_la_ligne() {
        User agent = new User();
        agent.setUsername("resp.admission");

        EtapeDossier ligne = service.enregistrer(candidature, EnumEtapeQms.SELECTION,
                EnumStatutCandidature.ADMIS3, agent, "Admis.");

        assertEquals("resp.admission", ligne.getAuteurNom());
        assertEquals(agent, ligne.getAuteur(), "le lien reste tant que le compte existe");
    }

    @Test
    void un_agent_sans_identifiant_est_designe_par_son_email() {
        User agent = new User();
        agent.setUsername("  ");
        agent.setEmail("test.admission@inpg.sn");

        EtapeDossier ligne = service.enregistrer(candidature, EnumEtapeQms.EVALUATION,
                EnumStatutCandidature.ADMIS1, agent, null);

        assertEquals("test.admission@inpg.sn", ligne.getAuteurNom());
    }

    @Test
    void un_agent_sans_identifiant_ni_email_ne_laisse_pas_la_colonne_vide() {
        // auteur_nom est NOT NULL (migration V32) : sans ce repli, l'écriture échouerait
        // et l'action métier échouerait avec elle.
        EtapeDossier ligne = service.enregistrer(candidature, EnumEtapeQms.INSCRIPTION,
                EnumStatutCandidature.ADMIS3, new User(), null);

        assertEquals("Système", ligne.getAuteurNom());
    }
}
