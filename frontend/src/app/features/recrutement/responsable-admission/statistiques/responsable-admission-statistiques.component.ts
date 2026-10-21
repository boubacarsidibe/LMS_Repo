import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { CandidatureResponse, RecrutementService } from '../../../../core/recrutement/services/recrutement.service';
import { downloadBlob } from '../../../../shared/utils/download-file';
import { NotificationService } from '../../../../shared/components/ui/notification.service';
import {
  estAdmission,
  estATraiter,
  estEnEvaluation,
  estRejete,
  GROUPES_STATUTS,
} from '../../../../core/recrutement/statuts-candidature';
import { DashboardPreferences, DashboardPreferencesService } from '../services/dashboard-preferences.service';

interface SegmentStatut {
  statut: string;
  label: string;
  couleur: string;
  count: number;
  pourcentage: number;
}

/** Circonférence d'un cercle SVG de rayon 58 (r="58"), utilisée pour l'anneau de progression. */
const CIRCONFERENCE_ANNEAU = 2 * Math.PI * 58;

@Component({
  selector: 'app-responsable-admission-statistiques',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './responsable-admission-statistiques.component.html',
  styleUrl: './responsable-admission-statistiques.component.css',
})
export class ResponsableAdmissionStatistiquesComponent implements OnInit {
  candidatures: CandidatureResponse[] = [];
  loading = false;
  exporting = false;
  loadError = false;
  prefs: DashboardPreferences;

  constructor(
    private recrutementService: RecrutementService,
    private prefsService: DashboardPreferencesService,
    private notifications: NotificationService
  ) {
    this.prefs = this.prefsService.charger();
  }

  ngOnInit(): void {
    this.loading = true;
    this.loadError = false;
    this.recrutementService.getAllCandidatures().subscribe({
      next: (data) => (this.candidatures = data || []),
      error: () => { this.loadError = true; this.loading = false; },
      complete: () => (this.loading = false),
    });
  }

  get totalCandidats(): number {
    return this.candidatures.length;
  }

  /** Dossiers qui attendent la validation du responsable (SOUMIS, DOSSIER_VALIDE). */
  get validationEnCours(): number {
    return this.candidatures.filter((c) => estATraiter(c.statut)).length;
  }

  /** Dossiers présélectionnés et en cours d'évaluation : paiement, test, entretien. */
  get preselectionnes(): number {
    return this.candidatures.filter((c) => estEnEvaluation(c.statut)).length;
  }

  get rejetes(): number {
    return this.candidatures.filter((c) => estRejete(c.statut)).length;
  }

  /** Dépôts reçus par mois pendant l'année en cours. */
  get candidaturesParMois(): number[] {
    const valeurs = Array.from({ length: 12 }, () => 0);
    const annee = new Date().getFullYear();
    for (const candidature of this.candidatures) {
      const date = new Date(candidature.dateSoumission);
      if (!Number.isNaN(date.getTime()) && date.getFullYear() === annee) {
        valeurs[date.getMonth()]++;
      }
    }
    return valeurs;
  }

  get pointsGraphique(): string {
    const valeurs = this.candidaturesParMois;
    const maximum = Math.max(...valeurs, 1);
    return valeurs.map((valeur, index) => {
      const x = 24 + (index * 552) / 11;
      const y = 168 - (valeur * 136) / maximum;
      return `${x.toFixed(1)},${y.toFixed(1)}`;
    }).join(' ');
  }

  readonly moisGraphique = ['Jan.', 'Fév.', 'Mars', 'Avr.', 'Mai', 'Juin', 'Juil.', 'Août', 'Sept.', 'Oct.', 'Nov.', 'Déc.'];

