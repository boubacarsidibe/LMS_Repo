import { CommonModule } from '@angular/common';
import { Component, ElementRef, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MediaProtegeService } from '../../services/media-protege.service';
import { UiEmptyStateComponent } from '../../../../shared/components/ui/ui-empty-state.component';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import { AlbumPhoto, Photo } from '../../models/scolarite.models';
import { Cohorte } from '../../models/cohorte';
import { CohorteService } from '../../services/cohorte.service';
import { PhotoService } from '../../services/photo.service';

/**
 * Photothèque du formateur : consultation des albums et dépôt de clichés.
 *
 * <p>L'écran s'adressait auparavant à `GET /photos` et `POST /formateur/photos`,
 * disparus lors du passage à un modèle en albums — un cliché appartient
 * désormais toujours à un album. Ni l'affichage ni l'envoi ne fonctionnaient.
 *
 * <p>Le dépôt se fait donc en deux temps, comme le prévoit l'API : on choisit
 * (ou l'on crée) un album, puis on y ajoute des photos.
 */
@Component({
  selector: 'app-phototheque',
  standalone: true,
  imports: [CommonModule, FormsModule, UiSpinnerComponent, UiEmptyStateComponent],
  templateUrl: './phototheque.html',
})
export class Phototheque implements OnInit, OnDestroy {
  private photoService = inject(PhotoService);
  private cohorteService = inject(CohorteService);

  @ViewChild('champFichier') champFichier?: ElementRef<HTMLInputElement>;

  albums: AlbumPhoto[] = [];
  cohortes: Cohorte[] = [];
  chargement = true;

  /** Album ouvert dans la visionneuse, avec ses clichés. */
  albumOuvert: AlbumPhoto | null = null;
  photosAlbumOuvert: Photo[] = [];
  chargementPhotos = false;

  // ----- Création d'un album -----
  formulaireAlbumOuvert = false;
  nouveauTitre = '';
  nouvelleDescription = '';
  nouvelleDate = '';
  nouvellePromotionId: number | null = null;

  // ----- Dépôt d'un cliché -----
  albumCibleId: number | null = null;
  fichierSelectionne: File | null = null;
  legende = '';

  enCours = false;
  messageErreur = '';
  messageSucces = '';

  ngOnInit(): void {
    this.charger();
    this.cohorteService.getAll().subscribe({
      next: (liste) => (this.cohortes = liste || []),
      error: () => (this.cohortes = []),
    });
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
  }

  // ===================== Création d'un album =====================

  ouvrirFormulaireAlbum(): void {
    this.formulaireAlbumOuvert = true;
    this.nouveauTitre = '';
    this.nouvelleDescription = '';
    this.nouvelleDate = '';
    this.nouvellePromotionId = null;
    this.reinitialiserMessages();
  }

  fermerFormulaireAlbum(): void {
    this.formulaireAlbumOuvert = false;
  }

  creerAlbum(): void {
    if (!this.nouveauTitre.trim()) {
      this.messageErreur = "Le titre de l'album est requis.";
      return;
    }
    this.enCours = true;
    this.reinitialiserMessages();
    this.photoService
      .creerAlbum(
        this.nouveauTitre.trim(),
        this.nouvelleDescription.trim() || null,
        this.nouvellePromotionId,
        this.nouvelleDate || null
      )
      .subscribe({
        next: (album) => {
          this.enCours = false;
          this.formulaireAlbumOuvert = false;
          this.messageSucces = `Album « ${album.titre} » créé.`;
          this.albumCibleId = album.id;
          this.charger();
        },
        error: (erreur) => {
          this.enCours = false;
          this.messageErreur = erreur.error?.message || "L'album n'a pas pu être créé.";
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
