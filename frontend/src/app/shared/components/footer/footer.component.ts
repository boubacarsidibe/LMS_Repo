import { Component } from '@angular/core';
import { LogoInpgComponent } from '../logo-inpg/logo-inpg.component';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [LogoInpgComponent],
  templateUrl: './footer.component.html'
})
export class FooterComponent { }