  /** Répartition réelle des candidatures par étape du workflow, pour le graphique en barres. */
  get repartitionStatuts(): SegmentStatut[] {
    const total = this.candidatures.length || 1;
    // Une barre par étape du parcours (les onglets de la liste des candidatures) : vingt statuts seraient illisibles.
    const couleurs: Record<string, string> = {
      A_INSTRUIRE: 'bg-surface-tint', PRESELECTION: 'bg-secondary', TEST: 'bg-primary', ENTRETIEN: 'bg-primary-fixed-dim',
      LISTE_ATTENTE: 'bg-blue-500', ADMIS: 'bg-green-600', REFUSES: 'bg-error',
    };
    return GROUPES_STATUTS.filter((g) => g.statuts !== null).map((g) => {
      const count = this.candidatures.filter((c) => g.statuts!.includes(c.statut as never)).length;
      return { statut: g.cle, label: g.libelle, couleur: couleurs[g.cle] ?? 'bg-surface-tint', count, pourcentage: Math.round((count / total) * 100) };
    });
  }

  /** Candidatures ayant reçu une décision finale (admises ou rejetées). */
  get candidaturesDecidees(): number {
    return this.candidatures.filter((c) => estAdmission(c.statut) || estRejete(c.statut)).length;
  }

  /** Taux d'admission parmi les dossiers déjà tranchés (0 si aucune décision encore prise). */
  get tauxAdmission(): number {
    const decidees = this.candidaturesDecidees;
    if (decidees === 0) return 0;
    const admis = this.candidatures.filter((c) => estAdmission(c.statut)).length;
    return Math.round((admis / decidees) * 100);
  }

  get anneauDashOffset(): number {
    return CIRCONFERENCE_ANNEAU - (CIRCONFERENCE_ANNEAU * this.tauxAdmission) / 100;
  }

  get anneauCirconference(): number {
    return CIRCONFERENCE_ANNEAU;
  }

  /** Répartition par genre déclaré (TDR : statistiques démographiques). */
  get repartitionGenre(): SegmentStatut[] {
    const total = this.candidatures.length || 1;
    const definitions: Array<{ statut: string; label: string; couleur: string }> = [
      { statut: 'Homme', label: 'Homme', couleur: 'bg-primary' },
      { statut: 'Femme', label: 'Femme', couleur: 'bg-secondary' },
      { statut: 'Autre', label: 'Autre', couleur: 'bg-primary-fixed-dim' },
    ];
    const segments = definitions.map((d) => {
      const count = this.candidatures.filter((c) => c.candidatGenre === d.statut).length;
      return { ...d, count, pourcentage: Math.round((count / total) * 100) };
    });
    const nonRenseigne = this.candidatures.filter((c) => !c.candidatGenre?.trim()).length;
    if (nonRenseigne > 0) {
      segments.push({
        statut: 'NON_RENSEIGNE',
        label: 'Non renseigné',
        couleur: 'bg-surface-tint',
        count: nonRenseigne,
        pourcentage: Math.round((nonRenseigne / total) * 100),
      });
    }
    const nonClasse = this.totalCandidats - segments.reduce((sum, seg) => sum + seg.count, 0);
    if (nonClasse > 0) segments.push({ statut: 'NON_CLASSE', label: 'Non classé', couleur: 'bg-surface-tint', count: nonClasse, pourcentage: Math.round(nonClasse / total * 100) });
    return segments;
  }

  readonly couleursGenre = ['#5875d8', '#a79aea', '#70b9b0', '#cbd3df', '#cbd3df'];

  get gradientGenre(): string {
    if (!this.totalCandidats) return '#edf1f7';
    let offset = 0;
    const stops = this.repartitionGenre.map((seg, index) => {
      const start = offset;
      offset += seg.count / this.totalCandidats * 100;
      return `${this.couleursGenre[index]} ${start}% ${offset}%`;
    });
    return `conic-gradient(${stops.join(', ')})`;
  }

  get agesRenseignes(): number {
    return this.repartitionAge.reduce((sum, seg) => sum + seg.count, 0);
  }

  /** Âge courant calculé depuis la date de naissance déclarée. */
  private age(dateNaissance: string | null): number | null {
    if (!dateNaissance) return null;
    const naissance = new Date(dateNaissance);
    if (Number.isNaN(naissance.getTime())) return null;
    const aujourdhui = new Date();
    let age = aujourdhui.getFullYear() - naissance.getFullYear();
    const moisEcoule = aujourdhui.getMonth() - naissance.getMonth();
    if (moisEcoule < 0 || (moisEcoule === 0 && aujourdhui.getDate() < naissance.getDate())) age--;
    return age;
  }

