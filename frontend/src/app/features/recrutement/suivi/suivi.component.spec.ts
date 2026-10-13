import { PhaseSuivi, phaseDeStatut } from '../parcours-candidature';
import { SuiviComponent } from './suivi.component';

/** Les 20 valeurs de `EnumStatutCandidature` (backend), dans l'ordre du parcours. */
const STATUTS_BACKEND = [
  'BROUILLON', 'SOUMIS', 'INCOMPLET', 'DOSSIER_VALIDE', 'DOSSIER_REJETE', 'PRESELECTIONNE', 'PAIEMENT_EN_ATTENTE',
  'PAIEMENT_CONFIRME', 'CONVOQUE_TEST', 'TEST_REUSSI', 'TEST_ECHOUE', 'ABSENT_TEST', 'CONVOQUE_ENTRETIEN',
  'ENTRETIEN_EFFECTUE', 'ABSENT_ENTRETIEN', 'ADMIS', 'LISTE_ATTENTE', 'NON_ADMIS', 'INSCRIPTION_FINALISEE', 'DESISTEMENT',
];

function suivi(statut: string, paiement: 'SUCCES' | 'EN_ATTENTE' | null = null): SuiviComponent {
  const c = new SuiviComponent({} as never, {} as never);
  c.candidature = { id: 1, statut, paiement: paiement ? { statut: paiement } : null } as never;
  return c;
}

/** Libellé de l'étape en cours, et des étapes en échec / franchies. */
function frise(c: SuiviComponent): { actives: string[]; echecs: string[]; faites: string[] } {
  const items = c.timelineItems;
  return {
    actives: items.filter((i) => i.active).map((i) => i.label),
    echecs: items.filter((i) => i.erreur).map((i) => i.label),
    faites: items.filter((i) => !i.pending && !i.active && !i.erreur).map((i) => i.label),
  };
}

describe('SuiviComponent — le candidat voit toujours quelque chose', () => {
  /**
   * La régression d'origine : après la refonte du workflow (22/09), 13 statuts sur 14 ne
   * correspondaient à aucun gabarit, et le candidat ne voyait plus que le menu et le pied de page.
   */
  it('chaque statut du backend (sauf le brouillon) a une phase, donc un gabarit', () => {
    for (const statut of STATUTS_BACKEND.filter((s) => s !== 'BROUILLON')) {
      expect(phaseDeStatut(statut), statut).not.toBeNull();
      expect(suivi(statut).isStatut(...(['SOUMIS', 'INCOMPLET', 'PRESELECTION', 'ADMIS1', 'ADMIS2', 'ADMIS3', 'LISTE_ATTENTE', 'REJETE'] as PhaseSuivi[])), statut).toBe(true);
    }
  });

  it('un brouillon, ou aucun dossier, retombe sur l\'écran d\'accueil', () => {
    expect(phaseDeStatut('BROUILLON')).toBeNull();
    expect(phaseDeStatut(null)).toBeNull();
    expect(suivi('BROUILLON').phase).toBeNull();
  });

  it('un statut inconnu (ajouté au backend plus tard) dégrade l\'écran sans le vider', () => {
    expect(phaseDeStatut('STATUT_FUTUR')).toBe('SOUMIS');
  });

  it('reprend la correspondance de la migration SQL du 21/09', () => {
    expect(phaseDeStatut('PRESELECTIONNE')).toBe('PRESELECTION');
    expect(phaseDeStatut('CONVOQUE_TEST')).toBe('ADMIS1');
    expect(phaseDeStatut('CONVOQUE_ENTRETIEN')).toBe('ADMIS2');
    expect(phaseDeStatut('ADMIS')).toBe('ADMIS3');
  });

  it('un dossier présélectionné ne dit pas « en cours d\'évaluation » et ne propose aucun paiement en ligne', () => {
    const c = suivi('PRESELECTIONNE');
    expect(c.entete.titre).toContain('présélectionné');
    expect(c.entete.texte).toContain('service des admissions');
    // Le paiement en ligne n'existe pas encore : pas d'étape « Paiement » côté candidat.
    expect(c.timelineItems.map((i) => i.label)).toEqual(['Dossier', 'Présélection', 'Test Académique', 'Entretien']);
    expect(frise(c).actives).toEqual(['Test Académique']);
  });

  it('après la confirmation du règlement par l\'admission, la frise du candidat est au test académique', () => {
    const c = suivi('PAIEMENT_CONFIRME', 'SUCCES');
    expect(frise(c).actives).toEqual(['Test Académique']);
    expect(frise(c).faites).toEqual(['Dossier', 'Présélection']);
  });

  it('un dossier à compléter est signalé et son étape « Dossier » reste ouverte', () => {
    const c = suivi('INCOMPLET');
    expect(c.entete.badge).toBe('Dossier à compléter');
    expect(c.entete.texte).toContain('À fournir');
    expect(frise(c).actives).toEqual(['Dossier']);
  });

  it('l\'échec est marqué à l\'étape réelle où le parcours s\'est arrêté', () => {
    expect(frise(suivi('DOSSIER_REJETE')).echecs).toEqual(['Présélection']);
    expect(frise(suivi('TEST_ECHOUE', 'SUCCES')).echecs).toEqual(['Test Académique']);
    expect(frise(suivi('ABSENT_TEST', 'SUCCES')).echecs).toEqual(['Test Académique']);
    expect(frise(suivi('NON_ADMIS', 'SUCCES')).echecs).toEqual(['Entretien']);
    expect(frise(suivi('ABSENT_ENTRETIEN', 'SUCCES')).echecs).toEqual(['Entretien']);
  });

  it('un refus avant le test ne parle pas de test, et n\'affiche pas le bilan du test', () => {
    const refus = suivi('DOSSIER_REJETE');
    expect(refus.entete.badge).not.toContain('Test');
    expect(refus.afficherBilanTest).toBe(false);

    const echec = suivi('TEST_ECHOUE', 'SUCCES');
    expect(echec.entete.badge).toBe('Test non concluant');
    expect(echec.afficherBilanTest).toBe(true);
  });

  it('la liste complémentaire a son propre en-tête, et un parcours terminé', () => {
    const c = suivi('LISTE_ATTENTE', 'SUCCES');
    expect(c.entete.titre).toContain('liste complémentaire');
    expect(frise(c).faites).toEqual(['Dossier', 'Présélection', 'Test Académique', 'Entretien']);
    expect(c.etapeActuelle).toBe('Étape 4 sur 4');
  });

  it('l\'admis voit les cinq étapes franchies', () => {
    const c = suivi('ADMIS', 'SUCCES');
    expect(frise(c).faites).toHaveLength(4);
    expect(c.etapeActuelle).toBe('Étape 4 sur 4');
  });
});

