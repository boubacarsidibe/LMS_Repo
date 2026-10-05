import { CommonModule } from '@angular/common';
import { Component, ElementRef, HostListener, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MediaProtegeService } from '../../services/media-protege.service';
import { UiEmptyStateComponent } from '../../../../shared/components/ui/ui-empty-state.component';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import { AlbumPhoto, Photo } from '../../models/scolarite.models';
import { PhotoService } from '../../services/photo.service';

/**
 * Photothèque du formateur : consultation des albums et dépôt de clichés dans
 * un album existant.
 *
 * <p>La création d'un album est réservée au responsable pédagogique (décision
 * produit) : le formateur ne fait que visualiser les albums et leurs photos,
 * et peut y déposer un cliché une fois qu'un album existe — ou en retirer un.
 *
 * <p>L'écran s'adressait auparavant à `GET /photos` et `POST /formateur/photos`,
 * disparus lors du passage à un modèle en albums — un cliché appartient
 * désormais toujours à un album.
 *
 * <p>Une photo cliquée dans la grille d'un album s'ouvre en grand dans une
 * visionneuse par-dessus celle de l'album (flèches, clavier, compteur), pour
 * parcourir l'album sans revenir à la grille entre deux clichés — patron
 * courant de toute photothèque.
 */
@Component({
  selector: 'app-phototheque',
  standalone: true,
  imports: [CommonModule, FormsModule, UiSpinnerComponent, UiEmptyStateComponent],
  templateUrl: './phototheque.html',
})
export class Phototheque implements OnInit, OnDestroy {
  private photoService = inject(PhotoService);

  @ViewChild('champFichier') champFichier?: ElementRef<HTMLInputElement>;

  albums: AlbumPhoto[] = [];
  chargement = true;

  /** Album ouvert dans la visionneuse, avec ses clichés. */
  albumOuvert: AlbumPhoto | null = null;
  photosAlbumOuvert: Photo[] = [];
  chargementPhotos = false;

  /** Index, dans {@link photosAlbumOuvert}, de la photo affichée en grand — `null` si fermée. */
  photoOuverteIndex: number | null = null;
  suppressionPhotoEnCoursId: number | null = null;

  // ----- Dépôt d'un cliché -----
  albumCibleId: number | null = null;
  fichierSelectionne: File | null = null;
  legende = '';

  enCours = false;
  messageErreur = '';
  messageSucces = '';

  ngOnInit(): void {
    this.charger();
  }

  private charger(): void {
    this.chargement = true;
    this.photoService.getAlbums().subscribe({
      next: (liste) => {
        this.albums = liste || [];
        this.chargement = false;
      },
      error: () => {
        this.albums = [];
        this.chargement = false;
        this.messageErreur = "Les albums n'ont pas pu être chargés.";
      },
    });
  }

  /**
   * Les photos sont servies derriere le jeton : un <img src> direct recevait un 403.
   * Le fichier passe par HttpClient, qui porte l'en-tete d'authentification.
   */
  media = inject(MediaProtegeService);

  ngOnDestroy(): void {
    this.media.libererTout();
  }

  ouvrirAlbum(album: AlbumPhoto): void {
    this.albumOuvert = album;
    this.photosAlbumOuvert = [];
    this.chargementPhotos = true;
    this.photoService.getAlbum(album.id).subscribe({
      next: (contenu) => {
        this.photosAlbumOuvert = contenu.photos || [];
        this.chargementPhotos = false;
      },
      error: () => {
        this.chargementPhotos = false;
        this.messageErreur = "Les photos de cet album n'ont pas pu être chargées.";
      },
    });
  }

  fermerAlbum(): void {
    this.albumOuvert = null;
    this.photosAlbumOuvert = [];
    this.photoOuverteIndex = null;
  }

  // ===================== Visionneuse plein écran =====================

  get photoOuverte(): Photo | null {
    return this.photoOuverteIndex !== null ? this.photosAlbumOuvert[this.photoOuverteIndex] ?? null : null;
  }

  ouvrirPhoto(index: number): void {
    this.photoOuverteIndex = index;
  }

  fermerPhoto(): void {
    this.photoOuverteIndex = null;
  }