  /** Répartition par tranche d'âge, calculée depuis la date de naissance. */
  get repartitionAge(): SegmentStatut[] {
    const total = this.candidatures.length || 1;
    const tranches: Array<{ statut: string; label: string; couleur: string; test: (a: number) => boolean }> = [
      { statut: 'MOINS_20', label: '< 20 ans', couleur: 'bg-surface-tint', test: (a) => a < 20 },
      { statut: '20_25', label: '20-25 ans', couleur: 'bg-secondary', test: (a) => a >= 20 && a <= 25 },
      { statut: '26_30', label: '26-30 ans', couleur: 'bg-primary', test: (a) => a >= 26 && a <= 30 },
      { statut: '31_40', label: '31-40 ans', couleur: 'bg-primary-fixed-dim', test: (a) => a >= 31 && a <= 40 },
      { statut: 'PLUS_40', label: '> 40 ans', couleur: 'bg-green-600', test: (a) => a > 40 },
    ];
    const ages = this.candidatures.map((c) => this.age(c.candidatDateNaissance)).filter((a): a is number => a !== null);
    return tranches.map((t) => {
      const count = ages.filter(t.test).length;
      return { statut: t.statut, label: t.label, couleur: t.couleur, count, pourcentage: Math.round((count / total) * 100) };
    });
  }

  /** Professions déclarées les plus fréquentes (top 6, regroupées sans casse ni espaces superflus). */
  get topProfessions(): SegmentStatut[] {
    const total = this.candidatures.length || 1;
    const compte = new Map<string, number>();
    for (const c of this.candidatures) {
      const p = (c.candidatProfession || '').trim();
      if (!p) continue;
      const cle = p.toLowerCase();
      compte.set(cle, (compte.get(cle) || 0) + 1);
    }
    const couleurs = ['bg-primary', 'bg-secondary', 'bg-primary-fixed-dim', 'bg-green-600', 'bg-surface-tint', 'bg-error'];
    return Array.from(compte.entries())
      .sort((a, b) => b[1] - a[1])
      .slice(0, 6)
      .map(([label, count], i) => ({
        statut: label,
        label: label.charAt(0).toUpperCase() + label.slice(1),
        couleur: couleurs[i % couleurs.length],
        count,
        pourcentage: Math.round((count / total) * 100),
      }));
  }

  get nbSansProfessionDeclaree(): number {
    return this.candidatures.filter((c) => !c.candidatProfession || !c.candidatProfession.trim()).length;
  }

  /** Nationalités déclarées les plus fréquentes (top 6). */
  get topNationalites(): SegmentStatut[] {
    const total = this.candidatures.length || 1;
    const compte = new Map<string, number>();
    for (const c of this.candidatures) {
      const n = (c.candidatNationalite || '').trim();
      if (!n) continue;
      compte.set(n, (compte.get(n) || 0) + 1);
    }
    const couleurs = ['bg-primary', 'bg-secondary', 'bg-primary-fixed-dim', 'bg-green-600', 'bg-surface-tint', 'bg-error'];
    return Array.from(compte.entries())
      .sort((a, b) => b[1] - a[1])
      .slice(0, 6)
      .map(([label, count], i) => ({
        statut: label,
        label,
        couleur: couleurs[i % couleurs.length],
        count,
        pourcentage: Math.round((count / total) * 100),
      }));
  }

  exporter(): void {
    this.exporting = true;
    this.recrutementService.exporterCandidatures().subscribe({
      next: (blob) => {
        downloadBlob(blob, `candidatures_statistiques_${new Date().toISOString().slice(0, 10)}.xlsx`);
        this.exporting = false;
      },
      error: () => {
        this.exporting = false;
        this.notifications.erreur("Échec de l'export Excel.");
      },
    });
  }
}
