import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FooterComponent } from '../../../../shared/components/footer/footer.component';
import { NavbarComponent } from '../../../../shared/components/navbar/navbar.component';
import { RecrutementService } from '../../../../core/recrutement/services/recrutement.service';

export interface FormationDetailData {
  id: number; libelle: string; description: string; imageUrl: string; niveau: string; domaine: string; duree: string;
  badgeLabel?: string; badgeIcon?: string;
  heroMeta?: Array<{ icon: string; label: string; value: string }>;
  modules?: Array<{ titre: string; icon: string; items: string[]; colSpan?: 1|2; badge?: string; description?: string }>;
  methodePedagogique?: { description: string; items: string[]; sousSections?: Array<{ titre: string; items: string[] }> };
  pointsCles?: Array<{ icon: string; titre: string; description: string }>;
  debouches?: Array<{ icon: string; titre: string; description: string }>;
  ctaText?: string;
  sections?: Array<{ titre: string; icon: string; type: 'grid'|'list'|'cards'|'accordion'; items: Array<{ titre?: string; description?: string; icon?: string; badge?: string; sousItems?: string[] }> }>;
  sidebar?: { duree?: string; format?: string; langue?: string; certificat?: string; lieu?: string; tarif?: string; prochaineRentree?: string; dateLimite?: string; boutonTexte?: string; brochure?: boolean };
  /** §3 du Référentiel Accréditation DRCQ — encart affiché seulement si au moins un champ est renseigné. */
  heuresSimulationLabo?: number | null;
  anglaisProfessionnel?: boolean;
  softSkills?: boolean;
  creditsTotaux?: number | null;
  volumeHoraireTotal?: number | null;
}



@Component({
  selector: 'app-formation-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, FooterComponent, NavbarComponent],
  templateUrl: './formation-detail.component.html',
})
export class FormationDetailComponent implements OnInit {
  formation: FormationDetailData | null = null;
  notFound = false;
  chargement = true;

  constructor(private route: ActivatedRoute, private recrutementService: RecrutementService) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const id = idParam ? Number(idParam) : NaN;
    if (isNaN(id)) {
      this.notFound = true;
      this.chargement = false;
      return;
    }
    this.recrutementService.getFormationPublique(id).subscribe({
      next: (dto) => {
        // Le contenu éditorial est renvoyé dans la forme attendue par le gabarit :
        // les colonnes propres l'emportent, la fiche fournit le reste.
        this.formation = {
          ...(dto.ficheDetaillee ?? {}),
          id: dto.id,
          libelle: dto.intitule,
          description: (dto.ficheDetaillee?.['description'] as string) ?? dto.description ?? '',
          imageUrl: dto.imageUrl ?? '',
          niveau: dto.niveau ?? '',
          domaine: dto.domaine ?? '',
          duree: dto.duree ?? '',
          heuresSimulationLabo: dto.heuresSimulationLabo,
          anglaisProfessionnel: dto.anglaisProfessionnel,
          softSkills: dto.softSkills,
          creditsTotaux: dto.creditsTotaux,
          volumeHoraireTotal: dto.volumeHoraireTotal,
        } as FormationDetailData;
        this.chargement = false;
      },
      error: () => {
        this.notFound = true;
        this.chargement = false;
      },
    });
  }

  /** L'encart §3 « Conforme au référentiel DRCQ » ne s'affiche que si au moins un champ est renseigné. */
  get conformiteReferentielVisible(): boolean {
    const f = this.formation;
    if (!f) {
      return false;
    }
    return !!(f.heuresSimulationLabo || f.anglaisProfessionnel || f.softSkills || f.creditsTotaux || f.volumeHoraireTotal);
  }

  methodeIcon(item: string): string {
    const map: Record<string, string> = {
      'Cours théoriques': 'menu_book', 'Exercices': 'edit_note', 'Études de cas': 'assignment',
      'Travaux de recherche': 'search', 'E-learning': 'laptop_chromebook', 'Présentations': 'co_present',
      'Visites d’entreprises': 'factory', 'Conférences': 'mic', 'Mémoire': 'workspace_premium',
      'Voyage d’études': 'flight_takeoff',
    };
    return map[item] || 'check_circle';
  }
}