import { HttpErrorResponse } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { PieceAEnvoyer, envoyerPieces, estPanneTransitoire, raisonEchec } from './envoi-pieces';

const piece = (nom: string, type = 'CV'): PieceAEnvoyer => ({
  libelle: `${type} (${nom})`,
  file: new File(['x'], nom, { type: 'application/pdf' }),
  typeDocument: type,
});

const erreur = (status: number, message?: string) =>
  new HttpErrorResponse({ status, error: message ? { message } : null });

const sansAttente = { attendre: () => Promise.resolve(), delaiMs: () => 0 };

describe('estPanneTransitoire', () => {
  it('reconnaît une coupure réseau, un délai dépassé et une erreur du serveur', () => {
    for (const statut of [0, 408, 425, 429, 500, 502, 503, 504]) {
      expect(estPanneTransitoire(erreur(statut)), String(statut)).toBe(true);
    }
  });

  it('ne reprend pas un refus que le prochain essai répéterait', () => {
    for (const statut of [400, 401, 403, 404, 409, 413, 422]) {
      expect(estPanneTransitoire(erreur(statut)), String(statut)).toBe(false);
    }
  });
});

describe('raisonEchec', () => {
  it('donne la parole au serveur quand il a répondu quelque chose', () => {
    expect(raisonEchec(erreur(409, 'Vous ne pouvez plus ajouter de document à cette étape du dossier.')))
      .toBe('Vous ne pouvez plus ajouter de document à cette étape du dossier.');
  });

  it('sinon, dit quoi faire plutôt que « Http failure response »', () => {
    expect(raisonEchec(erreur(0))).toBe('Connexion interrompue.');
    expect(raisonEchec(erreur(413))).toBe('Fichier trop volumineux.');
    expect(raisonEchec(erreur(502))).toBe('Erreur 502 du serveur.');
  });
});

describe('envoyerPieces', () => {
  it('envoie toutes les pièces et rend un bilan sans échec', async () => {
    const envoyer = vi.fn(() => of({ message: 'ok' }));

    const bilan = await envoyerPieces([piece('a.pdf'), piece('b.pdf'), piece('c.pdf')], envoyer, sansAttente);

    expect(envoyer).toHaveBeenCalledTimes(3);
    expect(bilan.recues).toHaveLength(3);
    expect(bilan.echecs).toEqual([]);
  });

  /** Le cas de l'audit : un réseau mobile qui lâche une fois ne doit plus coûter un document au dossier. */
  it('reprend une panne passagère et finit par transmettre la pièce', async () => {
    let appels = 0;
    const envoyer = vi.fn(() => (++appels < 3 ? throwError(() => erreur(0)) : of({ message: 'ok' })));

    const bilan = await envoyerPieces([piece('cv.pdf')], envoyer, sansAttente);

    expect(envoyer).toHaveBeenCalledTimes(3);
    expect(bilan.recues).toHaveLength(1);
    expect(bilan.echecs).toEqual([]);
  });

  it('abandonne après le nombre d\'essais prévu et dit que la panne est passagère', async () => {
    const envoyer = vi.fn(() => throwError(() => erreur(503)));

    const bilan = await envoyerPieces([piece('cv.pdf')], envoyer, { ...sansAttente, tentatives: 3 });

    expect(envoyer).toHaveBeenCalledTimes(3);
    expect(bilan.recues).toEqual([]);
    expect(bilan.echecs).toHaveLength(1);
    expect(bilan.echecs[0].definitif).toBe(false);
  });

  it('ne réessaie pas un refus définitif, et garde le message du serveur', async () => {
    const envoyer = vi.fn(() => throwError(() => erreur(409, 'Vous ne pouvez plus ajouter de document.')));

    const bilan = await envoyerPieces([piece('cv.pdf')], envoyer, sansAttente);

    expect(envoyer).toHaveBeenCalledTimes(1);
    expect(bilan.echecs[0]).toMatchObject({ definitif: true, raison: 'Vous ne pouvez plus ajouter de document.' });
  });

  it('une pièce en échec n\'empêche pas les autres de partir', async () => {
    const envoyer = vi.fn((p: PieceAEnvoyer): Observable<unknown> =>
      p.file.name === 'mauvais.pdf' ? throwError(() => erreur(400, 'Format refusé.')) : of({ message: 'ok' }));

    const bilan = await envoyerPieces(
      [piece('a.pdf'), piece('mauvais.pdf'), piece('c.pdf'), piece('d.pdf')], envoyer, sansAttente);

    expect(bilan.recues.map((p) => p.file.name).sort()).toEqual(['a.pdf', 'c.pdf', 'd.pdf']);
    expect(bilan.echecs.map((e) => e.piece.file.name)).toEqual(['mauvais.pdf']);
  });

  it('n\'a jamais plus de pièces en route que le nombre demandé', async () => {
    let enCours = 0;
    let maxEnCours = 0;
    const envoyer = (): Observable<unknown> =>
      new Observable((abonne) => {
        enCours++;
        maxEnCours = Math.max(maxEnCours, enCours);
        setTimeout(() => {
          enCours--;
          abonne.next({});
          abonne.complete();
        }, 5);
      });

    const bilan = await envoyerPieces(
      ['1', '2', '3', '4', '5', '6'].map((n) => piece(`${n}.pdf`)), envoyer, { ...sansAttente, simultanes: 2 });

    expect(bilan.recues).toHaveLength(6);
    expect(maxEnCours).toBe(2);
  });

  it('rend compte de la progression, pièce après pièce', async () => {
    const progression: string[] = [];

    await envoyerPieces([piece('a.pdf'), piece('b.pdf')], () => of({}), {
      ...sansAttente,
      simultanes: 1,
      surProgression: (faites, total) => progression.push(`${faites}/${total}`),
    });

    expect(progression).toEqual(['1/2', '2/2']);
  });

  it('n\'a rien à faire sans pièce, et ne lève pas', async () => {
    const bilan = await envoyerPieces([], () => of({}), sansAttente);

    expect(bilan).toEqual({ recues: [], echecs: [] });
  });

  it('attend de plus en plus longtemps entre deux essais', async () => {
    const attentes: number[] = [];
    const envoyer = vi.fn(() => throwError(() => erreur(0)));

    await envoyerPieces([piece('cv.pdf')], envoyer, {
      tentatives: 3,
      delaiMs: (n) => n * 100,
      attendre: (ms) => { attentes.push(ms); return Promise.resolve(); },
    });

    expect(attentes).toEqual([100, 200]);
  });
});
