import { TestBed } from '@angular/core/testing';

import { ConfirmationService } from './confirmation.service';

/**
 * `prompt()` natif remplacé : la saisie d'un motif passe par la boîte du projet, qui reste visible dans un partage
 * d'écran, respecte la charte et se teste.
 */
describe('ConfirmationService — saisie d\'un motif', () => {
  let service: ConfirmationService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ConfirmationService);
  });

  const demande = {
    titre: 'Rejeter ce dossier',
    message: 'Le motif est obligatoire.',
    saisie: { libelle: 'Motif du rejet', obligatoire: true },
  };

  it('renvoie le texte saisi, sans espaces superflus, quand l\'utilisateur confirme', () => {
    let obtenu: string | null | undefined;
    service.demanderSaisie(demande).subscribe((v) => (obtenu = v));

    service.texteSaisi.set('  Pièces illisibles  ');
    service.repondre(true);

    expect(obtenu).toBe('Pièces illisibles');
    expect(service.demandeEnCours()).toBeNull();
  });

  it('renvoie null quand l\'utilisateur annule — jamais un texte à moitié saisi', () => {
    let obtenu: string | null | undefined;
    service.demanderSaisie(demande).subscribe((v) => (obtenu = v));

    service.texteSaisi.set('à moitié saisi');
    service.repondre(false);

    expect(obtenu).toBeNull();
  });

  it('chaque demande repart d\'un champ vide', () => {
    service.demanderSaisie(demande).subscribe();
    service.texteSaisi.set('ancien motif');
    service.repondre(false);

    service.demanderSaisie(demande).subscribe();

    expect(service.texteSaisi()).toBe('');
  });

  it('la confirmation simple reste un booléen', () => {
    let obtenu: boolean | undefined;
    service.demander({ titre: 'Supprimer', message: 'Définitivement.' }).subscribe((v) => (obtenu = v));

    service.repondre(true);

    expect(obtenu).toBe(true);
  });
});
