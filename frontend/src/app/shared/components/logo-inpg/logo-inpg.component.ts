import { NgTemplateOutlet } from '@angular/common';
import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * Logo officiel de l'INPG (INPG SENEGAL | Institut National du Pétrole et du Gaz), à la place de l'ancien
 * logotype en texte « INPG LMS ». Le fichier est détouré (fond transparent) : sur un fond sombre, le poser dans un
 * conteneur blanc arrondi.
 *
 * <p>{@code hauteur} est une classe Tailwind de hauteur ({@code h-10} par défaut) ; la largeur suit les proportions
 * du logo (640 x 174).
 *
 * <p>Le logo est <strong>cliquable partout</strong> et ramène à la page d'accueil ({@code /accueil}) : le lien vit ici,
 * une seule fois, plutôt que recopié autour de chaque logo — il l'était dans le navbar public et deux en-têtes, et
 * manquait dans cinq barres latérales. Ne pas l'entourer d'un {@code <a>} : un lien dans un lien est invalide.
 * {@code [cliquable]="false"} pose l'image seule, pour un logo déjà contenu dans un lien ou purement décoratif.
 */
@Component({
  selector: 'app-logo-inpg',
  standalone: true,
  imports: [NgTemplateOutlet, RouterLink],
  template: `
    <ng-template #image>
      <img
        src="/images/logo-inpg.png"
        alt="INPG — Institut National du Pétrole et du Gaz"
        width="640"
        height="174"
        [class]="'block w-auto max-w-none ' + hauteur"
      />
    </ng-template>

    @if (cliquable) {
      <!-- flex + w-fit : la zone cliquable épouse le logo (un bloc pleine largeur ferait naviguer en cliquant à côté),
           sans la ligne de base d'un élément en ligne qui décalerait le logo de quelques pixels. -->
      <a
        routerLink="/accueil"
        class="flex w-fit no-underline transition-opacity hover:opacity-80"
        title="Retour à l'accueil"
        aria-label="INPG — Institut National du Pétrole et du Gaz, retour à l'accueil"
      >
        <ng-container [ngTemplateOutlet]="image" />
      </a>
    } @else {
      <ng-container [ngTemplateOutlet]="image" />
    }
  `,
})
export class LogoInpgComponent {
  @Input() hauteur = 'h-10';
  /** Vrai par défaut : le logo mène à l'accueil. Faux : l'image seule. */
  @Input() cliquable = true;
}
