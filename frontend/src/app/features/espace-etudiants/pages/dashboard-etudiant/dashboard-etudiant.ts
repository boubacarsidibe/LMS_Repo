import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { EtudiantService, EtudiantProfile } from '../../services/etudiant.service';
import { FormationService } from '../../services/formation.service';
import { SupportService } from '../../services/support.service';
import { RecentlyViewedService } from '../../services/recently-viewed.service';
import { Formation } from '../../models/formation';
import { Support } from '../../models/support';
import { EspaceNavbarComponent } from '../../../../shared/components/espace-navbar/espace-navbar.component';
import { FormationCard } from '../mes-formations/formation-card';

@Component({
  selector: 'app-dashboard-etudiant',
  standalone: true,
  imports: [CommonModule, RouterLink, EspaceNavbarComponent, FormationCard],
  templateUrl: './dashboard-etudiant.html',
})
export class DashboardEtudiant implements OnInit {
  profile: EtudiantProfile | null = null;
  formations: Formation[] = [];
  supports: Support[] = [];
  loading = true;

  constructor(
    private etudiantService: EtudiantService,
    private formationService: FormationService,
    private supportService: SupportService,
    private recentlyViewed: RecentlyViewedService
  ) {}

  ngOnInit(): void {
    this.etudiantService.getMyProfile().subscribe({
      next: (p) => (this.profile = p),
      error: () => (this.profile = null),
    });
    this.formationService.getAll().subscribe({
      next: (data) => { this.formations = data || []; this.loading = false; },
      error: () => { this.formations = []; this.loading = false; },
    });
    this.supportService.getCourseSupports().subscribe({
      next: (data) => (this.supports = data || []),
      error: () => (this.supports = []),
    });
  }

  get prenom(): string {
    if (this.profile?.etudiant?.prenom) return this.profile.etudiant.prenom;
    return this.profile?.username?.split('_')[0] || '';
  }

  /** Dernières formations réellement ouvertes par l'étudiant (voir RecentlyViewedService). */
  get formationsRecentes(): Formation[] {
    return this.recentlyViewed.getRecent();
  }
}
