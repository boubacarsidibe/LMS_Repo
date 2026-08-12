import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormationService } from '../../services/formation.service';
import { PromotionService } from '../../services/promotion.service';
import { SupportService } from '../../services/support.service';
import { Formation } from '../../models/formation';
import { Promotion } from '../../models/promotion';
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
  promotions: Promotion[] = [];
  supports: Support[] = [];

  constructor(
    private formationService: FormationService,
    private promotionService: PromotionService,
    private supportService: SupportService
  ) {}

  ngOnInit(): void {
    this.formationService.getAll().subscribe({ next: (data) => this.formations = data || [], error: () => this.formations = [] });
    this.promotionService.getAll().subscribe({ next: (data) => this.promotions = data || [], error: () => this.promotions = [] });
    this.supportService.getCourseSupports().subscribe({ next: (data) => this.supports = data || [], error: () => this.supports = [] });
  }

  get isEtudiantRole(): boolean {
    return this.auth.getUserRoles().includes('ROLE_ETUDIANT');
  }

  get isFormateurRole(): boolean {
    return this.auth.getUserRoles().includes('ROLE_FORMATEUR');
  }
}