/** Un dossier tel que le serveur le renvoie : pièces déposées + documents attendus qu'il a lui-même calculés. */
type Etat = 'FOURNI' | 'A_FOURNIR' | 'A_REMPLACER';
function dossier(
  attendus: Array<[string, string, Etat]>,
  pieces: Array<Partial<{ typeDocument: string; statutVerification: string; fichierNomOriginal: string; moyenneDeclaree: number; motifNonConformite: string }>> = [],
  statut = 'SOUMIS',
): SuiviComponent {
  const c = new SuiviComponent({} as never, {} as never);
  c.candidature = {
    id: 1,
    statut,
    paiement: null,
    documentsAttendus: attendus.map(([type, libelle, etat]) => ({ type, libelle, etat })),
    piecesJustificatives: pieces,
  } as never;
  return c;
}

const TOUS_FOURNIS: Array<[string, string, Etat]> = [
  ['PIECE_IDENTITE', "Pièce d'identité", 'FOURNI'],
  ['CV', 'CV', 'FOURNI'],
  ['RELEVE_NOTES', 'Relevé de notes', 'FOURNI'],
  ['DIPLOME', 'Diplôme', 'FOURNI'],
];

describe('SuiviComponent — la liste des documents vient du serveur', () => {
  /**
   * La liste était écrite en dur dans le composant, avec trois types : le diplôme que la relance par e-mail réclamait
   * n'y figurait pas, et le candidat voyait « à jour » un dossier que l'admission jugeait incomplet.
   */
  it('affiche exactement les documents que le serveur exige, diplôme compris', () => {
    const c = dossier(TOUS_FOURNIS, [{ typeDocument: 'DIPLOME', statutVerification: 'A_VERIFIER', fichierNomOriginal: 'licence.pdf' }]);

    expect(c.checklistDocuments.map((d) => d.type)).toEqual(['PIECE_IDENTITE', 'CV', 'RELEVE_NOTES', 'DIPLOME']);
  });

  it('n\'affiche pas de diplôme à un candidat que le serveur en dispense', () => {
    const c = dossier(TOUS_FOURNIS.slice(0, 3));

    expect(c.checklistDocuments.map((d) => d.type)).toEqual(['PIECE_IDENTITE', 'CV', 'RELEVE_NOTES']);
  });

  it('un document absent est « à fournir », et le diplôme manquant est compté', () => {
    const c = dossier([...TOUS_FOURNIS.slice(0, 3), ['DIPLOME', 'Diplôme', 'A_FOURNIR']]);

    expect(c.checklistDocuments.find((d) => d.type === 'DIPLOME')!.statut).toBe('A_FOURNIR');
    expect(c.documentsAFournir).toBe(1);
  });

  it('un document déposé attend son contrôle, puis devient « validé » une fois toutes ses pièces conformes', () => {
    const attendu: Array<[string, string, Etat]> = [['CV', 'CV', 'FOURNI']];

    expect(dossier(attendu, [{ typeDocument: 'CV', statutVerification: 'A_VERIFIER', fichierNomOriginal: 'cv.pdf' }])
      .checklistDocuments[0].statut).toBe('A_VERIFIER');
    expect(dossier(attendu, [{ typeDocument: 'CV', statutVerification: 'CONFORME', fichierNomOriginal: 'cv.pdf' }])
      .checklistDocuments[0].statut).toBe('CONFORME');
  });

  it('un relevé refusé parmi plusieurs reste « à remplacer », avec le motif de l\'agent', () => {
    const c = dossier([['RELEVE_NOTES', 'Relevé de notes', 'A_REMPLACER']], [
      { typeDocument: 'RELEVE_NOTES', statutVerification: 'CONFORME', fichierNomOriginal: 'a.pdf' },
      { typeDocument: 'RELEVE_NOTES', statutVerification: 'A_REMPLACER', fichierNomOriginal: 'b.pdf', motifNonConformite: 'Illisible' },
    ]);

    const releve = c.checklistDocuments[0];
    expect(releve.statut).toBe('A_REMPLACER');
    expect(releve.motifNonConformite).toBe('Illisible');
    expect(releve.nbFichiers).toBe(2);
  });

  it('plusieurs relevés déposés : le nombre de fichiers est dit, la moyenne n\'est donnée que pour un seul', () => {
    const attendu: Array<[string, string, Etat]> = [['RELEVE_NOTES', 'Relevé de notes', 'FOURNI']];
    const unSeul = dossier(attendu, [{ typeDocument: 'RELEVE_NOTES', statutVerification: 'A_VERIFIER', moyenneDeclaree: 14 }]);
    const trois = dossier(attendu, [1, 2, 3].map((n) => ({ typeDocument: 'RELEVE_NOTES', statutVerification: 'A_VERIFIER', moyenneDeclaree: 10 + n })));

    expect(unSeul.checklistDocuments[0].moyenneDeclaree).toBe(14);
    expect(trois.checklistDocuments[0].nbFichiers).toBe(3);
    expect(trois.checklistDocuments[0].moyenneDeclaree).toBeNull();
  });

  it('sans réponse du serveur sur les documents, rien n\'est affirmé', () => {
    const c = new SuiviComponent({} as never, {} as never);
    c.candidature = { id: 1, statut: 'SOUMIS', piecesJustificatives: [] } as never;

    expect(c.checklistDocuments).toEqual([]);
    expect(c.documentsAFournir).toBeNull();
  });
});

