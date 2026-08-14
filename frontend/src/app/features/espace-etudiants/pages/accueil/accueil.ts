import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormationService } from '../../services/formation.service';
import { Formation } from '../../models/formation';
import { Auth } from '../../../../core/auth/services/authService';
import { EspaceNavbarComponent } from '../../../../shared/components/espace-navbar/espace-navbar.component';

@Component({
  selector: 'app-accueil',
  standalone: true,
  imports: [CommonModule, RouterLink, EspaceNavbarComponent],
  templateUrl: './accueil.html',
  styleUrl: './accueil.css',
})
export class Accueil implements OnInit {
  private auth = inject(Auth);

  formations: Formation[] = [];

  constructor(private formationService: FormationService) {}

  ngOnInit(): void {
    if (this.isEtudiantRole) {
      this.formationService.getAll().subscribe({ next: (data) => this.formations = data || [], error: () => this.formations = [] });
    }
  }

  get isEtudiantRole(): boolean {
    return this.auth.getUserRoles().includes('ROLE_ETUDIANT');
  }

  get isFormateurRole(): boolean {
    return this.auth.getUserRoles().includes('ROLE_FORMATEUR');
  }
}
