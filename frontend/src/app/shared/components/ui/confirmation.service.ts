import { Injectable, signal } from '@angular/core';
import { Observable, Subject } from 'rxjs';

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

  private reponse$: Subject<boolean> | null = null;

  demander(demande: DemandeConfirmation): Observable<boolean> {
    // Une éventuelle demande précédente restée ouverte est refusée, pour ne pas
    // laisser un abonné suspendu indéfiniment.
    this.cloturer(false);

    this.reponse$ = new Subject<boolean>();
    this.demandeEnCours.set(demande);
    return this.reponse$.asObservable();
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
