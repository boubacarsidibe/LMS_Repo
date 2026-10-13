import { CandidatureResponse } from '../../../../core/recrutement/services/recrutement.service';
import { ResponsableAdmissionCandidaturesComponent } from './responsable-admission-candidatures.component';

/**
 * L'écran « Candidatures » lisait l'ancien vocabulaire (ADMIS1/2/3, EN_COURS_DE_PRESELECTION, REJETE) que le serveur
 * n'émet plus : les onglets restaient vides, les libellés sortaient en nom technique et, à partir de la présélection,
 * aucun bouton ne s'affichait.
 */
describe('ResponsableAdmissionCandidaturesComponent — statuts réels du serveur', () => {
  let n = 0;
  const dossier = (statut: string, extra: Partial<CandidatureResponse> = {}): CandidatureResponse => ({
    id: ++n, numeroDossier: `INPG-${n}`, statut, sessionCandidatureId: 1, sessionCandidatureLibelle: 'S1',
    candidatNom: 'Diop', candidatPrenom: 'Awa', candidatEmail: 'a@x.sn', piecesJustificatives: [], documentsAttendus: [],
    actionsPossibles: [], reculsPossibles: [], ...extra,
  } as unknown as CandidatureResponse);

  function ecran(candidatures: CandidatureResponse[]): ResponsableAdmissionCandidaturesComponent {
    const c = new ResponsableAdmissionCandidaturesComponent({} as never, {} as never, {} as never, {} as never);
    c.candidatures = candidatures;
    c.recalculerFiltres();
    return c;
  }

  const TOUS_LES_STATUTS = [
    'SOUMIS', 'INCOMPLET', 'DOSSIER_VALIDE', 'PRESELECTIONNE', 'PAIEMENT_EN_ATTENTE', 'PAIEMENT_CONFIRME', 'CONVOQUE_TEST',
    'TEST_REUSSI', 'TEST_ECHOUE', 'ABSENT_TEST', 'CONVOQUE_ENTRETIEN', 'ENTRETIEN_EFFECTUE', 'ABSENT_ENTRETIEN', 'ADMIS',
    'LISTE_ATTENTE', 'INSCRIPTION_FINALISEE', 'DOSSIER_REJETE', 'NON_ADMIS', 'DESISTEMENT',
  ];

  it('chaque dossier tombe dans un onglet : la somme des onglets est le total', () => {
    const c = ecran(TOUS_LES_STATUTS.map((s) => dossier(s)));

    const somme = c.onglets.filter((o) => o.statut !== 'TOUS').reduce((s, o) => s + c.compter(o.statut), 0);

    expect(c.compter('TOUS')).toBe(TOUS_LES_STATUTS.length);
    expect(somme).toBe(TOUS_LES_STATUTS.length);
  });

  it('filtre par onglet : un onglet regroupe les statuts de son étape', () => {
    const c = ecran(TOUS_LES_STATUTS.map((s) => dossier(s)));

    c.setFiltreStatut('TEST');
    expect(c.candidaturesFiltrees.map((d) => d.statut)).toEqual(['CONVOQUE_TEST', 'TEST_REUSSI', 'TEST_ECHOUE', 'ABSENT_TEST']);

    c.setFiltreStatut('ECARTE');
    expect(c.candidaturesFiltrees.map((d) => d.statut)).toEqual(['DOSSIER_REJETE', 'NON_ADMIS', 'DESISTEMENT']);

    c.setFiltreStatut('A_COMPLETER');
    expect(c.candidaturesFiltrees.map((d) => d.statut)).toEqual(['INCOMPLET']);
  });

  it('donne un libellé lisible, jamais le nom technique', () => {
    const c = ecran([]);
    expect(c.libelleStatut('CONVOQUE_TEST')).toBe('Convoqué au test');
    expect(c.libelleStatut('DOSSIER_REJETE')).toBe('Dossier rejeté');
  });

  /** Le défaut central : à partir de la présélection, aucun bouton ne s'affichait. */
  it('propose à chaque étape le geste que le serveur autorise', () => {
    const c = ecran([]);
    expect(c.actionSuivante(dossier('SOUMIS', { actionsPossibles: ['PRESELECTIONNER', 'REJETER'] }))?.libelle).toBe('Valider présélection');
    expect(c.actionSuivante(dossier('SOUMIS', { actionsPossibles: ['DEMANDER_COMPLEMENT', 'REJETER'] }))?.libelle).toBe('Demander un complément');
    expect(c.actionSuivante(dossier('PRESELECTIONNE', { actionsPossibles: ['ENREGISTRER_REGLEMENT', 'REJETER'] }))?.libelle).toBe('Enregistrer le règlement');
    expect(c.actionSuivante(dossier('PAIEMENT_CONFIRME', { actionsPossibles: ['CONVOQUER_TEST'] }))?.libelle).toBe('Convoquer au test');
    expect(c.actionSuivante(dossier('CONVOQUE_TEST', { actionsPossibles: ['ENREGISTRER_RESULTAT_TEST'] }))?.libelle).toBe('Résultat du test');
    expect(c.actionSuivante(dossier('ENTRETIEN_EFFECTUE', { actionsPossibles: ['ADMETTRE_FINAL', 'PLACER_LISTE_ATTENTE', 'REJETER'] }))?.libelle).toBe('Admettre');
    expect(c.actionSuivante(dossier('DOSSIER_REJETE'))).toBeNull();
  });

  it('rejeter et annuler suivent le serveur, pas une liste de statuts recopiée', () => {
    const c = ecran([]);
    expect(c.peutRejeter(dossier('TEST_REUSSI', { actionsPossibles: ['ADMETTRE_ENTRETIEN', 'REJETER'] }))).toBe(true);
    expect(c.peutRejeter(dossier('CONVOQUE_TEST', { actionsPossibles: ['ENREGISTRER_RESULTAT_TEST'] }))).toBe(false);
    expect(c.peutAnnuler(dossier('PRESELECTIONNE', { statutPrecedent: 'SOUMIS' as never }))).toBe(true);
    expect(c.peutAnnuler(dossier('INSCRIPTION_FINALISEE', { statutPrecedent: 'ADMIS' as never }))).toBe(false);
  });

  it('un geste qui demande un choix ouvre le dossier au lieu d\'agir à l\'aveugle', () => {
    const navigate = vi.fn();
    const c = new ResponsableAdmissionCandidaturesComponent({} as never, {} as never, { navigate } as never, {} as never);

    c.lancerActionSuivante(dossier('CONVOQUE_TEST', { id: 77, actionsPossibles: ['ENREGISTRER_RESULTAT_TEST'] }));
    expect(navigate).toHaveBeenCalledWith(['/responsable-admission/candidatures', 77]);

    c.lancerActionSuivante(dossier('PRESELECTIONNE', { actionsPossibles: ['ENREGISTRER_REGLEMENT'] }));
    expect(navigate).toHaveBeenCalledWith(['/responsable-admission/paiements']);
  });

  it('les étapes de recul viennent du serveur, regroupées sur la sélection', () => {
    const a = dossier('PRESELECTIONNE', { reculsPossibles: ['SOUMIS', 'INCOMPLET'] });
    const b = dossier('CONVOQUE_TEST', { reculsPossibles: ['SOUMIS', 'INCOMPLET', 'PRESELECTIONNE'] });
    const autre = dossier('SOUMIS', { reculsPossibles: [] });
    const c = ecran([a, b, autre]);
    c.selection = new Set([a.id, b.id, autre.id]);

    expect(c.ciblesReculerSelection).toEqual([
      { valeur: 'SOUMIS', libelle: 'Soumis', count: 2 },
      { valeur: 'INCOMPLET', libelle: 'À compléter', count: 2 },
      { valeur: 'PRESELECTIONNE', libelle: 'Présélectionné', count: 1 },
    ]);
  });

  /** Le compteur de la relance doit dire ce que la relance enverra, pas « tout dossier à pièce non contrôlée ». */
  it('compte comme incomplets les dossiers relançables auxquels il manque un document exigé', () => {
    const manque = [{ type: 'CV', libelle: 'CV', etat: 'A_FOURNIR' }];
    const complet = [{ type: 'CV', libelle: 'CV', etat: 'FOURNI' }];
    const c = ecran([
      dossier('SOUMIS', { documentsAttendus: manque as never }),
      dossier('INCOMPLET', { documentsAttendus: manque as never }),
      dossier('SOUMIS', { documentsAttendus: complet as never }),
      dossier('ADMIS', { documentsAttendus: manque as never }),     // décidé : plus relançable
      dossier('DOSSIER_REJETE', { documentsAttendus: manque as never }),
    ]);

    expect(c.nombreDossiersIncomplets).toBe(2);
  });
});
