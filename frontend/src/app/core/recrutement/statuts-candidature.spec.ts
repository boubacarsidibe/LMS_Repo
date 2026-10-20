import {
  actionSuivante,
  attenteDuDossier,
  CleAction,
  classeStatut,
  estAdmission,
  estAInstruire,
  estATraiter,
  estDecide,
  estRejete,
  estEnEvaluation,
  estRefus,
  etapeActuelle,
  friseParcours,
  GROUPES_STATUTS,
  libelleStatut,
  LIBELLE_STATUT,
  messageCandidat,
  peutAnnuler,
  peutReculerVers,
  peutRejeter,
  STATUTS_A_DELIBERER,
  STATUTS_CANDIDATURE,
  STATUTS_CONVOCABLES_A_L_ENTRETIEN,
  STATUTS_CONVOCABLES_AU_TEST,
  statutsDuFiltre,
  StatutCandidature,
  tonStatut,
} from './statuts-candidature';

/**
 * Copie de {@code CandidatureService#estTransitionValide} (backend). Ce test existe parce que le frontend avait cessé de
 * suivre le serveur : il proposait des gestes que le serveur refusait ({@code CONVOQUE_TEST → CONVOQUE_ENTRETIEN}) et en
 * omettait d'autres. Si le serveur change une transition, ce tableau — et ce fichier — doivent changer avec lui.
 */
const TRANSITIONS_SERVEUR: Record<StatutCandidature, StatutCandidature[]> = {
  BROUILLON: ['SOUMIS', 'BROUILLON'],
  SOUMIS: ['INCOMPLET', 'DOSSIER_VALIDE', 'PRESELECTIONNE', 'DOSSIER_REJETE'],
  INCOMPLET: ['SOUMIS', 'DOSSIER_REJETE'],
  DOSSIER_VALIDE: ['PRESELECTIONNE', 'DOSSIER_REJETE'],
  PRESELECTIONNE: ['PAIEMENT_EN_ATTENTE', 'PAIEMENT_CONFIRME', 'DOSSIER_REJETE'],
  PAIEMENT_EN_ATTENTE: ['PAIEMENT_CONFIRME', 'PRESELECTIONNE'],
  PAIEMENT_CONFIRME: ['CONVOQUE_TEST'],
  CONVOQUE_TEST: ['TEST_REUSSI', 'TEST_ECHOUE', 'ABSENT_TEST'],
  ABSENT_TEST: ['CONVOQUE_TEST', 'NON_ADMIS'],
  TEST_REUSSI: ['CONVOQUE_ENTRETIEN', 'NON_ADMIS'],
  TEST_ECHOUE: ['NON_ADMIS'],
  CONVOQUE_ENTRETIEN: ['ENTRETIEN_EFFECTUE', 'ABSENT_ENTRETIEN'],
  ABSENT_ENTRETIEN: ['CONVOQUE_ENTRETIEN', 'NON_ADMIS'],
  ENTRETIEN_EFFECTUE: ['ADMIS', 'LISTE_ATTENTE', 'NON_ADMIS'],
  LISTE_ATTENTE: ['ADMIS', 'NON_ADMIS'],
  ADMIS: ['INSCRIPTION_FINALISEE', 'DESISTEMENT'],
  DOSSIER_REJETE: [],
  NON_ADMIS: [],
  INSCRIPTION_FINALISEE: [],
  DESISTEMENT: [],
};

const CIBLE_DE_L_ACTION: Record<CleAction, StatutCandidature> = {
  VALIDER_PRESELECTION: 'PRESELECTIONNE',
  ADMETTRE_TEST: 'CONVOQUE_TEST',
  ADMETTRE_ENTRETIEN: 'CONVOQUE_ENTRETIEN',
  ADMETTRE_FINAL: 'ADMIS',
  FINALISER_INSCRIPTION: 'INSCRIPTION_FINALISEE',
};

