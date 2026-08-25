import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FooterComponent } from '../../../../shared/components/footer/footer.component';
import { NavbarComponent } from '../../../../shared/components/navbar/navbar.component';
import { Program } from '../../models/program';
import { RecrutementService } from '../../../../core/recrutement/services/recrutement.service';
import { RecrutementModule } from '../../recrutement.module';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    NavbarComponent,
    FooterComponent,
    RecrutementModule
  ],
  templateUrl: './landing.component.html',
})
export class LandingComponent implements OnInit {
  /** Programmes mis en avant, issus du catalogue réel et non plus d'un fichier figé. */
  programmesPhares: Program[] = [];

  private static readonly NB_PHARES = 6;

  constructor(private router: Router, private recrutementService: RecrutementService) {}

  ngOnInit(): void {
    this.recrutementService.getFormationsPubliques().subscribe({
      next: (formations) => {
        this.programmesPhares = formations.slice(0, LandingComponent.NB_PHARES).map((f) => ({
          id: f.id,
          titre: f.intitule,
          description: f.description ?? '',
          imageUrl: f.imageUrl ?? '',
          niveau: f.niveau ?? '',
          duree: f.duree ?? '',
        }));
      },
      // La page d'accueil reste consultable même si le catalogue est indisponible :
      // la section des programmes est simplement vide.
      error: () => (this.programmesPhares = []),
    });
  }

  goToCatalogue(): void {
    sessionStorage.removeItem('catalogue_current_page');
    this.router.navigate(['/recrutement/catalogue']);
  }
}