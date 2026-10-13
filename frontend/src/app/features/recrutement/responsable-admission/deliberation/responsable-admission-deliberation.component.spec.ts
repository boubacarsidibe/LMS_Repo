import { CandidatureResponse } from '../../../../core/recrutement/services/recrutement.service';
import { ResponsableAdmissionDeliberationComponent } from './responsable-admission-deliberation.component';

/**
 * La délibération attendait des dossiers « ADMIS2 » : un statut que le serveur n'émet plus. Le candidat n'y apparaissait
 * jamais après son entretien.
 */
describe('ResponsableAdmissionDeliberationComponent — statuts réels du serveur', () => {
  let n = 0;
  const dossier = (statut: string, actionsPossibles: string[] = []): CandidatureResponse => ({
    id: ++n, candidatId: n, numeroDossier: `INPG-${n}`, statut, candidatNom: 'Diop', candidatPrenom: 'Awa', actionsPossibles,
  } as unknown as CandidatureResponse);

  function ecran(candidatures: CandidatureResponse[]): ResponsableAdmissionDeliberationComponent {
    const c = new ResponsableAdmissionDeliberationComponent({} as never, {} as never);
    c.candidatures = candidatures;
    return c;
  }

  const TOUS = ['SOUMIS', 'INCOMPLET', 'PRESELECTIONNE', 'CONVOQUE_TEST', 'TEST_REUSSI', 'CONVOQUE_ENTRETIEN', 'ENTRETIEN_EFFECTUE',
    'ADMIS', 'INSCRIPTION_FINALISEE', 'LISTE_ATTENTE', 'NON_ADMIS', 'DOSSIER_REJETE', 'DESISTEMENT'];

  it('ne retient que les dossiers que le jury a vus ou doit trancher', () => {
    const c = ecran(TOUS.map((s) => dossier(s)));

    expect(c.lignes.map((l) => l.candidature.statut).sort()).toEqual(
      ['ADMIS', 'ENTRETIEN_EFFECTUE', 'INSCRIPTION_FINALISEE', 'LISTE_ATTENTE', 'NON_ADMIS']);
  });

  it('un candidat dont l\'entretien vient d\'avoir lieu attend la décision du jury', () => {
    const c = ecran(TOUS.map((s) => dossier(s)));

    expect(c.nbEnAttente).toBe(1);
    expect(c.nbAdmis).toBe(2);
    expect(c.nbListeAttente).toBe(1);
    expect(c.nbRejetes).toBe(1);
    c.filtre = 'EN_ATTENTE';
    expect(c.lignesFiltrees.map((l) => l.candidature.statut)).toEqual(['ENTRETIEN_EFFECTUE']);
  });

  it('un dossier rejeté dès l\'examen n\'apparaît pas : le jury ne l\'a jamais jugé', () => {
    const c = ecran([dossier('DOSSIER_REJETE'), dossier('NON_ADMIS')]);

    expect(c.lignes.map((l) => l.candidature.statut)).toEqual(['NON_ADMIS']);
  });

  it('les décisions possibles viennent du serveur', () => {
    const c = ecran([]);
    const enAttente = { candidature: dossier('ENTRETIEN_EFFECTUE', ['ADMETTRE_FINAL', 'PLACER_LISTE_ATTENTE', 'REJETER']) } as never;
    const surListe = { candidature: dossier('LISTE_ATTENTE', ['ADMETTRE_FINAL', 'REJETER']) } as never;
    const admis = { candidature: dossier('ADMIS', ['FINALISER_INSCRIPTION']) } as never;

    expect([c.peutAdmettre(enAttente), c.peutMettreEnListe(enAttente), c.peutRefuser(enAttente)]).toEqual([true, true, true]);
    expect([c.peutAdmettre(surListe), c.peutMettreEnListe(surListe), c.peutRefuser(surListe)]).toEqual([true, false, true]);
    expect(c.decisionEncorePossible(admis)).toBe(false);
  });

  it('libellés des pastilles', () => {
    const c = ecran([]);
    const ligne = (s: string) => ({ candidature: dossier(s) }) as never;

    expect(c.libelleDecision(ligne('ADMIS'))).toBe('Admis');
    expect(c.libelleDecision(ligne('INSCRIPTION_FINALISEE'))).toBe('Admis');
    expect(c.libelleDecision(ligne('NON_ADMIS'))).toBe('Non retenu');
    expect(c.libelleDecision(ligne('LISTE_ATTENTE'))).toBe('Liste complémentaire');
    expect(c.libelleDecision(ligne('ENTRETIEN_EFFECTUE'))).toBe('En attente');
  });
});