describe('Statuts de candidature', () => {
  describe('vocabulaire', () => {
    it('chaque statut du serveur a un libellé lisible — aucun code brut à l\'écran', () => {
      for (const statut of STATUTS_CANDIDATURE) {
        expect(LIBELLE_STATUT[statut]).toBeTruthy();
        expect(libelleStatut(statut)).not.toBe(statut);
        expect(libelleStatut(statut)).not.toMatch(/_/);
      }
    });

    it('un statut inconnu est rendu tel quel au lieu de faire planter l\'écran', () => {
      expect(libelleStatut('STATUT_DE_DEMAIN')).toBe('STATUT_DE_DEMAIN');
      expect(libelleStatut(null)).toBe('');
    });

    it('les anciens codes (ADMIS1/2/3, REJETE, EN_COURS_DE_PRESELECTION) ne sont plus des statuts', () => {
      for (const ancien of ['ADMIS1', 'ADMIS2', 'ADMIS3', 'REJETE', 'EN_COURS_DE_PRESELECTION']) {
        expect((STATUTS_CANDIDATURE as readonly string[]).includes(ancien), ancien).toBe(false);
      }
    });

    it('chaque statut a un ton et des classes de badge', () => {
      for (const statut of STATUTS_CANDIDATURE) {
        expect(tonStatut(statut)).toBeTruthy();
        expect(classeStatut(statut)).toMatch(/^bg-/);
      }
      expect(tonStatut('ADMIS')).toBe('succes');
      expect(tonStatut('NON_ADMIS')).toBe('echec');
      expect(tonStatut('LISTE_ATTENTE')).toBe('liste');
    });
  });

  describe('familles', () => {
    it('les onglets couvrent chaque statut une seule fois, hors brouillon', () => {
      const vus = GROUPES_STATUTS.flatMap((g) => g.statuts ?? []);
      expect(new Set(vus).size).toBe(vus.length);
      const manquants = STATUTS_CANDIDATURE.filter((s) => s !== 'BROUILLON' && !vus.includes(s));
      expect(manquants).toEqual([]);
    });

    it('un filtre d\'URL est une clé d\'onglet ou un statut précis ; le reste est ignoré', () => {
      expect(statutsDuFiltre('A_INSTRUIRE')).toEqual(['SOUMIS', 'INCOMPLET', 'DOSSIER_VALIDE']);
      expect(statutsDuFiltre('SOUMIS')).toEqual(['SOUMIS']);
      expect(statutsDuFiltre('TOUS')).toBeNull();
      expect(statutsDuFiltre('N_IMPORTE_QUOI')).toBeUndefined();
    });

    it('un filtre peut réunir plusieurs onglets et statuts : une carte de chiffres renvoie vers la liste exactement filtrée', () => {
      expect(statutsDuFiltre('SOUMIS,DOSSIER_VALIDE')).toEqual(['SOUMIS', 'DOSSIER_VALIDE']);
      expect(statutsDuFiltre('LISTE_ATTENTE,ADMIS')).toEqual(['LISTE_ATTENTE', 'ADMIS', 'INSCRIPTION_FINALISEE']);
      expect(statutsDuFiltre('SOUMIS,SOUMIS')).toEqual(['SOUMIS']);
      expect(statutsDuFiltre('SOUMIS,TOUS')).toBeNull();
      expect(statutsDuFiltre('SOUMIS,INCONNU')).toBeUndefined();
    });

    it('« à traiter » et « rejeté » : les deux chiffres des tableaux de bord', () => {
      expect(estATraiter('SOUMIS')).toBe(true);
      expect(estATraiter('DOSSIER_VALIDE')).toBe(true);
      expect(estATraiter('INCOMPLET')).toBe(false); // attend le candidat, pas le responsable
      expect(estRejete('DOSSIER_REJETE')).toBe(true);
      expect(estRejete('NON_ADMIS')).toBe(true);
      expect(estRejete('DESISTEMENT')).toBe(false); // choix du candidat, pas un refus
    });

    it('les prédicats de décision distinguent admis, refusés et liste complémentaire', () => {
      expect(estAdmission('ADMIS')).toBe(true);
      expect(estAdmission('INSCRIPTION_FINALISEE')).toBe(true);
      expect(estRefus('NON_ADMIS')).toBe(true);
      expect(estRefus('DOSSIER_REJETE')).toBe(true);
      expect(estDecide('LISTE_ATTENTE')).toBe(false);
      expect(estDecide('ADMIS')).toBe(true);
      expect(estDecide('NON_ADMIS')).toBe(true);
      expect(estAInstruire('SOUMIS')).toBe(true);
      expect(estAInstruire('PRESELECTIONNE')).toBe(false);
      expect(estEnEvaluation('CONVOQUE_ENTRETIEN')).toBe(true);
      expect(estEnEvaluation('ADMIS')).toBe(false);
    });
  });

  describe('gestes proposés au responsable — ils doivent être acceptés par le serveur', () => {
    it('chaque geste proposé mène à un statut que le serveur autorise depuis le statut courant', () => {
      for (const statut of STATUTS_CANDIDATURE) {
        const action = actionSuivante(statut);
        if (!action) continue;
        expect(TRANSITIONS_SERVEUR[statut], `${statut} → « ${action.libelle} » (${CIBLE_DE_L_ACTION[action.cle]})`).toContain(CIBLE_DE_L_ACTION[action.cle]);
      }
    });

    it('« Rejeter » n\'est proposé que là où le serveur accepte un refus', () => {
      for (const statut of STATUTS_CANDIDATURE) {
        const refusable = TRANSITIONS_SERVEUR[statut].some((s) => s === 'DOSSIER_REJETE' || s === 'NON_ADMIS');
        expect(peutRejeter(statut), statut).toBe(refusable);
      }
    });

    it('le parcours complet est franchissable geste par geste, sans impasse', () => {
      // SOUMIS → présélection → (paiement) → test → (notes) → entretien → (note) → admission → inscription
      expect(actionSuivante('SOUMIS')?.cle).toBe('VALIDER_PRESELECTION');
      expect(actionSuivante('PAIEMENT_CONFIRME')?.cle).toBe('ADMETTRE_TEST');
      expect(actionSuivante('TEST_REUSSI')?.cle).toBe('ADMETTRE_ENTRETIEN');
      expect(actionSuivante('ENTRETIEN_EFFECTUE')?.cle).toBe('ADMETTRE_FINAL');
      expect(actionSuivante('LISTE_ATTENTE')?.cle).toBe('ADMETTRE_FINAL');
      expect(actionSuivante('ADMIS')?.cle).toBe('FINALISER_INSCRIPTION');
    });

    it('un dossier sans geste dit ce qu\'il attend', () => {
      for (const statut of ['INCOMPLET', 'PRESELECTIONNE', 'PAIEMENT_EN_ATTENTE', 'CONVOQUE_TEST', 'CONVOQUE_ENTRETIEN']) {
        expect(actionSuivante(statut)).toBeNull();
        expect(attenteDuDossier(statut)).toBeTruthy();
      }
    });

    it('les candidats convocables au test, à l\'entretien et à la délibération sont ceux du nouveau parcours', () => {
      expect(STATUTS_CONVOCABLES_AU_TEST).toEqual(['CONVOQUE_TEST']);
      expect(STATUTS_CONVOCABLES_A_L_ENTRETIEN).toEqual(['CONVOQUE_ENTRETIEN']);
      expect(STATUTS_A_DELIBERER).toEqual(['ENTRETIEN_EFFECTUE', 'LISTE_ATTENTE']);
    });
  });

  describe('corrections', () => {
    it('on ne recule que vers une étape strictement antérieure du parcours', () => {
      expect(peutReculerVers('PRESELECTIONNE', 'SOUMIS')).toBe(true);
      expect(peutReculerVers('CONVOQUE_ENTRETIEN', 'CONVOQUE_TEST')).toBe(true);
      expect(peutReculerVers('SOUMIS', 'SOUMIS')).toBe(false);
      expect(peutReculerVers('SOUMIS', 'PRESELECTIONNE')).toBe(false);
    });
    it('un refus ou la liste complémentaire peut être défait vers n\'importe quelle étape', () => {
      for (const s of ['DOSSIER_REJETE', 'NON_ADMIS', 'LISTE_ATTENTE']) {
        expect(peutReculerVers(s, 'SOUMIS'), s).toBe(true);
        expect(peutReculerVers(s, 'CONVOQUE_ENTRETIEN'), s).toBe(true);
      }
    });
    it('un dossier admis ne recule pas : il s\'annule, ou l\'inscription le verrouille', () => {
      expect(peutReculerVers('ADMIS', 'SOUMIS')).toBe(false);
      expect(peutReculerVers('INSCRIPTION_FINALISEE', 'SOUMIS')).toBe(false);
      expect(peutAnnuler('INSCRIPTION_FINALISEE', 'ADMIS')).toBe(false);
      expect(peutAnnuler('ADMIS', 'ENTRETIEN_EFFECTUE')).toBe(true);
      expect(peutAnnuler('ADMIS', null)).toBe(false);
    });
  });

  describe('frise du parcours', () => {
    const etat = (statut: string, precedent?: string) =>
      friseParcours(statut, { audience: 'agent', statutPrecedent: precedent }).map((i) =>
        i.erreur ? 'E' : i.active ? 'A' : i.pending ? '.' : 'F'
      ).join('');

    it('SOUMIS : dossier fait, présélection en cours', () => expect(etat('SOUMIS')).toBe('FA...'));
    it('PRESELECTIONNE : le paiement est l\'étape en cours', () => expect(etat('PRESELECTIONNE')).toBe('FFA..'));
    it('PAIEMENT_CONFIRME / CONVOQUE_TEST : le test est l\'étape en cours', () => {
      expect(etat('PAIEMENT_CONFIRME')).toBe('FFFA.');
      expect(etat('CONVOQUE_TEST')).toBe('FFFA.');
    });
    it('TEST_REUSSI / CONVOQUE_ENTRETIEN : l\'entretien est l\'étape en cours', () => {
      expect(etat('TEST_REUSSI')).toBe('FFFFA');
      expect(etat('CONVOQUE_ENTRETIEN')).toBe('FFFFA');
    });
    it('un dossier admis a franchi toutes les étapes — il ne reste plus figé devant l\'entretien', () => {
      for (const s of ['ENTRETIEN_EFFECTUE', 'ADMIS', 'LISTE_ATTENTE', 'INSCRIPTION_FINALISEE']) {
        expect(etat(s), s).toBe('FFFFF');
      }
    });
    it('un dossier rejeté échoue à la présélection ; un test échoué, au test', () => {
      expect(etat('DOSSIER_REJETE')).toBe('FE...');
      expect(etat('TEST_ECHOUE')).toBe('FFFE.');
      expect(etat('ABSENT_TEST')).toBe('FFFE.');
    });
    it('NON_ADMIS échoue là où le dossier s\'est arrêté, d\'après le statut précédent', () => {
      expect(etat('NON_ADMIS', 'TEST_ECHOUE')).toBe('FFFE.');
      expect(etat('NON_ADMIS', 'ENTRETIEN_EFFECTUE')).toBe('FFFFE');
      expect(etat('NON_ADMIS')).toBe('FFFFE');
    });
    it('un brouillon ou un statut inconnu : le dossier est l\'étape en cours', () => {
      expect(etat('BROUILLON')).toBe('A....');
      expect(etat('INCOMPLET')).toBe('A....');
      expect(etat('STATUT_DE_DEMAIN')).toBe('A....');
    });
    it('le texte s\'adresse au candidat (« votre ») ou à l\'agent (« la ») selon l\'audience', () => {
      expect(friseParcours('SOUMIS', { audience: 'candidat' })[1].desc).toContain('votre');
      expect(friseParcours('SOUMIS', { audience: 'agent' })[1].desc).not.toContain('votre');
    });
    it('« Étape n sur 5 » n\'apparaît qu\'à partir du test', () => {
      expect(etapeActuelle('SOUMIS')).toBe('');
      expect(etapeActuelle('CONVOQUE_TEST')).toBe('Étape 4 sur 5');
      expect(etapeActuelle('CONVOQUE_ENTRETIEN')).toBe('Étape 5 sur 5');
      expect(etapeActuelle('ADMIS')).toBe('Étape 5 sur 5');
    });
  });

  describe('ce que lit le candidat', () => {
    it('chaque statut a un message complet', () => {
      for (const statut of STATUTS_CANDIDATURE) {
        const m = messageCandidat(statut, 'Hiver 2026');
        expect(m.badge, statut).toBeTruthy();
        expect(m.titre, statut).toBeTruthy();
        expect(m.description, statut).toBeTruthy();
      }
    });

    it('aucun message ne cite un programme ni une promotion en dur', () => {
      for (const statut of STATUTS_CANDIDATURE) {
        const m = messageCandidat(statut, 'Session Hiver 2026-2027 — Executive MBA');
        const texte = `${m.titre} ${m.description}`;
        expect(texte, statut).not.toMatch(/Bachelor|Strategic Management|2024-2025/);
      }
    });

    it('le libellé de la session du dossier est repris, pas un nom de programme codé en dur', () => {
      expect(messageCandidat('TEST_ECHOUE', 'Hiver 2026').description).toContain('« Hiver 2026 »');
      expect(messageCandidat('ADMIS', 'Hiver 2026').description).toContain('« Hiver 2026 »');
      expect(messageCandidat('ADMIS', null).description).toContain('cette session');
    });

    it('les refus gardent un ton respectueux et jamais le mot « rejeté » pour une décision de jury', () => {
      expect(messageCandidat('NON_ADMIS').badge).toBe('Non admis(e)');
      expect(messageCandidat('NON_ADMIS').description).not.toMatch(/rejet/i);
    });
  });
});
