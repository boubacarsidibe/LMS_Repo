import { CandidatureResponse } from '../../../../core/recrutement/services/recrutement.service';
import { ResponsableAdmissionPaiementsComponent } from './responsable-admission-paiements.component';

describe('ResponsableAdmissionPaiementsComponent — règlements attendus', () => {
  /** Un règlement s'attend de la présélection au paiement confirmé : les anciens ADMIS1/2/3 n'existent plus. */
  it('liste les dossiers présélectionnés dont les frais ne sont pas réglés', () => {
    const c = new ResponsableAdmissionPaiementsComponent({} as never, {} as never);
    const dossier = (statut: string, paiement: string | null) =>
      ({ id: 1, statut, paiement: paiement ? { statut: paiement } : null }) as unknown as CandidatureResponse;
    c.candidatures = [
      dossier('SOUMIS', null), dossier('PRESELECTIONNE', null), dossier('PAIEMENT_EN_ATTENTE', 'EN_ATTENTE'),
      dossier('PAIEMENT_CONFIRME', 'SUCCES'), dossier('CONVOQUE_TEST', 'SUCCES'), dossier('ADMIS', 'SUCCES'),
    ];

    expect(c.candidaturesImpayees.map((x) => x.statut)).toEqual(['PRESELECTIONNE', 'PAIEMENT_EN_ATTENTE']);
  });
});
