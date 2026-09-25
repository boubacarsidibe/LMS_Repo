import { Component, Input } from '@angular/core';

/**
 * Logo officiel de l'INPG (INPG SENEGAL | Institut National du Pétrole et du Gaz), à la place de l'ancien
 * logotype en texte « INPG LMS ». Le fichier est détouré (fond transparent) : sur un fond sombre, le poser dans un
 * conteneur blanc arrondi.
 *
 * <p>{@code hauteur} est une classe Tailwind de hauteur ({@code h-10} par défaut) ; la largeur suit les proportions
 * du logo (640 x 174).
 */
@Component({
  selector: 'app-logo-inpg',
  standalone: true,
  template: `
    <img
      src="/images/logo-inpg.png"
      alt="INPG — Institut National du Pétrole et du Gaz"
      width="640"
      height="174"
      [class]="'block w-auto max-w-none ' + hauteur"
    />
  `,
})
export class LogoInpgComponent {
  @Input() hauteur = 'h-10';
}
