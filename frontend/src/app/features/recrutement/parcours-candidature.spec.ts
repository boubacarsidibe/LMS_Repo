import { depotPossibleApres, friseCandidature } from './parcours-candidature';

const etats = (statut: string, pdv: 'candidat' | 'admission') =>
  friseCandidature(statut, pdv).map((e) => `${e.label}:${e.erreur ? 'echec' : e.active ? 'en cours' : e.pending ? 'a venir' : 'fait'}`);

describe('friseCandidature', () => {
  it('le candidat n\'a jamais d\'étape de paiement : le paiement en ligne n\'est pas en service', () => {
    for (const statut of ['SOUMIS', 'PRESELECTIONNE', 'PAIEMENT_CONFIRME', 'CONVOQUE_TEST', 'ADMIS', 'NON_ADMIS']) {
      expect(friseCandidature(statut, 'candidat').map((e) => e.label), statut)
        .toEqual(['Dossier', 'Présélection', 'Test Académique', 'Entretien']);
    }
  });

  it('l\'admission voit l\'étape « Paiement », qu\'elle renseigne elle-même pour un règlement hors plateforme', () => {
    expect(friseCandidature('SOUMIS', 'admission').map((e) => e.label))
      .toEqual(['Dossier', 'Présélection', 'Paiement', 'Test Académique', 'Entretien']);
  });

  it('le règlement est attendu dès la présélection, puis acquis', () => {
    expect(etats('SOUMIS', 'admission')).toEqual(['Dossier:fait', 'Présélection:en cours', 'Paiement:a venir', 'Test Académique:a venir', 'Entretien:a venir']);
    expect(etats('PRESELECTIONNE', 'admission')).toEqual(['Dossier:fait', 'Présélection:fait', 'Paiement:en cours', 'Test Académique:a venir', 'Entretien:a venir']);
    expect(etats('PAIEMENT_EN_ATTENTE', 'admission')[2]).toBe('Paiement:en cours');
    expect(etats('PAIEMENT_CONFIRME', 'admission')).toEqual(['Dossier:fait', 'Présélection:fait', 'Paiement:fait', 'Test Académique:en cours', 'Entretien:a venir']);
    expect(etats('ADMIS', 'admission')).toEqual(['Dossier:fait', 'Présélection:fait', 'Paiement:fait', 'Test Académique:fait', 'Entretien:fait']);
  });

  it('un refus de dossier laisse le paiement à venir ; un échec au test le laisse acquis', () => {
    expect(etats('DOSSIER_REJETE', 'admission')).toEqual(['Dossier:fait', 'Présélection:echec', 'Paiement:a venir', 'Test Académique:a venir', 'Entretien:a venir']);
    expect(etats('TEST_ECHOUE', 'admission')).toEqual(['Dossier:fait', 'Présélection:fait', 'Paiement:fait', 'Test Académique:echec', 'Entretien:a venir']);
  });
});

describe('friseCandidature — description de l\'étape « Dossier »', () => {
  const dossier = (documentsAFournir: number | null) => friseCandidature('SOUMIS', 'candidat', documentsAFournir)[0].desc;

  it('ne dit « tous les documents téléchargés » que si le serveur confirme que rien ne manque', () => {
    expect(dossier(0)).toBe('Tous les documents requis téléchargés.');
  });

  it('dit combien de documents manquent plutôt que de prétendre le contraire', () => {
    expect(dossier(1)).toBe('1 document à fournir ou à remplacer.');
    expect(dossier(3)).toBe('3 documents à fournir ou à remplacer.');
    expect(friseCandidature('SOUMIS', 'admission', 2)[0].desc).toBe('2 documents à fournir ou à remplacer par le candidat.');
  });

  it('reste neutre quand le contenu du dossier est inconnu', () => {
    expect(dossier(null)).toBe('Dossier déposé.');
    expect(friseCandidature('SOUMIS', 'candidat')[0].desc).toBe('Dossier déposé.');
  });
});

describe('depotPossibleApres', () => {
  it('autorise un nouveau dépôt après un brouillon, un refus de dossier ou une non-admission', () => {
    for (const statut of ['BROUILLON', 'DOSSIER_REJETE', 'NON_ADMIS']) {
      expect(depotPossibleApres(statut), statut).toBe(true);
    }
  });

  /** « REJETE » n'est plus un statut du serveur : le tester laissait croire qu'un refus renvoyait au formulaire. */
  it('ne retient pas l\'ancien nom « REJETE », et verrouille tout dossier en cours de route', () => {
    for (const statut of ['REJETE', 'SOUMIS', 'INCOMPLET', 'PRESELECTIONNE', 'CONVOQUE_TEST', 'ADMIS', 'LISTE_ATTENTE', 'DESISTEMENT']) {
      expect(depotPossibleApres(statut), statut).toBe(false);
    }
  });
});
