import { booleanAttribute, Component, Input } from '@angular/core';

/**
 * Logo officiel de l'INPG (INPG SENEGAL | Institut National du Pétrole et du Gaz), à la place de l'ancien
 * logotype en texte « INPG LMS ». Le fichier est détouré (fond transparent) : sur un fond sombre, utiliser la
 * variante {@code blanc} plutôt que de poser la version couleur dans un conteneur blanc.
 *
 * <p>{@code hauteur} est une classe Tailwind de hauteur ({@code h-10} par défaut) ; la largeur suit les proportions
 * du logo (640 x 174 en couleur, 640 x 167 en blanc, déjà rogné de ses marges transparentes).
 */
@Component({
  selector: 'app-logo-inpg',
  standalone: true,
  template: `
    <img
      [src]="blanc ? '/images/logo-inpg-blanc.png' : '/images/logo-inpg.png'"
      alt="INPG — Institut National du Pétrole et du Gaz"
      width="640"
      [attr.height]="blanc ? 167 : 174"
      [class]="'block w-auto max-w-none ' + hauteur"
    />
  `,
})
export class LogoInpgComponent {
  @Input() hauteur = 'h-10';

  /** Version blanche du logo, pour un fond sombre (panneau de connexion). */
  @Input({ transform: booleanAttribute }) blanc = false;
}
