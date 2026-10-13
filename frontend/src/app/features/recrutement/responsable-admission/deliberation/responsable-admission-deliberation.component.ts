
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import {
  CandidatureResponse,
  EntretienResponse,
  RecrutementService,
  TestAcademiqueResponse,
} from '../../../../core/recrutement/services/recrutement.service';
import { downloadBlob } from '../../../../shared/utils/download-file';
import { NotificationService } from '../../../../shared/components/ui/notification.service';
import { estAdmis, peutFaire, peutRejeter } from '../../statuts-admission';

interface LigneDeliberation {
  candidature: CandidatureResponse;
  noteTest: number | null;
  noteEntretien: number | null;
  moyenne: number | null;
}

@Component({
  selector: 'app-responsable-admission-deliberation',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './responsable-admission-deliberation.component.html',
})
export class ResponsableAdmissionDeliberationComponent implements OnInit {
  candidatures: CandidatureResponse[] = [];
  entretiens: EntretienResponse[] = [];
  tests: TestAcademiqueResponse[] = [];
  loading = false;
  errorMessage = '';
  private chargementsEnCours = 0;

  recherche = '';
  filtre: 'TOUS' | 'EN_ATTENTE' | 'ADMIS' | 'LISTE_ATTENTE' | 'REJETES' = 'EN_ATTENTE';
  actionEnCours = false;
  exporting = false;

  constructor(private recrutementService: RecrutementService, private notifications: NotificationService) {}

  ngOnInit(): void {
    this.charger();
  }

  private charger(): void {
    this.loading = true;
    this.errorMessage = '';
    this.chargementsEnCours = 3;

    this.recrutementService.getAllCandidatures().subscribe({
      next: (data) => (this.candidatures = data || []),
      error: () => {
        this.candidatures = [];
        this.errorMessage = 'Impossible de charger les candidatures.';
      },
      complete: () => this.finDeChargement(),
    });
    this.recrutementService.getAllEntretiens().subscribe({
      next: (data) => (this.entretiens = data || []),
      error: () => (this.entretiens = []),
      complete: () => this.finDeChargement(),
    });
    this.recrutementService.getAllTests().subscribe({
      next: (data) => (this.tests = data || []),
      error: () => (this.tests = []),
      complete: () => this.finDeChargement(),
    });
  }

  private finDeChargement(): void {
    this.chargementsEnCours--;
    if (this.chargementsEnCours <= 0) this.loading = false;
  }

  /** Moyenne pondérée par coefficient des épreuves notées du candidat, ou null s'il n'en a aucune. */
  private noteTestDe(candidatId: number): number | null {
    const testsDuCandidat = this.tests.filter((t) => t.candidatId === candidatId && t.noteObtenue !== null);
    if (testsDuCandidat.length === 0) return null;
    const sommeCoeffs = testsDuCandidat.reduce((s, t) => s + (t.coefficient || 1), 0);
    const sommePonderee = testsDuCandidat.reduce((s, t) => s + t.noteObtenue! * (t.coefficient || 1), 0);
    return sommeCoeffs > 0 ? Math.round((sommePonderee / sommeCoeffs) * 100) / 100 : null;
  }

  /** Note du dernier entretien effectué par le candidat (s'il en a plusieurs). */
  private noteEntretienDe(candidatId: number): number | null {
    const entretiensDuCandidat = this.entretiens
      .filter((e) => e.candidatId === candidatId && e.noteEntretien !== null)
      .sort((a, b) => b.dateHeure.localeCompare(a.dateHeure));
    return entretiensDuCandidat.length > 0 ? entretiensDuCandidat[0].noteEntretien : null;
  }

  get lignes(): LigneDeliberation[] {
    // Seules les candidatures dont l'entretien a eu lieu (ENTRETIEN_EFFECTUE), ou déjà tranchées à cette étape
    // (admises, sur liste complémentaire, non admises), ont leur place en délibération. Un dossier rejeté dès
    // l'examen (DOSSIER_REJETE) n'a jamais été jugé par le jury : il n'y figure pas.
    const eligibles = this.candidatures.filter((c) =>
      c.statut === 'ENTRETIEN_EFFECTUE' || estAdmis(c.statut) || c.statut === 'LISTE_ATTENTE' || c.statut === 'NON_ADMIS'
    );

    return eligibles
      .map((c) => {
        const noteTest = this.noteTestDe(c.candidatId);
        const noteEntretien = this.noteEntretienDe(c.candidatId);
        const notes = [noteTest, noteEntretien].filter((n): n is number => n !== null);
        const moyenne = notes.length > 0 ? Math.round((notes.reduce((s, n) => s + n, 0) / notes.length) * 100) / 100 : null;
        return { candidature: c, noteTest, noteEntretien, moyenne };
      })
      .sort((a, b) => (b.moyenne ?? -1) - (a.moyenne ?? -1));
  }

