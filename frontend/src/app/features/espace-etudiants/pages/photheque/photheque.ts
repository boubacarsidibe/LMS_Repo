import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { UiEmptyStateComponent } from '../../../../shared/components/ui/ui-empty-state.component';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import { AlbumPhoto, Photo } from '../../models/scolarite.models';
import { CommunicationService } from '../../services/communication.service';
import { environment } from '../../../../../environments/environment';

/** Photothèque de l'institut (TDR §3.2) : albums d'événements et de sorties. */
@Component({
  selector: 'app-photheque',
  standalone: true,
  imports: [CommonModule, UiSpinnerComponent, UiEmptyStateComponent],
  template: `
    <div class="p-margin-desktop space-y-stack-lg max-w-container-max mx-auto">
      <header>
        <h1 class="font-headline-lg text-headline-lg text-on-surface">Photothèque</h1>
        <p class="font-body-md text-body-md text-on-surface-variant mt-1">
          Sorties, visites et temps forts de la vie de l’institut.
        </p>
      </header>

      <app-ui-spinner *ngIf="chargement" message="Chargement des albums…"></app-ui-spinner>

      <app-ui-empty-state
        *ngIf="!chargement && albums.length === 0"
        icone="photo_library"
        titre="Aucun album"
        description="Aucun album photo n’a encore été publié pour votre promotion."
      ></app-ui-empty-state>

      <section
        *ngIf="!chargement && albums.length > 0"
        class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-gutter"
      >
        <button
          *ngFor="let a of albums"
          type="button"
          (click)="ouvrir(a)"
          class="text-left bg-surface-container-lowest rounded-xl border border-outline-variant overflow-hidden hover:border-secondary transition-colors"
        >
          <div class="h-40 bg-surface-container flex items-center justify-center overflow-hidden">
            <img
              *ngIf="a.couvertureUrl; else vide"
              [src]="lienAbsolu(a.couvertureUrl)"
              [alt]="'Couverture de l’album ' + a.titre"
              class="w-full h-full object-cover"
            />
            <ng-template #vide>
              <span class="material-symbols-outlined text-outline text-[36px]">image</span>
            </ng-template>
          </div>
          <div class="p-stack-md">
            <p class="font-label-md text-label-md text-on-surface">{{ a.titre }}</p>
            <p class="font-body-sm text-body-sm text-on-surface-variant mt-1 line-clamp-2">
              {{ a.description }}
            </p>
            <p class="font-label-sm text-label-sm text-on-surface-variant mt-2">
              {{ a.nbPhotos }} photo(s)
              <span *ngIf="a.dateEvenement"> · {{ a.dateEvenement | date: 'd MMMM y' }}</span>
            </p>
          </div>
        </button>
      </section>

      <!-- Album ouvert -->
      <div
        *ngIf="albumOuvert"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4"
        (click)="fermer()"
      >
        <div
          class="w-full max-w-4xl max-h-[90vh] overflow-y-auto rounded-2xl bg-surface p-stack-lg"
          (click)="$event.stopPropagation()"
        >
          <div class="flex items-start justify-between gap-4">
            <div>
              <h2 class="font-headline-sm text-headline-sm text-on-surface">
                {{ albumOuvert.titre }}
              </h2>
              <p class="font-body-sm text-body-sm text-on-surface-variant mt-1">
                {{ albumOuvert.description }}
              </p>
            </div>
            <button
              type="button"
              (click)="fermer()"
              class="p-2 rounded-full text-on-surface-variant hover:bg-surface-container transition-colors"
              aria-label="Fermer l’album"
            >
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>

          <app-ui-spinner *ngIf="chargementAlbum" message="Chargement des photos…"></app-ui-spinner>

          <app-ui-empty-state
            *ngIf="!chargementAlbum && photos.length === 0"
            icone="no_photography"
            titre="Album vide"
            description="Aucun cliché n’a encore été déposé dans cet album."
          ></app-ui-empty-state>

          <div
            *ngIf="!chargementAlbum && photos.length > 0"
            class="grid grid-cols-2 md:grid-cols-3 gap-3 mt-4"
          >
            <figure *ngFor="let p of photos" class="rounded-lg overflow-hidden bg-surface-container">
              <img
                [src]="lienAbsolu(p.url)"
                [alt]="p.legende || 'Photo de l’album ' + albumOuvert.titre"
                class="w-full h-40 object-cover"
              />
              <figcaption
                *ngIf="p.legende"
                class="p-2 font-body-sm text-body-sm text-on-surface-variant"
              >
                {{ p.legende }}
              </figcaption>
            </figure>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class Photheque implements OnInit {
  albums: AlbumPhoto[] = [];
  photos: Photo[] = [];
  albumOuvert: AlbumPhoto | null = null;
  chargement = true;
  chargementAlbum = false;

  constructor(private communication: CommunicationService) {}

  ngOnInit(): void {
    this.communication.getAlbums().subscribe({
      next: (albums) => {
        this.albums = albums;
        this.chargement = false;
      },
      error: () => (this.chargement = false),
    });
  }

  ouvrir(album: AlbumPhoto): void {
    this.albumOuvert = album;
    this.photos = [];
    this.chargementAlbum = true;
    this.communication.getAlbum(album.id).subscribe({
      next: (contenu) => {
        this.photos = contenu.photos;
        this.chargementAlbum = false;
      },
      error: () => (this.chargementAlbum = false),
    });
  }

  fermer(): void {
    this.albumOuvert = null;
    this.photos = [];
  }

  lienAbsolu(url: string): string {
    if (url.startsWith('http')) {
      return url;
    }
    return environment.apiUrl.replace(/\/api$/, '') + url;
  }
}
