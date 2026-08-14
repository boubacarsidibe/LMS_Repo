import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormationService } from '../../services/formation.service';
import { CohorteService } from '../../services/cohorte.service';
import { SupportService } from '../../services/support.service';
import { Formation } from '../../models/formation';
import { Cohorte } from '../../models/cohorte';
import { Support } from '../../models/support';
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
  cohortes: Cohorte[] = [];
  supports: Support[] = [];

  constructor(
    private formationService: FormationService,
    private cohorteService: CohorteService,
    private supportService: SupportService
  ) {}

  ngOnInit(): void {
    this.formationService.getAll().subscribe({ next: (data) => this.formations = data || [], error: () => this.formations = [] });
    this.cohorteService.getAll().subscribe({ next: (data) => this.cohortes = data || [], error: () => this.cohortes = [] });
    this.supportService.getCourseSupports().subscribe({ next: (data) => this.supports = data || [], error: () => this.supports = [] });
  }

  get isEtudiantRole(): boolean {
    return this.auth.getUserRoles().includes('ROLE_ETUDIANT');
  }

  get isFormateurRole(): boolean {
    return this.auth.isFormateur();
  }
}