  get lignesFiltrees(): LigneDeliberation[] {
    const q = this.recherche.trim().toLowerCase();
    return this.lignes.filter((l) => {
      if (q) {
        const texte = `${l.candidature.candidatPrenom} ${l.candidature.candidatNom} ${l.candidature.numeroDossier}`.toLowerCase();
        if (!texte.includes(q)) return false;
      }
      switch (this.filtre) {
        case 'EN_ATTENTE': return l.candidature.statut === 'ENTRETIEN_EFFECTUE';
        case 'ADMIS': return estAdmis(l.candidature.statut);
        case 'LISTE_ATTENTE': return l.candidature.statut === 'LISTE_ATTENTE';
        case 'REJETES': return l.candidature.statut === 'NON_ADMIS';
        default: return true;
      }
    });
  }

  get nbEnAttente(): number {
    return this.lignes.filter((l) => l.candidature.statut === 'ENTRETIEN_EFFECTUE').length;
  }

  get nbAdmis(): number {
    return this.lignes.filter((l) => estAdmis(l.candidature.statut)).length;
  }

  get nbRejetes(): number {
    return this.lignes.filter((l) => l.candidature.statut === 'NON_ADMIS').length;
  }

  get nbListeAttente(): number {
    return this.lignes.filter((l) => l.candidature.statut === 'LISTE_ATTENTE').length;
  }

  /** Ce que le jury peut encore faire de ce dossier : décidé par le serveur, comme partout dans l'espace admission. */
  peutAdmettre(l: LigneDeliberation): boolean { return peutFaire(l.candidature, 'ADMETTRE_FINAL'); }
  peutMettreEnListe(l: LigneDeliberation): boolean { return peutFaire(l.candidature, 'PLACER_LISTE_ATTENTE'); }
  peutRefuser(l: LigneDeliberation): boolean { return peutRejeter(l.candidature); }
  /** Une décision reste à prendre (ou à reprendre, pour la liste complémentaire). */
  decisionEncorePossible(l: LigneDeliberation): boolean { return this.peutAdmettre(l) || this.peutMettreEnListe(l) || this.peutRefuser(l); }

  /** Pastille du statut : admis, non retenu, liste complémentaire, ou en attente de la décision du jury. */
  libelleDecision(l: LigneDeliberation): string {
    const s = l.candidature.statut;
    return estAdmis(s) ? 'Admis' : s === 'NON_ADMIS' ? 'Non retenu' : s === 'LISTE_ATTENTE' ? 'Liste complémentaire' : 'En attente';
  }

  classeDecision(l: LigneDeliberation): Record<string, boolean> {
    const s = l.candidature.statut;
    return {
      'bg-success-container text-on-success-container border-success-outline': estAdmis(s),
      'bg-error-container/20 text-error border-transparent': s === 'NON_ADMIS',
      'bg-primary-container/40 text-primary border-primary-fixed-dim': s === 'LISTE_ATTENTE',
      'bg-warning-container text-on-warning-container border-warning-outline': s === 'ENTRETIEN_EFFECTUE',
    };
  }

  /** Retenu par le jury mais sans place disponible (TDR §3.1.C) : ni admis, ni rejeté. */
  placerEnListeAttente(l: LigneDeliberation): void {
    this.actionEnCours = true;
    this.recrutementService.placerEnListeAttente(l.candidature.id).subscribe({
      next: () => {
        this.actionEnCours = false;
        this.charger();
      },
      error: (err) => {
        this.actionEnCours = false;
        this.notifications.erreur(err.error?.message || 'Échec du placement en liste complémentaire.');
      },
    });
  }

  admettreFinal(l: LigneDeliberation): void {
    this.actionEnCours = true;
    this.recrutementService.admettreFinal(l.candidature.id).subscribe({
      next: () => {
        this.actionEnCours = false;
        this.charger();
      },
      error: (err) => {
        this.actionEnCours = false;
        this.notifications.erreur(err.error?.message || "Échec de l'admission finale.");
      },
    });
  }

  rejeter(l: LigneDeliberation): void {
    const motif = window.prompt(`Motif de rejet pour ${l.candidature.numeroDossier} :`);
    if (motif === null) return;
    if (!motif.trim()) {
      this.notifications.erreur('Un motif de rejet est requis.');
      return;
    }
    this.actionEnCours = true;
    this.recrutementService.rejeterDossier(l.candidature.id, motif.trim()).subscribe({
      next: () => {
        this.actionEnCours = false;
        this.charger();
      },
      error: (err) => {
        this.actionEnCours = false;
        this.notifications.erreur(err.error?.message || 'Échec du rejet.');
      },
    });
  }

  exporter(): void {
    this.exporting = true;
    this.recrutementService.exporterCandidatures().subscribe({
      next: (blob) => {
        downloadBlob(blob, `deliberation_candidatures_${new Date().toISOString().slice(0, 10)}.xlsx`);
        this.exporting = false;
      },
      error: () => {
        this.exporting = false;
        this.notifications.erreur("Échec de l'export Excel.");
      },
    });
  }
}
