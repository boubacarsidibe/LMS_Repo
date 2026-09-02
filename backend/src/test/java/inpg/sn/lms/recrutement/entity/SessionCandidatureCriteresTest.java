package inpg.sn.lms.recrutement.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;

import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.recrutement.enums.EnumTypeCritere;

/**
 * {@link SessionCandidature#respecteCriteres} n'avait jamais été appelée nulle part
 * dans le code (voir CandidatController#soumettreCandidature, où un commentaire
 * renvoyait vers une méthode d'AdminRecrutementController qui n'existait pas) — donc
 * jamais testée non plus. Fige désormais le comportement, base et personnalisé.
 */
class SessionCandidatureCriteresTest {

    private Candidat candidatMajeur() {
        Candidat candidat = new Candidat();
        candidat.setDateNaissance(LocalDate.now().minusYears(25));
        candidat.setNiveauEtude("Master 1 (M1)");
        candidat.setGenre("Femme");
        candidat.setAnneesExperience(3);
        return candidat;
    }

    @Test
    void filtre_desactive_accepte_tout_le_monde() {
        SessionCandidature session = new SessionCandidature();
        session.setCriteresActifs(false);
        session.setCriteresAgeMinimum(30); // aurait rejeté sans le filtre désactivé

        assertTrue(session.respecteCriteres(candidatMajeur(), Map.of()));
    }

    @Test
    void un_critere_de_base_non_rempli_rejette() {
        SessionCandidature session = new SessionCandidature();
        session.setCriteresActifs(true);
        session.setCriteresAgeMinimum(30);

        assertFalse(session.respecteCriteres(candidatMajeur(), Map.of()));
    }

    @Test
    void un_critere_personnalise_inactif_est_ignore() {
        SessionCandidature session = new SessionCandidature();
        session.setCriteresActifs(true);
        CritereAdmissionPersonnalise critere = critereNumerique(1L, 10, false);
        session.getCriteresPersonnalises().add(critere);

        // Aucune réponse fournie, et le critère est retiré : ne doit pas bloquer.
        assertTrue(session.respecteCriteres(candidatMajeur(), Map.of()));
    }

    @Test
    void un_critere_personnalise_numerique_sous_le_seuil_rejette() {
        SessionCandidature session = new SessionCandidature();
        session.setCriteresActifs(true);
        CritereAdmissionPersonnalise critere = critereNumerique(1L, 10, true);
        session.getCriteresPersonnalises().add(critere);

        assertFalse(session.respecteCriteres(candidatMajeur(), Map.of(1L, "5")));
    }

    @Test
    void un_critere_personnalise_numerique_au_seuil_accepte() {
        SessionCandidature session = new SessionCandidature();
        session.setCriteresActifs(true);
        CritereAdmissionPersonnalise critere = critereNumerique(1L, 10, true);
        session.getCriteresPersonnalises().add(critere);

        assertTrue(session.respecteCriteres(candidatMajeur(), Map.of(1L, "10")));
    }

    @Test
    void un_critere_personnalise_sans_reponse_rejette_quand_une_regle_est_definie() {
        SessionCandidature session = new SessionCandidature();
        session.setCriteresActifs(true);
        CritereAdmissionPersonnalise critere = critereNumerique(1L, 10, true);
        session.getCriteresPersonnalises().add(critere);

        assertFalse(session.respecteCriteres(candidatMajeur(), Map.of()));
    }

    @Test
    void un_critere_personnalise_sans_regle_est_purement_informatif() {
        SessionCandidature session = new SessionCandidature();
        session.setCriteresActifs(true);
        CritereAdmissionPersonnalise critere = new CritereAdmissionPersonnalise();
        critere.setId(2L);
        critere.setType(EnumTypeCritere.TEXTE);
        critere.setActif(true);
        // Ni valeurMinimale ni valeurAttendue : aucune règle d'évaluation.
        session.getCriteresPersonnalises().add(critere);

        assertTrue(session.respecteCriteres(candidatMajeur(), Map.of()));
        assertTrue(session.respecteCriteres(candidatMajeur(), Map.of(2L, "n'importe quoi")));
    }

    @Test
    void un_critere_personnalise_texte_avec_valeur_attendue_est_insensible_a_la_casse() {
        SessionCandidature session = new SessionCandidature();
        session.setCriteresActifs(true);
        CritereAdmissionPersonnalise critere = new CritereAdmissionPersonnalise();
        critere.setId(3L);
        critere.setType(EnumTypeCritere.BOOLEEN);
        critere.setActif(true);
        critere.setValeurAttendue("Oui");
        session.getCriteresPersonnalises().add(critere);

        assertTrue(session.respecteCriteres(candidatMajeur(), Map.of(3L, "oui")));
        assertFalse(session.respecteCriteres(candidatMajeur(), Map.of(3L, "Non")));
    }

    @Test
    void base_et_personnalise_doivent_tous_deux_etre_respectes() {
        SessionCandidature session = new SessionCandidature();
        session.setCriteresActifs(true);
        session.setCriteresAgeMinimum(18); // respecté par candidatMajeur()
        CritereAdmissionPersonnalise critere = critereNumerique(1L, 10, true);
        session.getCriteresPersonnalises().add(critere);

        // Base OK, personnalisé KO -> global KO
        assertFalse(session.respecteCriteres(candidatMajeur(), Map.of(1L, "0")));
        // Les deux OK -> global OK
        assertTrue(session.respecteCriteres(candidatMajeur(), Map.of(1L, "20")));
    }

    private CritereAdmissionPersonnalise critereNumerique(Long id, int valeurMinimale, boolean actif) {
        CritereAdmissionPersonnalise critere = new CritereAdmissionPersonnalise();
        critere.setId(id);
        critere.setType(EnumTypeCritere.NUMERIQUE);
        critere.setValeurMinimale(valeurMinimale);
        critere.setActif(actif);
        return critere;
    }
}
