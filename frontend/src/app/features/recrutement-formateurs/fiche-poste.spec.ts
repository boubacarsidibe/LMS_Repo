import { TAILLE_MAX_FICHE_POSTE, erreurFichePoste } from './fiche-poste';

describe('erreurFichePoste', () => {
  it('accepte un PDF de taille raisonnable, quelle que soit la casse de l’extension', () => {
    expect(erreurFichePoste({ name: 'fiche.pdf', size: 120_000 })).toBeNull();
    expect(erreurFichePoste({ name: 'FICHE HSE.PDF', size: 1 })).toBeNull();
  });

  it('refuse Word, images et tout ce qui n’est pas un PDF', () => {
    for (const nom of ['fiche.docx', 'fiche.doc', 'scan.png', 'fiche', 'fiche.pdf.exe']) {
      expect(erreurFichePoste({ name: nom, size: 1000 })).toBe('La fiche de poste doit être un fichier PDF.');
    }
  });

  it('refuse un fichier vide', () => {
    expect(erreurFichePoste({ name: 'fiche.pdf', size: 0 })).toBe('Le fichier est vide.');
  });

  it('refuse au-delà de 10 Mo, accepte pile 10 Mo', () => {
    expect(erreurFichePoste({ name: 'fiche.pdf', size: TAILLE_MAX_FICHE_POSTE })).toBeNull();
    expect(erreurFichePoste({ name: 'fiche.pdf', size: TAILLE_MAX_FICHE_POSTE + 1 })).toBe(
      'La fiche de poste ne doit pas dépasser 10 Mo.'
    );
  });
});
