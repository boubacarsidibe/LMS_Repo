import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FooterComponent } from '../../../../shared/components/footer/footer.component';
import { NavbarComponent } from '../../../../shared/components/navbar/navbar.component';
import { programmes } from '../../data/catalogue-data';
import { RecrutementModule } from '../../recrutement.module';

@Component({
  selector: 'app-landing',
  standalone: true,

  imports: [CommonModule,RecrutementModule,NavbarComponent,FooterComponent,RouterModule],
  templateUrl: './landing.component.html',
})
export class LandingComponent {
  constructor(private router: Router) {}

  goToCatalogue(): void {
    sessionStorage.removeItem('catalogue_current_page');
    this.router.navigate(['/recrutement/catalogue']);
  }

  programmesPhares = programmes;
}