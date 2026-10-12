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

    // ---- Critères de présélection communiqués par l'INPG (questionnaire du 2026-09-30) ----

    private Candidat candidat(String niveau, int age) {
        Candidat candidat = new Candidat();
        candidat.setNiveauEtude(niveau);
        candidat.setDateNaissance(LocalDate.now().minusYears(age));
        return candidat;
    }

    private SessionCandidature session(String niveauMinimum, Integer ageMaximum) {
        SessionCandidature session = new SessionCandidature();
        session.setCriteresActifs(true);
        session.setCriteresNiveauEtudeMinimum(niveauMinimum);
        session.setCriteresAgeMaximum(ageMaximum);
        return session;
    }

    @Test
    void cps_niveau_4e_secondaire_minimum_et_30_ans_au_plus() {
        SessionCandidature cps = session("4e secondaire", 30);

        assertTrue(cps.respecteCriteres(candidat("4e secondaire", 30), Map.of()));
        assertTrue(cps.respecteCriteres(candidat("Baccalauréat", 22), Map.of()));
        assertFalse(cps.respecteCriteres(candidat("4e secondaire", 31), Map.of()));
    }

    @Test
    void licence_bac_plus_2_minimum_et_28_ans_au_plus() {
        SessionCandidature licence = session("Bac+2 (DUT / BTS)", 28);

        assertTrue(licence.respecteCriteres(candidat("Bac+2 (DUT / BTS)", 24), Map.of()));
        assertTrue(licence.respecteCriteres(candidat("Licence (L3)", 28), Map.of()));
        assertFalse(licence.respecteCriteres(candidat("Baccalauréat", 20), Map.of()));
        assertFalse(licence.respecteCriteres(candidat("4e secondaire", 19), Map.of()));
        assertFalse(licence.respecteCriteres(candidat("Licence (L3)", 29), Map.of()));
    }

    @Test
    void mastere_et_mba_bac_plus_5_sans_limite_d_age_pour_le_mba() {
        SessionCandidature mba = session("Master 2 (M2)", null);

        assertTrue(mba.respecteCriteres(candidat("Master 2 (M2)", 55), Map.of()));
        assertFalse(mba.respecteCriteres(candidat("Master 1 (M1)", 30), Map.of()));
    }

    @Test
    void un_niveau_hors_liste_ne_bloque_jamais() {
        assertTrue(session("Master 2 (M2)", null).respecteCriteres(candidat("Autre", 30), Map.of()));
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
