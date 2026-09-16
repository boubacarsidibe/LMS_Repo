import { CommonModule } from '@angular/common';
import {
  Component,
  ElementRef,
  Input,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { Observable, Subscription } from 'rxjs';

type EtatNote = 'repos' | 'envoi' | 'enregistre' | 'erreur';

/**
 * Champ de saisie d'une note, enregistrée au fil de la frappe (grille de notation).
 *
 * <p>Écrit une fois pour toutes plutôt que recopié dans chaque grille : les deux
 * copies précédentes (tests académiques, entretiens) avaient déjà divergé — l'une
 * restaurait la valeur après une erreur serveur, l'autre non ; l'une proposait
 * d'annuler la dernière saisie, l'autre non.
 *
 * <p>Trois choix, chacun corrigeant un défaut constaté :
 *
 * <ul>
 *   <li><b>{@code type="text"} et non {@code type="number"}.</b> Sur un champ
 *   numérique, la spécification HTML impose de vider {@code value} dès que le
 *   contenu n'est pas un nombre décimal <i>à point</i>. Un « 15,5 » tapé au pavé
 *   numérique français y était donc lu comme une chaîne vide, c'est-à-dire comme
 *   un effacement de la note — sans le moindre message. La virgule est désormais
 *   convertie en point avant analyse ; {@code inputmode="decimal"} conserve le
 *   pavé numérique sur mobile.</li>
 *
 *   <li><b>Le champ n'est jamais désactivé pendant l'enregistrement.</b> Le griser
 *   le temps d'un aller-retour réseau interrompt la saisie en série, qui est
 *   pourtant tout l'intérêt d'une grille. L'état est signalé par un pictogramme à
 *   droite, à largeur réservée pour que rien ne se décale.</li>
 *
 *   <li><b>Entrée passe au champ suivant</b> au lieu de rendre le focus au corps de
 *   page : on note une série de candidats sans jamais quitter le clavier. Échap
 *   restaure la valeur enregistrée.</li>
 * </ul>
 */
@Component({
  selector: 'app-ui-note',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="inline-flex items-center gap-1">
      <input
        #champ
        data-note
        type="text"
        inputmode="decimal"
        autocomplete="off"
        placeholder="—"
        [value]="texte"
        [disabled]="desactive"
        [attr.aria-label]="etiquette"
        [attr.aria-invalid]="etat === 'erreur' ? 'true' : null"
        [title]="message || etiquette"
        (focus)="champ.select()"
        (input)="saisieEnCours()"
        (keydown.enter)="$event.preventDefault(); valider(champ.value, true)"
        (keydown.escape)="$event.preventDefault(); restaurer()"
        (blur)="valider(champ.value, false)"
        class="w-20 h-9 px-2 text-center tabular-nums font-body-sm text-body-sm text-on-surface bg-surface border rounded-lg outline-none transition-colors disabled:opacity-50"
        [ngClass]="
          etat === 'erreur'
            ? 'border-error ring-1 ring-error/30'
            : 'border-outline-variant focus:border-secondary focus:ring-1 focus:ring-secondary/20'
        "
      />
      <!-- Largeur fixe : sans elle, l'apparition du pictogramme décalerait la colonne. -->
      <span class="w-4 shrink-0 flex items-center justify-center" aria-hidden="true">
        <span
          *ngIf="etat === 'envoi'"
          class="material-symbols-outlined text-[16px] text-on-surface-variant animate-spin"
          >progress_activity</span
        >
        <span
          *ngIf="etat === 'enregistre'"
          class="material-symbols-outlined text-[16px] text-secondary"
          >check</span
        >
        <span *ngIf="etat === 'erreur'" class="material-symbols-outlined text-[16px] text-error"
          >error</span
        >
      </span>
      <span class="sr-only" role="status">{{ message }}</span>
    </div>
  `,
})
export class UiNoteComponent implements OnChanges, OnDestroy {
  /** Note enregistrée côté serveur. {@code null} = pas encore notée. */
  @Input() valeur: number | null = null;
  @Input() min = 0;
  @Input() max = 20;
  @Input() etiquette = 'Note';
  @Input() desactive = false;

  /** Passé tel quel à {@link enregistrer} — la ligne à laquelle ce champ appartient. */
  @Input() contexte: unknown = null;

  /**
   * Appelé quand la saisie change et qu'elle est valide. Doit renvoyer la note
   * telle que le serveur l'a retenue : c'est elle qui sera réaffichée.
   *
   * <p>À déclarer côté appelant comme propriété fléchée ({@code readonly f = (…) => …}),
   * jamais comme appel de méthode dans le gabarit : une fonction recréée à chaque
   * cycle de détection relancerait un enregistrement en boucle.
   */
  @Input() enregistrer: ((note: number | null, contexte: unknown) => Observable<number | null>) | null = null;

  @ViewChild('champ') champRef?: ElementRef<HTMLInputElement>;

  etat: EtatNote = 'repos';
  message = '';
  texte = '';

  private envoi: Subscription | null = null;
  private minuteur: ReturnType<typeof setTimeout> | null = null;

  /**
   * Note d'un envoi encore en vol. Entrée valide puis déplace le focus, et ce
   * déplacement fait perdre le focus au champ — donc déclenche son {@code blur},
   * donc une seconde validation. Sans cette mémoire, la même note partait deux
   * fois : {@link valeur} n'est pas encore à jour quand la seconde arrive.
   */
  private noteEnvoyee: number | null = null;
  private envoiActif = false;

  ngOnChanges(changes: SimpleChanges): void {
    // Pendant un envoi, la valeur d'entrée est en retard sur ce que l'utilisateur
    // a tapé : la recopier ferait clignoter le champ.
    if (changes['valeur'] && this.etat !== 'envoi') {
      this.texte = this.formater(this.valeur);
    }
  }

  ngOnDestroy(): void {
    this.envoi?.unsubscribe();
    if (this.minuteur) clearTimeout(this.minuteur);
  }

  /** Toute frappe efface l'état précédent : le rouge d'une erreur corrigée doit partir. */
  saisieEnCours(): void {
    if (this.etat !== 'envoi') this.reposer();
  }

  restaurer(): void {
    this.appliquerTexte(this.formater(this.valeur));
    this.reposer();
    this.champRef?.nativeElement.blur();
  }

  valider(saisi: string, passerAuSuivant: boolean): void {
    const brut = saisi.trim().replace(',', '.');

    if (brut !== '' && !/^\d+(\.\d+)?$/.test(brut)) {
      this.echouer(`Saisie invalide : entrez un nombre entre ${this.min} et ${this.max}.`);
      return;
    }
    const note = brut === '' ? null : Number(brut);
    if (note !== null && (note < this.min || note > this.max)) {
      this.echouer(`La note doit être comprise entre ${this.min} et ${this.max}.`);
      return;
    }

    const dejaEnVol = this.envoiActif && note === this.noteEnvoyee;
    if (note === (this.valeur ?? null) || dejaEnVol) {
      // Rien à envoyer : on se contente de normaliser l'affichage (« 15,50 » → « 15.5 »).
      if (!dejaEnVol) {
        this.appliquerTexte(this.formater(this.valeur));
        this.reposer();
      }
      if (passerAuSuivant) this.focusSuivant();
      return;
    }

    if (!this.enregistrer) return;

    this.envoi?.unsubscribe();
    this.etat = 'envoi';
    this.message = '';
    this.noteEnvoyee = note;
    this.envoiActif = true;
    this.envoi = this.enregistrer(note, this.contexte).subscribe({
      next: (retenue) => {
        this.envoiActif = false;
        this.valeur = retenue;
        this.appliquerTexte(this.formater(retenue));
        this.etat = 'enregistre';
        this.message = retenue === null ? 'Note effacée.' : `Note ${this.formater(retenue)} enregistrée.`;
        this.effacerEtatApresDelai();
      },
      error: (err) => {
        this.envoiActif = false;
        // La saisie refusée est retirée : laisser le champ afficher une note que le
        // serveur n'a pas retenue ferait croire à un enregistrement.
        this.appliquerTexte(this.formater(this.valeur));
        this.echouer(err?.error?.message || "Échec de l'enregistrement de la note.");
      },
    });

    if (passerAuSuivant) this.focusSuivant();
  }

  private echouer(message: string): void {
    this.etat = 'erreur';
    this.message = message;
  }

  private reposer(): void {
    if (this.minuteur) {
      clearTimeout(this.minuteur);
      this.minuteur = null;
    }
    this.etat = 'repos';
    this.message = '';
  }

  /** La coche verte s'efface seule : sur une grille entière, elles s'accumuleraient. */
  private effacerEtatApresDelai(): void {
    if (this.minuteur) clearTimeout(this.minuteur);
    this.minuteur = setTimeout(() => {
      if (this.etat === 'enregistre') {
        this.etat = 'repos';
        this.message = '';
      }
      this.minuteur = null;
    }, 1600);
  }

  /**
   * Le champ n'est pas lié à {@code [(ngModel)]} : un rafraîchissement de la liste
   * effacerait la saisie en cours. On écrit donc directement dans l'élément, et on
   * garde {@link texte} synchronisé pour les prochains cycles de détection.
   */
  private appliquerTexte(valeur: string): void {
    this.texte = valeur;
    if (this.champRef) this.champRef.nativeElement.value = valeur;
  }

  private formater(note: number | null): string {
    return note === null || note === undefined ? '' : String(note);
  }

  /** Enchaîne sur la note suivante de la page — la saisie en série ne quitte pas le clavier. */
  private focusSuivant(): void {
    const courant = this.champRef?.nativeElement;
    if (!courant) return;
    const champs = Array.from(
      document.querySelectorAll<HTMLInputElement>('input[data-note]'),
    ).filter((c) => !c.disabled);
    const i = champs.indexOf(courant);
    if (i >= 0 && i + 1 < champs.length) champs[i + 1].focus();
    else courant.blur();
  }
}
