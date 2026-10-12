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
