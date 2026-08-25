import { Injectable, signal } from '@angular/core';

export type TonNotification = 'succes' | 'erreur' | 'info';

export interface Notification {
  id: number;
  ton: TonNotification;
  message: string;
}

/**
 * Remplace les `alert()` d'information et d'erreur.
 *
 * <p>Contrairement à `alert()`, le message ne bloque pas l'onglet et disparaît
 * seul. Les erreurs restent affichées plus longtemps que les succès : on a
 * besoin de les lire, parfois de les recopier.
 */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  readonly notifications = signal<Notification[]>([]);

  private compteur = 0;

  succes(message: string): void {
    this.ajouter('succes', message, 4000);
  }

  erreur(message: string): void {
    this.ajouter('erreur', message, 8000);
  }

  info(message: string): void {
    this.ajouter('info', message, 5000);
  }

  fermer(id: number): void {
    this.notifications.update((liste) => liste.filter((n) => n.id !== id));
  }

  private ajouter(ton: TonNotification, message: string, dureeMs: number): void {
    const id = ++this.compteur;
    this.notifications.update((liste) => [...liste, { id, ton, message }]);
    setTimeout(() => this.fermer(id), dureeMs);
  }
}
