import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { RecrutementService } from '../../../../core/recrutement/services/recrutement.service';

interface StatsKpi {
  totalCandidatures: number;
  variation: number;
  tauxAdmission: number;
  cibleAdmission: number;
  fraisCollectes: number;
  tempsTraitement: number;
  deltaTemps: number;
}

interface VolumeMonth {
  mois: string;
  total: number;
  validees: number;
  rejetees: number;
}

interface GeoEntry {
  pays: string;
  pourcentage: number;
  effectif: number;
}

interface FunnelStep {
  etape: string;
  nombre: number;
  pourcentage: number;
}

@Component({
  selector: 'app-responsable-admission-statistiques',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './responsable-admission-statistiques.component.html',
})
export class ResponsableAdmissionStatistiquesComponent implements OnInit, OnDestroy {
  anneeAcademique = '2026-2027';
  chargement = false;

  kpis: StatsKpi = { totalCandidatures: 0, variation: 0, tauxAdmission: 0, cibleAdmission: 15, fraisCollectes: 0, tempsTraitement: 0, deltaTemps: 0 };
  volumeByMonth: VolumeMonth[] = [];
  geoDistribution: GeoEntry[] = [];
  funnel: FunnelStep[] = [];

  maxVolume = 400; // pour les barres proportionnelles

  toastVisible = false;
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';

  private subs: Subscription[] = [];

  constructor(private service: RecrutementService) {}

  ngOnInit(): void { this.chargerTout(); }

  ngOnDestroy(): void { this.subs.forEach(s => s.unsubscribe()); }

  chargerTout(): void {
    this.chargement = true;
    this.chargerKpis();
    this.chargerVolume();
    this.chargerGeo();
    this.chargerFunnel();
    this.chargement = false;
  }

  chargerKpis(): void {
    const sub = this.service.getAnalyticsKpis().subscribe({
      next: d => { this.kpis = d; },
      error: () => { /* fallback mock already in TS defaults */ }
    });
    this.subs.push(sub);
  }

  chargerVolume(): void {
    const sub = this.service.getVolumeByMonth().subscribe({
      next: (d: VolumeMonth[]) => {
        this.volumeByMonth = d;
        this.maxVolume = Math.max(...d.map(v => v.total), 400);
      },
      error: () => { /* keep empty */ }
    });
    this.subs.push(sub);
  }

  chargerGeo(): void {
    const sub = this.service.getGeographicDistribution().subscribe({
      next: (d: GeoEntry[]) => { this.geoDistribution = d; },
      error: () => {}
    });
    this.subs.push(sub);
  }

  chargerFunnel(): void {
    const sub = this.service.getConversionFunnel().subscribe({
      next: (d: FunnelStep[]) => { this.funnel = d; },
      error: () => {}
    });
    this.subs.push(sub);
  }

  onAnneeChange(): void { this.chargerTout(); }

  exportReport(): void {
    const sub = this.service.exportAnalyticsReport(this.anneeAcademique).subscribe({
      next: blob => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a'); a.href = url;
        a.download = `rapport_statistiques_${this.anneeAcademique}.csv`;
        a.click(); window.URL.revokeObjectURL(url);
        this.afficherToast('Rapport analytique généré avec succès.', 'success');
      },
      error: () => this.afficherToast('Erreur lors de l\'export.', 'error')
    });
    this.subs.push(sub);
  }

  formatFrais(montant: number): string {
    if (montant >= 1000000) return (montant / 1000000).toFixed(1).replace('.0', '') + 'M FCFA';
    return montant.toLocaleString('fr') + ' FCFA';
  }

  barWidth(value: number): string {
    return this.maxVolume > 0 ? ((value / this.maxVolume) * 100) + '%' : '0%';
  }

  afficherToast(message: string, type: 'success' | 'error'): void {
    this.toastMessage = message; this.toastType = type; this.toastVisible = true;
    setTimeout(() => this.toastVisible = false, 4000);
  }
}