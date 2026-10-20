import { Component } from '@angular/core';
import { LogoInpgComponent } from '../logo-inpg/logo-inpg.component';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [LogoInpgComponent],
  templateUrl: './footer.component.html'
})
export class FooterComponent {
  /** L'année du copyright était écrite en dur (2024) : le pied de page vieillissait de lui-même. */
  readonly annee = new Date().getFullYear();
}