import { Injectable, signal } from '@angular/core';
import { map, Observable, Subject } from 'rxjs';

export interface DemandeConfirmation {
  /** Titre court, ex. « Supprimer le cours ». */
  titre: string;
  /** Ce qui va réellement se passer, formulé sans ambiguïté. */
  message: string;
  /** Libellé du bouton qui valide. Doit nommer l'action, pas dire « OK ». */
  libelleConfirmer?: string;
  libelleAnnuler?: string;
  /** true pour une action destructrice : le bouton passe en rouge. */
  destructif?: boolean;
  /** Si présent, la boîte recueille aussi un texte (motif de rejet…) : voir {@link ConfirmationService#demanderSaisie}. */
  saisie?: {
    libelle: string;
    /** Le bouton de confirmation reste grisé tant que le champ est vide. */
    obligatoire?: boolean;
    placeholder?: string;
    maxLength?: number;
  };
}

/**
 * Remplace le `confirm()` natif du navigateur.
 *
 * <p>`confirm()` fige l'onglet, ignore la charte graphique, n'est pas traduisible
 * et ne peut pas être testé. Ici la demande est asynchrone : l'appelant s'abonne
 * au résultat au lieu de bloquer.
 *
 * <p>Utilisation :
 * <pre>
 *   this.confirmation.demander({
 *     titre: 'Supprimer le cours',
 *     message: `« ${cours.titre} » et ses supports seront définitivement supprimés.`,
 *     libelleConfirmer: 'Supprimer',
 *     destructif: true,
 *   }).subscribe((confirme) => {
 *     if (!confirme) return;
 *     // ...
 *   });
 * </pre>
 */
@Injectable({ providedIn: 'root' })
export class ConfirmationService {
  /** Demande courante, ou null quand aucune boîte n'est ouverte. */
  readonly demandeEnCours = signal<DemandeConfirmation | null>(null);

  /** Texte en cours de saisie dans la boîte (quand la demande comporte un champ). */
  readonly texteSaisi = signal('');

  private reponse$: Subject<boolean> | null = null;

  demander(demande: DemandeConfirmation): Observable<boolean> {
    // Une éventuelle demande précédente restée ouverte est refusée, pour ne pas
    // laisser un abonné suspendu indéfiniment.
    this.cloturer(false);

    this.reponse$ = new Subject<boolean>();
    this.texteSaisi.set('');
    this.demandeEnCours.set(demande);
    return this.reponse$.asObservable();
  }

  /**
   * Remplace le `prompt()` natif : demande une confirmation ET un texte (le motif d'un rejet, par exemple).
   *
   * <p>Le `prompt()` natif n'est ni stylé ni accessible, et il n'apparaît pas dans un partage d'écran ou un
   * enregistrement : un responsable qui présente la plateforme voit son écran se figer sans rien comprendre.
   *
   * @return le texte saisi (sans espaces en tête ni en queue), ou {@code null} si l'utilisateur annule
   */
  demanderSaisie(demande: DemandeConfirmation & { saisie: NonNullable<DemandeConfirmation['saisie']> }): Observable<string | null> {
    return this.demander(demande).pipe(
      // Lu au moment de la réponse : cloturer() n'efface la boîte qu'après avoir émis.
      map((confirme) => (confirme ? this.texteSaisi().trim() : null))
    );
  }

  /** Appelé par le composant hôte quand l'utilisateur tranche. */
  repondre(confirme: boolean): void {
    this.cloturer(confirme);
  }

  private cloturer(valeur: boolean): void {
    if (this.reponse$) {
      this.reponse$.next(valeur);
      this.reponse$.complete();
      this.reponse$ = null;
    }
    this.demandeEnCours.set(null);
  }
}
