import { Component, OnInit } from '@angular/core';
import { FormationService } from '../../services/formation.service';
import { PromotionService } from '../../services/promotion.service';
import { SupportService } from '../../services/support.service';
import { Formation } from '../../models/formation';
import { Promotion } from '../../models/promotion';
import { Support } from '../../models/support';

@Component({
  selector: 'app-accueil',
  standalone: false,
  templateUrl: './accueil.html',
  styleUrl: './accueil.css',
})
export class Accueil implements OnInit {
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

  test() {
    console.log('clic détecté');
  }
}
