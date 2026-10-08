import { CommonModule } from '@angular/common';
import { Component, ElementRef, computed, input, model, signal, viewChild } from '@angular/core';

/** Une entrée du champ : `valeur` est ce que le parent récupère, `detail` aide à départager deux homonymes. */
export interface OptionCombobox {
  valeur: number;
  libelle: string;
  detail?: string;
}

/** Minuscules sans accents : « Élodie » se retrouve en tapant « elo ». */
function normaliser(texte: string): string {
  return texte
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .toLowerCase();
}

/**
 * Liste déroulante avec recherche.
 *
 * <p>Un `<select>` natif ne se filtre pas : sur une centaine de promotions ou
 * d'étudiants, il faut le dérouler et chercher à l'œil. Ici on peut aussi taper
 * quelques lettres du nom, et la liste se réduit.
 *
 * <p>Deux usages, une seule liaison `[(selection)]` (tableau de valeurs) :
 * <ul>
 *   <li>choix unique (défaut) : `selection` porte zéro ou une valeur ;</li>
 *   <li>choix multiple (`[multiple]="true"`) : les choix s'affichent en pastilles
 *       retirables, et ne sont plus proposés dans la liste.</li>
 * </ul>
 *
 * <p>Au clavier : flèches pour parcourir, Entrée pour choisir, Échap pour fermer,
 * Retour arrière sur un champ vide pour retirer la dernière pastille. Le pointeur
 * agit sur `mousedown` et non sur `click` : le `click` arriverait après la perte de
 * focus du champ, qui aurait déjà refermé la liste.
 */