  /** Boucle sur l'album : la dernière photo ramène à la première, et inversement. */
  photoSuivante(): void {
    if (this.photoOuverteIndex === null || this.photosAlbumOuvert.length === 0) {
      return;
    }
    this.photoOuverteIndex = (this.photoOuverteIndex + 1) % this.photosAlbumOuvert.length;
  }

  photoPrecedente(): void {
    if (this.photoOuverteIndex === null || this.photosAlbumOuvert.length === 0) {
      return;
    }
    this.photoOuverteIndex =
      (this.photoOuverteIndex - 1 + this.photosAlbumOuvert.length) % this.photosAlbumOuvert.length;
  }

  /** Navigation clavier de la visionneuse : flèches pour parcourir, Échap pour fermer. */
  @HostListener('document:keydown', ['$event'])
  onKeydown(evenement: KeyboardEvent): void {
    if (this.photoOuverteIndex === null) {
      return;
    }
    if (evenement.key === 'Escape') {
      this.fermerPhoto();
    } else if (evenement.key === 'ArrowRight') {
      this.photoSuivante();
    } else if (evenement.key === 'ArrowLeft') {
      this.photoPrecedente();
    }
  }

  /** Retire un cliché, depuis la grille ou depuis la visionneuse. */
  supprimerPhoto(photo: Photo, evenement?: Event): void {
    evenement?.stopPropagation();
    if (!confirm('Supprimer cette photo de l’album ?')) {
      return;
    }
    this.suppressionPhotoEnCoursId = photo.id;
    this.reinitialiserMessages();
    this.photoService.supprimerPhoto(photo.id).subscribe({
      next: () => {
        this.suppressionPhotoEnCoursId = null;
        const indexSupprime = this.photosAlbumOuvert.findIndex((p) => p.id === photo.id);
        this.photosAlbumOuvert = this.photosAlbumOuvert.filter((p) => p.id !== photo.id);

        if (this.photoOuverteIndex !== null) {
          if (this.photosAlbumOuvert.length === 0) {
            this.photoOuverteIndex = null;
          } else {
            const decalage = indexSupprime < this.photoOuverteIndex ? 1 : 0;
            this.photoOuverteIndex = Math.min(this.photoOuverteIndex - decalage, this.photosAlbumOuvert.length - 1);
          }
        }

        // Recompte le nombre de photos et la couverture de l'album dans la grille, sans
        // rouvrir le spinner de page (la visionneuse reste au premier plan pendant l'appel).
        this.photoService.getAlbums().subscribe({
          next: (liste) => (this.albums = liste || this.albums),
          error: () => undefined,
        });
      },
      error: (erreur) => {
        this.suppressionPhotoEnCoursId = null;
        this.messageErreur = erreur.error?.message || "La suppression de la photo a échoué.";
      },
    });
  }

  // ===================== Dépôt d'un cliché =====================

  ouvrirExplorateur(): void {
    this.champFichier?.nativeElement.click();
  }

  fichierChoisi(evenement: Event): void {
    const champ = evenement.target as HTMLInputElement;
    this.fichierSelectionne = champ.files?.length ? champ.files[0] : null;
  }

  get depotPossible(): boolean {
    return !!this.albumCibleId && !!this.fichierSelectionne && !this.enCours;
  }

  envoyerPhoto(): void {
    if (!this.albumCibleId || !this.fichierSelectionne) {
      this.messageErreur = 'Choisissez un album et un fichier.';
      return;
    }
    this.enCours = true;
    this.reinitialiserMessages();
    this.photoService
      .ajouterPhoto(this.albumCibleId, this.fichierSelectionne, this.legende.trim() || null)
      .subscribe({
        next: () => {
          this.enCours = false;
          this.messageSucces = 'Photo ajoutée à l’album.';
          this.fichierSelectionne = null;
          this.legende = '';
          if (this.champFichier) {
            this.champFichier.nativeElement.value = '';
          }
          this.charger();
        },
        error: (erreur) => {
          this.enCours = false;
          this.messageErreur = erreur.error?.message || "L'envoi de la photo a échoué.";
        },
      });
  }

  private reinitialiserMessages(): void {
    this.messageErreur = '';
    this.messageSucces = '';
  }
}
