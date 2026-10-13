import {
  GROUPES_STATUT, STATUTS_CANDIDATURE, actionSuivante, classeStatut, estAdmis, estDecide, estEngageDansLaSelection,
  estRefuse, groupeDeStatut, libelleAction, libelleStatut, peutAnnuler, peutRejeter, reculsPossibles, statutsDuGroupe,
} from './statuts-admission';

/** Les 20 valeurs de `EnumStatutCandidature` (serveur). Si le serveur en ajoute une, ce test doit le dire. */
const STATUTS_SERVEUR = [
  'BROUILLON', 'SOUMIS', 'INCOMPLET', 'DOSSIER_VALIDE', 'DOSSIER_REJETE', 'PRESELECTIONNE', 'PAIEMENT_EN_ATTENTE',
  'PAIEMENT_CONFIRME', 'CONVOQUE_TEST', 'TEST_REUSSI', 'TEST_ECHOUE', 'ABSENT_TEST', 'CONVOQUE_ENTRETIEN',
  'ENTRETIEN_EFFECTUE', 'ABSENT_ENTRETIEN', 'ADMIS', 'LISTE_ATTENTE', 'NON_ADMIS', 'INSCRIPTION_FINALISEE', 'DESISTEMENT',
];

describe('statuts-admission — la lecture des statuts par l\'admission', () => {
  it('connaît exactement les statuts du serveur', () => {
    expect([...STATUTS_CANDIDATURE].sort()).toEqual([...STATUTS_SERVEUR].sort());
  });

  it('donne un libellé lisible à chaque statut — jamais le nom technique', () => {
    for (const statut of STATUTS_SERVEUR) {
      const libelle = libelleStatut(statut);
      expect(libelle, statut).not.toBe(statut);
      expect(libelle, statut).not.toMatch(/^[A-Z_]+$/);
    }
    expect(libelleStatut('CONVOQUE_TEST')).toBe('Convoqué au test');
    expect(libelleStatut('INCOMPLET')).toBe('À compléter');
  });

  /** L'ancien vocabulaire ne désigne plus rien : il s'affiche tel quel, signe d'un défaut à corriger, sans casser l'écran. */
  it('laisse passer un statut inconnu tel quel', () => {
    expect(libelleStatut('ADMIS3')).toBe('ADMIS3');
    expect(libelleStatut('REJETE')).toBe('REJETE');
    expect(libelleStatut(null)).toBe('');
    expect(groupeDeStatut('ADMIS3')).toBeNull();
    expect(classeStatut('ADMIS3')).toContain('bg-surface-container');
  });

  it('range chaque statut (hors brouillon) dans un groupe, et tous les groupes servent', () => {
    for (const statut of STATUTS_SERVEUR.filter((s) => s !== 'BROUILLON')) {
      expect(groupeDeStatut(statut), statut).not.toBeNull();
    }
    expect(groupeDeStatut('BROUILLON')).toBeNull();
    for (const groupe of GROUPES_STATUT) {
      expect(statutsDuGroupe(groupe.cle).length, groupe.cle).toBeGreaterThan(0);
    }
  });

  it('regroupe les statuts comme les onglets de la liste', () => {
    expect(statutsDuGroupe('A_EXAMINER')).toEqual(['SOUMIS', 'DOSSIER_VALIDE']);
    expect(statutsDuGroupe('A_COMPLETER')).toEqual(['INCOMPLET']);
    expect(statutsDuGroupe('PRESELECTION')).toEqual(['PRESELECTIONNE', 'PAIEMENT_EN_ATTENTE', 'PAIEMENT_CONFIRME']);
    expect(statutsDuGroupe('TEST')).toEqual(['CONVOQUE_TEST', 'TEST_REUSSI', 'TEST_ECHOUE', 'ABSENT_TEST']);
    expect(statutsDuGroupe('ENTRETIEN')).toEqual(['CONVOQUE_ENTRETIEN', 'ENTRETIEN_EFFECTUE', 'ABSENT_ENTRETIEN']);
    expect(statutsDuGroupe('ADMIS')).toEqual(['ADMIS', 'INSCRIPTION_FINALISEE']);
    expect(statutsDuGroupe('ECARTE')).toEqual(['DOSSIER_REJETE', 'NON_ADMIS', 'DESISTEMENT']);
  });

  describe('indicateurs', () => {
    it('admis = admis et inscription finalisée', () => {
      expect(['ADMIS', 'INSCRIPTION_FINALISEE'].every(estAdmis)).toBe(true);
      expect(['ENTRETIEN_EFFECTUE', 'LISTE_ATTENTE', 'DOSSIER_REJETE'].some(estAdmis)).toBe(false);
    });

    it('refusé = dossier rejeté ou non admis ; un désistement est le choix du candidat', () => {
      expect(['DOSSIER_REJETE', 'NON_ADMIS'].every(estRefuse)).toBe(true);
      expect(estRefuse('DESISTEMENT')).toBe(false);
    });

    /** Le taux d'admission se calcule sur les décisions de l'INPG : ni la liste complémentaire ni un désistement n'en sont. */
    it('décidé = admis ou refusé', () => {
      for (const statut of STATUTS_SERVEUR) {
        expect(estDecide(statut), statut).toBe(['ADMIS', 'INSCRIPTION_FINALISEE', 'DOSSIER_REJETE', 'NON_ADMIS'].includes(statut));
      }
    });

    it('engagé dans la sélection = présélection, test ou entretien', () => {
      expect(estEngageDansLaSelection('PRESELECTIONNE')).toBe(true);
      expect(estEngageDansLaSelection('TEST_REUSSI')).toBe(true);
      expect(estEngageDansLaSelection('ENTRETIEN_EFFECTUE')).toBe(true);
      expect(estEngageDansLaSelection('SOUMIS')).toBe(false);
      expect(estEngageDansLaSelection('ADMIS')).toBe(false);
    });
  });

  describe('actions : tout vient du serveur', () => {
    const dossier = (statut: string, actionsPossibles: string[], extra: object = {}) => ({ statut, actionsPossibles, ...extra });

    it('le geste suivant est la première action qui n\'est pas un rejet', () => {
      expect(actionSuivante(dossier('SOUMIS', ['PRESELECTIONNER', 'REJETER']))).toEqual({ action: 'PRESELECTIONNER', libelle: 'Valider présélection' });
      expect(actionSuivante(dossier('SOUMIS', ['DEMANDER_COMPLEMENT', 'REJETER']))?.libelle).toBe('Demander un complément');
      expect(actionSuivante(dossier('ENTRETIEN_EFFECTUE', ['ADMETTRE_FINAL', 'PLACER_LISTE_ATTENTE', 'REJETER']))?.action).toBe('ADMETTRE_FINAL');
    });

    it('un dossier dont la seule action est le rejet n\'a pas de geste suivant', () => {
      expect(actionSuivante(dossier('TEST_ECHOUE', ['REJETER']))).toBeNull();
      expect(actionSuivante(dossier('DOSSIER_REJETE', []))).toBeNull();
    });

    /** C'était le défaut central : à partir de la présélection, aucun bouton ne s'affichait. */
    it('chaque étape du parcours a son geste', () => {
      const attendus: Array<[string, string, string]> = [
        ['PRESELECTIONNE', 'ENREGISTRER_REGLEMENT', 'Enregistrer le règlement'],
        ['PAIEMENT_CONFIRME', 'CONVOQUER_TEST', 'Convoquer au test'],
        ['CONVOQUE_TEST', 'ENREGISTRER_RESULTAT_TEST', 'Résultat du test'],
        ['TEST_REUSSI', 'ADMETTRE_ENTRETIEN', "Convoquer à l'entretien"],
        ['CONVOQUE_ENTRETIEN', 'ENREGISTRER_RESULTAT_ENTRETIEN', "Résultat de l'entretien"],
        ['ADMIS', 'FINALISER_INSCRIPTION', "Finaliser l'inscription"],
      ];
      for (const [statut, action, libelle] of attendus) {
        expect(actionSuivante(dossier(statut, [action]))?.libelle, statut).toBe(libelle);
      }
    });

    it('un absent est reconvoqué, pas convoqué', () => {
      expect(libelleAction('CONVOQUER_TEST', 'ABSENT_TEST')).toBe('Reconvoquer au test');
      expect(libelleAction('ADMETTRE_ENTRETIEN', 'ABSENT_ENTRETIEN')).toBe("Reconvoquer à l'entretien");
    });

    it('rejeter et annuler dépendent de ce que dit le serveur', () => {
      expect(peutRejeter(dossier('SOUMIS', ['PRESELECTIONNER', 'REJETER']))).toBe(true);
      expect(peutRejeter(dossier('CONVOQUE_TEST', ['ENREGISTRER_RESULTAT_TEST']))).toBe(false);
      expect(peutAnnuler(dossier('PRESELECTIONNE', [], { statutPrecedent: 'SOUMIS' }))).toBe(true);
      expect(peutAnnuler(dossier('PRESELECTIONNE', []))).toBe(false);
      expect(peutAnnuler(dossier('INSCRIPTION_FINALISEE', [], { statutPrecedent: 'ADMIS' }))).toBe(false);
    });

    it('sans réponse du serveur, aucune action n\'est inventée', () => {
      expect(actionSuivante({ statut: 'SOUMIS' })).toBeNull();
      expect(peutRejeter({ statut: 'SOUMIS' })).toBe(false);
      expect(reculsPossibles({ statut: 'SOUMIS' })).toEqual([]);
    });
  });
});