@Component({
  selector: 'app-ui-combobox',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="relative">
      <div class="relative">
        <input
          #champ
          type="text"
          role="combobox"
          autocomplete="off"
          [id]="idChamp()"
          [attr.aria-label]="libelleAccessible() || null"
          [attr.aria-expanded]="ouvert()"
          [attr.aria-controls]="idListe()"
          [attr.aria-activedescendant]="ouvert() && visibles().length ? idOption(actif()) : null"
          aria-autocomplete="list"
          [placeholder]="placeholder()"
          [value]="texteAffiche()"
          [disabled]="desactive()"
          (focus)="ouvrir($event)"
          (click)="ouvert.set(true)"
          (input)="surSaisie($any($event.target).value)"
          (keydown)="surTouche($event)"
          (blur)="fermer()"
          class="w-full p-3 pr-16 bg-surface-container-low rounded-lg border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors disabled:opacity-50"
        />
        <span class="absolute inset-y-0 right-2 flex items-center gap-1">
          <button
            *ngIf="!multiple() && choisies().length && !desactive()"
            type="button"
            (mousedown)="effacer($event)"
            aria-label="Effacer le choix"
            class="p-1 rounded-full text-on-surface-variant hover:bg-surface-container"
          >
            <span class="material-symbols-outlined text-[18px]" aria-hidden="true">close</span>
          </button>
          <span class="material-symbols-outlined text-on-surface-variant pointer-events-none" aria-hidden="true">{{
            ouvert() ? 'expand_less' : 'expand_more'
          }}</span>
        </span>
      </div>

      <ul
        *ngIf="ouvert()"
        role="listbox"
        [id]="idListe()"
        class="absolute z-20 mt-1 w-full max-h-60 overflow-y-auto bg-surface-container-lowest border border-outline-variant rounded-lg shadow-lg py-1"
      >
        <li
          *ngFor="let o of visibles(); let i = index"
          role="option"
          [id]="idOption(i)"
          [attr.aria-selected]="estChoisie(o)"
          (mousedown)="choisir(o, $event)"
          (mouseenter)="actif.set(i)"
          class="flex items-center justify-between gap-3 px-3 py-2 cursor-pointer"
          [ngClass]="i === actif() ? 'bg-surface-container-low' : ''"
        >
          <span class="min-w-0">
            <span class="block font-body-sm text-body-sm text-on-surface truncate">{{ o.libelle }}</span>
            <span *ngIf="o.detail" class="block font-label-sm text-label-sm text-on-surface-variant truncate">{{
              o.detail
            }}</span>
          </span>
          <span
            *ngIf="estChoisie(o)"
            class="material-symbols-outlined text-secondary shrink-0"
            aria-hidden="true"
            >check</span
          >
        </li>
        <li
          *ngIf="visibles().length === 0"
          class="px-3 py-2 font-body-sm text-body-sm text-on-surface-variant"
          role="option"
          aria-disabled="true"
          aria-selected="false"
        >
          {{ messageVide() }}
        </li>
      </ul>
    </div>

    <ul *ngIf="multiple() && choisies().length" class="flex flex-wrap gap-2 mt-2" aria-label="Choix retenus">
      <li
        *ngFor="let o of choisies()"
        class="inline-flex items-center gap-1 pl-3 pr-1 py-1 rounded-full bg-secondary-container text-on-secondary-container font-label-sm text-label-sm"
      >
        {{ o.libelle }}
        <button
          type="button"
          (click)="retirer(o)"
          [attr.aria-label]="'Retirer ' + o.libelle"
          class="p-0.5 rounded-full hover:bg-surface-container-high"
        >
          <span class="material-symbols-outlined text-[16px]" aria-hidden="true">close</span>
        </button>
      </li>
    </ul>
  `,
})
export class UiComboboxComponent {
  private static compteur = 0;

  readonly options = input<OptionCombobox[]>([]);
  readonly placeholder = input('');
  readonly multiple = input(false);
  readonly desactive = input(false);
  /** Nom lu par les lecteurs d'écran, quand aucun `<label for>` n'est relié au champ. */
  readonly libelleAccessible = input('');
  /** Texte de la liste quand il n'y a aucune option du tout. */
  readonly libelleVide = input('Aucun élément.');
  /** À relier à un `<label for>`. */
  readonly idChamp = input(`ui-combobox-champ-${++UiComboboxComponent.compteur}`);
  readonly selection = model<number[]>([]);

  readonly idListe = computed(() => `${this.idChamp()}-liste`);
  readonly ouvert = signal(false);
  /** Texte en cours de frappe ; `null` tant qu'on ne tape pas (le champ montre alors le choix). */
  readonly saisie = signal<string | null>(null);
  readonly actif = signal(0);

  /** Dans l'ordre où elles ont été choisies. */
  readonly choisies = computed(() => {
    const parValeur = new Map(this.options().map((o) => [o.valeur, o]));
    return this.selection()
      .map((v) => parValeur.get(v))
      .filter((o): o is OptionCombobox => !!o);
  });

  readonly visibles = computed(() => {
    const terme = normaliser(this.saisie() ?? '').trim();
    const dejaChoisies = this.multiple() ? new Set(this.selection()) : new Set<number>();
    return this.options().filter(
      (o) =>
        !dejaChoisies.has(o.valeur) &&
        (terme === '' || normaliser(`${o.libelle} ${o.detail ?? ''}`).includes(terme))
    );
  });

  readonly messageVide = computed(() => {
    if (this.options().length === 0) {
      return this.libelleVide();
    }
    const terme = this.saisie()?.trim();
    return terme ? `Aucun résultat pour « ${terme} ».` : 'Tout est déjà choisi.';
  });

  readonly texteAffiche = computed(
    () => this.saisie() ?? (this.multiple() ? '' : (this.choisies()[0]?.libelle ?? ''))
  );

  private readonly champ = viewChild<ElementRef<HTMLInputElement>>('champ');

  /**
   * Remet le champ à ce que le composant veut y montrer.
   *
   * <p>La liaison `[value]` ne suffit pas : l'application est sans zone, les mises à jour
   * sont regroupées. Si l'on tape puis qu'on valide aussitôt (Entrée, collage), le texte
   * lié passe de `''` à `''` — Angular ne voit aucun changement et laisse le texte tapé
   * dans le champ, alors que le choix est fait.
   */
  private synchroniserTexte(): void {
    const champ = this.champ()?.nativeElement;
    if (champ) {
      champ.value = this.texteAffiche();
    }
  }

  idOption(index: number): string {
    return `${this.idChamp()}-option-${index}`;
  }

  estChoisie(option: OptionCombobox): boolean {
    return this.selection().includes(option.valeur);
  }

  ouvrir(evenement: FocusEvent): void {
    this.ouvert.set(true);
    // Choix unique : tout sélectionner, pour que la première frappe remplace le choix affiché.
    if (!this.multiple()) {
      (evenement.target as HTMLInputElement).select();
    }
  }

  fermer(): void {
    this.ouvert.set(false);
    this.saisie.set(null);
    this.actif.set(0);
    this.synchroniserTexte();
  }

  surSaisie(texte: string): void {
    this.saisie.set(texte);
    this.actif.set(0);
    this.ouvert.set(true);
  }

  choisir(option: OptionCombobox, evenement?: Event): void {
    // Garde le focus dans le champ : sans cela, il se vide avant que la liste ne se referme.
    evenement?.preventDefault();
    if (this.multiple()) {
      if (!this.estChoisie(option)) {
        this.selection.set([...this.selection(), option.valeur]);
      }
      this.saisie.set(null);
      this.actif.set(0);
      this.synchroniserTexte();
    } else {
      this.selection.set([option.valeur]);
      this.fermer();
    }
  }

  retirer(option: OptionCombobox): void {
    this.selection.set(this.selection().filter((v) => v !== option.valeur));
  }

  effacer(evenement: Event): void {
    evenement.preventDefault();
    this.selection.set([]);
    this.saisie.set(null);
    this.synchroniserTexte();
  }

  surTouche(evenement: KeyboardEvent): void {
    const liste = this.visibles();
    switch (evenement.key) {
      case 'ArrowDown':
        evenement.preventDefault();
        this.ouvert.set(true);
        this.actif.set(liste.length ? Math.min(this.actif() + 1, liste.length - 1) : 0);
        break;
      case 'ArrowUp':
        evenement.preventDefault();
        this.actif.set(Math.max(this.actif() - 1, 0));
        break;
      case 'Enter':
        // Toujours intercepté : dans un formulaire, Entrée enverrait sinon la demande.
        evenement.preventDefault();
        if (this.ouvert() && liste[this.actif()]) {
          this.choisir(liste[this.actif()]);
        }
        break;
      case 'Escape':
        if (this.ouvert()) {
          evenement.stopPropagation();
          this.fermer();
        }
        break;
      case 'Backspace':
        if (this.multiple() && !this.saisie() && this.selection().length) {
          this.selection.set(this.selection().slice(0, -1));
        }
        break;
    }
  }
}
