import { CandidatureResponse } from '../../../../core/recrutement/services/recrutement.service';
import { ResponsableAdmissionStatistiquesComponent } from './responsable-admission-statistiques.component';
import { ResponsableAdmissionComponent } from '../overview/responsable-admission.component';

/**
 * L'entonnoir restait à zéro et le taux d'admission à 0 % : les compteurs cherchaient des statuts (ADMIS3, REJETE…)
 * que le serveur n'émet plus.
 */
describe('Vue d\'ensemble et statistiques — compteurs sur les statuts réels', () => {
  const dossiers = (statuts: string[]): CandidatureResponse[] =>
    statuts.map((statut, i) => ({ id: i + 1, statut }) as unknown as CandidatureResponse);

  const prefs = { charger: () => ({}) } as never;
  const stats = (statuts: string[]) => {
    const c = new ResponsableAdmissionStatistiquesComponent({} as never, prefs, {} as never);
    c.candidatures = dossiers(statuts);
    return c;
  };
  const vue = (statuts: string[]) => {
    const c = new ResponsableAdmissionComponent({} as never, prefs, {} as never);
    c.candidatures = dossiers(statuts);
    return c;
  };

  const PARCOURS = ['SOUMIS', 'SOUMIS', 'DOSSIER_VALIDE', 'INCOMPLET', 'PRESELECTIONNE', 'PAIEMENT_CONFIRME', 'CONVOQUE_TEST',
    'TEST_REUSSI', 'CONVOQUE_ENTRETIEN', 'ENTRETIEN_EFFECTUE', 'LISTE_ATTENTE', 'ADMIS', 'ADMIS', 'INSCRIPTION_FINALISEE',
    'DOSSIER_REJETE', 'NON_ADMIS', 'DESISTEMENT'];

  for (const [nom, fabrique] of [['statistiques', stats], ['vue d\'ensemble', vue]] as const) {
    it(`${nom} : les dossiers à examiner, engagés dans la sélection et décidés sont comptés`, () => {
      const c = fabrique(PARCOURS);

      expect(c.totalCandidats).toBe(17);
      expect(c.validationEnCours).toBe(3);        // 2 soumis + 1 dossier validé ; « à compléter » attend le candidat
      expect(c.preselectionnes).toBe(6);          // présélectionné, paiement confirmé, test (2), entretien (2)
      expect(c.candidaturesDecidees).toBe(5);     // 3 admis + dossier rejeté + non admis ; ni liste complémentaire ni désistement
    });

    it(`${nom} : le taux d'admission est celui des dossiers tranchés`, () => {
      expect(fabrique(PARCOURS).tauxAdmission).toBe(60);     // 3 admis sur 5 décisions
      expect(fabrique(['SOUMIS', 'PRESELECTIONNE']).tauxAdmission).toBe(0);
      expect(fabrique(['ADMIS']).tauxAdmission).toBe(100);
    });
  }

  it('statistiques : « rejetés » compte les refus de l\'INPG, pas les désistements', () => {
    expect(stats(PARCOURS).rejetes).toBe(2);
  });

  it('statistiques : l\'entonnoir répartit tous les dossiers par étape', () => {
    const repartition = stats(PARCOURS).repartitionStatuts;
    const compte = (cle: string) => repartition.find((s) => s.statut === cle)!.count;

    expect(compte('A_EXAMINER')).toBe(3);
    expect(compte('A_COMPLETER')).toBe(1);
    expect(compte('PRESELECTION')).toBe(2);
    expect(compte('TEST')).toBe(2);
    expect(compte('ENTRETIEN')).toBe(2);
    expect(compte('LISTE_ATTENTE')).toBe(1);
    expect(compte('ADMIS')).toBe(3);
    expect(compte('ECARTE')).toBe(3);
    expect(repartition.reduce((s, r) => s + r.count, 0)).toBe(17);
  });
});
