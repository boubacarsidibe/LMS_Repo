import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map, of, shareReplay } from 'rxjs';
import { environment } from '../../../../environments/environment';

/**
 * Accès aux fichiers servis par l'API derrière une authentification.
 *
 * <p>Les supports de cours et les photos sont protégés par le jeton JWT, que
 * l'intercepteur Angular pose dans l'en-tête <code>Authorization</code>. Or un
 * <code>&lt;a href&gt;</code> ou un <code>&lt;img src&gt;</code> ne passe pas par cet
 * intercepteur : c'est le <strong>navigateur</strong> qui va chercher l'adresse, sans
 * en-tête. Le serveur répond alors 403, et l'étudiant voit une page d'erreur brute
 * — la fameuse « whitelabel error » — ou une image cassée.
 *
 * <p>Le fichier est donc récupéré ici par <code>HttpClient</code>, qui porte le jeton,
 * puis exposé au navigateur sous forme d'URL locale (<code>blob:</code>). Aucun
 * assouplissement de la sécurité : le contrôle d'accès reste entier côté serveur.
 */
@Injectable({ providedIn: 'root' })
export class MediaProtegeService {
  private http = inject(HttpClient);

  /** Une image affichée deux fois ne doit pas être téléchargée deux fois. */
  private images = new Map<string, Observable<string>>();
  private aLiberer: string[] = [];

  /** Les URL renvoyées par le serveur sont relatives à l'API. */
  private absolu(url: string): string {
    return url.startsWith('http') ? url : environment.apiUrl.replace(/\/api$/, '') + url;
  }

  /**
   * Ouvre une ressource dans un nouvel onglet.
   *
   * <p>L'onglet est ouvert <strong>avant</strong> l'appel réseau, et non dans sa
   * réponse : ouvert plus tard, il ne serait plus rattaché au clic de l'utilisateur et
   * les navigateurs le bloqueraient comme une fenêtre intempestive.
   */
  ouvrir(url: string): void {
    if (url.startsWith('http')) {
      window.open(url, '_blank', 'noopener');
      return;
    }
    const onglet = window.open('', '_blank');
    this.http.get(this.absolu(url), { responseType: 'blob' }).subscribe({
      next: (fichier) => {
        const local = URL.createObjectURL(fichier);
        this.aLiberer.push(local);
        if (onglet) {
          onglet.location.href = local;
        }
      },
      error: () => onglet?.close(),
    });
  }

  /** URL affichable d'une image protégée, à passer à un `[src]`. */
  image(url: string): Observable<string> {
    if (url.startsWith('http')) {
      return of(url);
    }
    const connue = this.images.get(url);
    if (connue) {
      return connue;
    }
    const flux = this.http.get(this.absolu(url), { responseType: 'blob' }).pipe(
      map((fichier) => {
        const local = URL.createObjectURL(fichier);
        this.aLiberer.push(local);
        return local;
      }),
      shareReplay(1)
    );
    this.images.set(url, flux);
    return flux;
  }

  /**
   * Rend la mémoire des fichiers chargés.
   *
   * <p>Une URL `blob:` retient son contenu tant qu'elle n'est pas révoquée : sans cela,
   * une photothèque parcourue longuement finirait par occuper autant de mémoire que
   * toutes les images vues.
   */
  libererTout(): void {
    this.aLiberer.forEach((local) => URL.revokeObjectURL(local));
    this.aLiberer = [];
    this.images.clear();
  }
}