describe('SuiviComponent — la frise ne contredit pas la liste', () => {
  it('dit combien de documents manquent au lieu d\'affirmer que tout est téléchargé', () => {
    const c = dossier([...TOUS_FOURNIS.slice(0, 2), ['RELEVE_NOTES', 'Relevé de notes', 'A_FOURNIR'], ['DIPLOME', 'Diplôme', 'A_REMPLACER']]);

    expect(c.timelineItems[0].desc).toBe('2 documents à fournir ou à remplacer.');
  });

  it('confirme « tous les documents » quand le serveur ne signale plus rien', () => {
    expect(dossier(TOUS_FOURNIS).timelineItems[0].desc).toBe('Tous les documents requis téléchargés.');
  });
});

describe('SuiviComponent — ajout d\'un document depuis le suivi', () => {
  /** Sans description, le serveur refuse un diplôme : le bouton restait actif et le candidat ne voyait pas pourquoi. */
  it('un diplôme ne peut partir qu\'avec sa description', () => {
    const c = dossier(TOUS_FOURNIS);
    c.preparerAjout('DIPLOME');
    expect(c.ajoutPossible).toBe(false);

    c.descriptionAAjouter = '   ';
    expect(c.ajoutPossible).toBe(false);

    c.descriptionAAjouter = 'Licence en Informatique, 2022';
    expect(c.ajoutPossible).toBe(true);
  });

  it('les autres documents n\'exigent aucune description, et rien ne part sans type choisi', () => {
    const c = dossier(TOUS_FOURNIS);
    expect(c.ajoutPossible).toBe(false);

    c.preparerAjout('CV');
    expect(c.ajoutPossible).toBe(true);
  });
});

describe('SuiviComponent — l\'en-tête ne contredit pas la liste de documents', () => {
  it('un dossier déposé auquel il manque des pièces ne reçoit pas de « félicitations » ni d\'« examen en cours »', () => {
    const c = dossier([...TOUS_FOURNIS.slice(0, 3), ['DIPLOME', 'Diplôme', 'A_FOURNIR']]);

    expect(c.entete.badge).toBe('Documents manquants');
    expect(c.entete.texte).toContain('1 document ne nous est pas parvenu');
    expect(c.entete.texte).not.toContain('Félicitations');
  });

  it('accorde le pluriel quand plusieurs documents manquent', () => {
    const c = dossier([['CV', 'CV', 'A_FOURNIR'], ['DIPLOME', 'Diplôme', 'A_REMPLACER']]);

    expect(c.entete.texte).toContain('2 documents ne nous sont pas parvenus ou sont à remplacer');
  });

  it('un dossier complet garde l\'en-tête d\'examen en cours', () => {
    expect(dossier(TOUS_FOURNIS).entete.badge).toBe('Candidature en cours');
  });
});
