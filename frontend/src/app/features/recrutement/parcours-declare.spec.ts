import { libelleTypeDocument, lireParcoursDeclare } from './parcours-declare';

describe('lireParcoursDeclare', () => {
  it('relit le parcours académique et la situation professionnelle tels que le formulaire les envoie', () => {
    const parcours = lireParcoursDeclare(
      JSON.stringify({
        niveauActuel: 'Licence (L3)', etablissement: 'UCAD', diplome: 'Licence en physique', anneeObtention: '2023',
        formationsSuivies: [{ nom: 'Soudage', etablissement: 'CFPT', diplome: 'Attestation', anneeObtention: '2021', pays: 'Sénégal' }],
      }),
      JSON.stringify({ estProfessionnel: 'Non' }),
    );

    expect(parcours.champs).toEqual([
      { libelle: "Niveau d'études actuel", valeur: 'Licence (L3)' },
      { libelle: 'Établissement', valeur: 'UCAD' },
      { libelle: 'Dernier diplôme', valeur: 'Licence en physique' },
      { libelle: "Année d'obtention", valeur: '2023' },
      { libelle: 'Activité professionnelle', valeur: 'Non' },
    ]);
    expect(parcours.formations).toEqual([
      { nom: 'Soudage', etablissement: 'CFPT', diplome: 'Attestation', anneeObtention: '2021', pays: 'Sénégal' },
    ]);
  });

  it('ne montre pas les champs laissés vides, ni une formation sans nom', () => {
    const parcours = lireParcoursDeclare(
      JSON.stringify({ niveauActuel: 'Master (M2)', etablissement: '  ', diplome: '', formationsSuivies: [{ nom: '' }, { nom: 'MBA' }] }),
      null,
    );

    expect(parcours.champs).toEqual([{ libelle: "Niveau d'études actuel", valeur: 'Master (M2)' }]);
    expect(parcours.formations.map((f) => f.nom)).toEqual(['MBA']);
  });

  /** Un dossier déposé avant l'enregistrement du parcours n'en a pas : l'écran doit le supporter. */
  it('ne casse pas sur une absence, un JSON abîmé ou une forme inattendue', () => {
    for (const abime of [null, undefined, '', '{pas du json', '[1,2]', '"texte"', '42', 'null']) {
      expect(lireParcoursDeclare(abime, abime), String(abime)).toEqual({ champs: [], formations: [] });
    }
    expect(lireParcoursDeclare(JSON.stringify({ formationsSuivies: 'oups' }), null).formations).toEqual([]);
    expect(lireParcoursDeclare(JSON.stringify({ formationsSuivies: [null, 3, 'x'] }), null).formations).toEqual([]);
  });
});

describe('libelleTypeDocument', () => {
  it('traduit les identifiants du serveur, et laisse passer un type inconnu', () => {
    expect(libelleTypeDocument('PIECE_IDENTITE')).toBe("Pièce d'identité");
    expect(libelleTypeDocument('RELEVE_NOTES')).toBe('Relevé de notes');
    expect(libelleTypeDocument('AUTRE_CHOSE')).toBe('AUTRE_CHOSE');
  });
});
