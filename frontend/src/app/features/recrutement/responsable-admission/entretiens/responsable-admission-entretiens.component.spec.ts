import { FormBuilder } from '@angular/forms';
import { CandidatureResponse } from '../../../../core/recrutement/services/recrutement.service';
import { ResponsableAdmissionEntretiensComponent } from './responsable-admission-entretiens.component';

describe('ResponsableAdmissionEntretiensComponent — candidats planifiables', () => {
  /** Seuls les candidats convoqués à l'entretien se planifient : l'ancien « ADMIS2 » n'existe plus côté serveur. */
  it('propose les candidats convoqués à l\'entretien, et eux seuls', () => {
    const c = new ResponsableAdmissionEntretiensComponent({} as never, new FormBuilder(), {} as never, {} as never);
    c.candidatures = ['SOUMIS', 'TEST_REUSSI', 'CONVOQUE_ENTRETIEN', 'ENTRETIEN_EFFECTUE', 'ADMIS']
      .map((statut, i) => ({ id: i, candidatId: i, statut }) as unknown as CandidatureResponse);

    expect(c.candidatsEligibles.map((x) => x.statut)).toEqual(['CONVOQUE_ENTRETIEN']);
  });
});
