import { CandidatureDetailComponent } from './candidature-detail.component';

/** La fiche de l'admission lit le même calcul de complétude que le suivi du candidat et la relance par e-mail. */
describe('CandidatureDetailComponent — documents et parcours du candidat', () => {
  type Etat = 'FOURNI' | 'A_FOURNIR' | 'A_REMPLACER';

  function fiche(documentsAttendus: Array<[string, string, Etat]> | undefined, extra: object = {}): CandidatureDetailComponent {
    const c = new CandidatureDetailComponent({} as never, {} as never, {} as never, {} as never);
    c.candidature = {
      id: 1, statut: 'SOUMIS', piecesJustificatives: [], paiement: null,
      documentsAttendus: documentsAttendus?.map(([type, libelle, etat]) => ({ type, libelle, etat })),
      ...extra,
    } as never;
    return c;
  }

  it('liste les documents à fournir ou à remplacer, et seulement eux', () => {
    const c = fiche([['CV', 'CV', 'FOURNI'], ['RELEVE_NOTES', 'Relevé de notes', 'A_FOURNIR'], ['DIPLOME', 'Diplôme', 'A_REMPLACER']]);

    expect(c.documentsAFournir).toEqual([
      { libelle: 'Relevé de notes', etat: 'A_FOURNIR' },
      { libelle: 'Diplôme', etat: 'A_REMPLACER' },
    ]);
    expect(c.dossierComplet).toBe(false);
  });

  it('un dossier dont tous les documents sont fournis est complet', () => {
    const c = fiche([['CV', 'CV', 'FOURNI'], ['DIPLOME', 'Diplôme', 'FOURNI']]);

    expect(c.documentsAFournir).toEqual([]);
    expect(c.dossierComplet).toBe(true);
  });

  /** Sans réponse du serveur sur les documents, on n'affirme pas que le dossier est complet. */
  it('ne déclare pas complet un dossier dont le serveur n\'a rien dit', () => {
    expect(fiche(undefined).dossierComplet).toBe(false);
    expect(fiche([]).dossierComplet).toBe(false);
  });

  it('la frise d\'admission annonce le nombre de documents manquants', () => {
    const c = fiche([['CV', 'CV', 'A_FOURNIR'], ['DIPLOME', 'Diplôme', 'A_FOURNIR']]);

    expect(c.timelineItems[0].desc).toBe('2 documents à fournir ou à remplacer par le candidat.');
  });

  it('relit le parcours déclaré et décrit une formation sans les champs vides', () => {
    const c = fiche([], {
      informationsAcademiques: JSON.stringify({
        niveauActuel: 'Licence (L3)', etablissement: 'UCAD',
        formationsSuivies: [{ nom: 'Soudage', etablissement: 'CFPT', diplome: '', anneeObtention: '2021', pays: '' }],
      }),
    });

    expect(c.parcoursDeclare.champs.map((x) => x.valeur)).toEqual(['Licence (L3)', 'UCAD']);
    expect(c.detailsFormation(c.parcoursDeclare.formations[0])).toBe('CFPT · 2021');
  });

  it('donne un libellé lisible aux types de documents', () => {
    expect(fiche([]).libelleTypeDocument('PIECE_IDENTITE')).toBe("Pièce d'identité");
  });
});
