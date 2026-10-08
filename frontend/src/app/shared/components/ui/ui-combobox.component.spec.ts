import { ComponentFixture, TestBed } from '@angular/core/testing';
import { OptionCombobox, UiComboboxComponent } from './ui-combobox.component';

const OPTIONS: OptionCombobox[] = [
  { valeur: 1, libelle: 'Élodie Sarr', detail: 'INPG-001' },
  { valeur: 2, libelle: 'Moussa Ndiaye', detail: 'INPG-002' },
  { valeur: 3, libelle: 'Moussa Diop', detail: 'INPG-003' },
];

describe('UiComboboxComponent', () => {
  let fixture: ComponentFixture<UiComboboxComponent>;
  let composant: UiComboboxComponent;
  let champ: HTMLInputElement;

  function dispose(multiple: boolean): void {
    fixture = TestBed.createComponent(UiComboboxComponent);
    fixture.componentRef.setInput('options', OPTIONS);
    fixture.componentRef.setInput('multiple', multiple);
    composant = fixture.componentInstance;
    fixture.detectChanges();
    champ = fixture.nativeElement.querySelector('input');
  }

  function taper(texte: string): void {
    champ.value = texte;
    champ.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  function touche(key: string): void {
    champ.dispatchEvent(new KeyboardEvent('keydown', { key, cancelable: true }));
    fixture.detectChanges();
  }

  beforeEach(() => TestBed.configureTestingModule({ imports: [UiComboboxComponent] }));

  it('filtre en tapant, sans tenir compte de la casse ni des accents', () => {
    dispose(false);

    taper('elo');
    expect(composant.visibles().map((o) => o.valeur)).toEqual([1]);

    taper('MOUSSA');
    expect(composant.visibles().map((o) => o.valeur)).toEqual([2, 3]);
  });

  it('cherche aussi dans le détail (matricule)', () => {
    dispose(false);

    taper('inpg-003');
    expect(composant.visibles().map((o) => o.valeur)).toEqual([3]);
  });

  it('choix unique : choisir remplace le choix, affiche son libellé et ferme la liste', () => {
    dispose(false);
    taper('diop');
    touche('Enter');

    expect(composant.selection()).toEqual([3]);
    expect(composant.ouvert()).toBe(false);
    expect(composant.texteAffiche()).toBe('Moussa Diop');
  });

  it('choix multiple : empile les pastilles, ne reproposer pas ce qui est choisi, Retour arrière retire la dernière', () => {
    dispose(true);
    taper('moussa');
    touche('Enter');
    expect(champ.value).toBe('');
    taper('moussa');

    expect(composant.selection()).toEqual([2]);
    expect(composant.visibles().map((o) => o.valeur)).toEqual([3]);
    expect(fixture.nativeElement.querySelectorAll('ul[aria-label="Choix retenus"] li').length).toBe(1);

    taper('');
    touche('Backspace');
    expect(composant.selection()).toEqual([]);
  });

  it('vide le champ même si l’on valide avant tout rafraîchissement de la liaison (application sans zone)', () => {
    dispose(true);
    // Frappe puis Entrée sans detectChanges entre les deux : la liaison [value] ne change jamais de valeur.
    champ.value = 'moussa n';
    champ.dispatchEvent(new Event('input'));
    champ.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', cancelable: true }));

    expect(composant.selection()).toEqual([2]);
    expect(champ.value).toBe('');
  });

  it('les flèches parcourent la liste, Échap la ferme', () => {
    dispose(false);
    taper('moussa');
    touche('ArrowDown');
    expect(composant.actif()).toBe(1);
    touche('Enter');
    expect(composant.selection()).toEqual([3]);

    taper('x');
    touche('Escape');
    expect(composant.ouvert()).toBe(false);
  });

  it('Entrée n’envoie jamais le formulaire qui contient le champ', () => {
    dispose(false);
    const evenement = new KeyboardEvent('keydown', { key: 'Enter', cancelable: true });
    champ.dispatchEvent(evenement);

    expect(evenement.defaultPrevented).toBe(true);
  });

  it('dit pourquoi la liste est vide', () => {
    dispose(false);
    taper('zzz');
    expect(composant.messageVide()).toContain('Aucun résultat pour « zzz »');
  });
});
