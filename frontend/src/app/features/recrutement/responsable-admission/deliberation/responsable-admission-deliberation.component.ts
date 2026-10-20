
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
import { ConfirmationService } from '../../../../shared/components/ui/confirmation.service';
import { estAdmission, STATUTS_A_DELIBERER } from '../../../../core/recrutement/statuts-candidature';

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

  constructor(private recrutementService: RecrutementService, private notifications: NotificationService,
              private confirmation: ConfirmationService) {}

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
    // Seules les candidatures dont l'entretien a eu lieu (ENTRETIEN_EFFECTUE), ou déjà décidées à cette étape
    // (admises, sur liste complémentaire, non retenues), ont leur place en délibération. Le filtre attendait les
    // cinq anciens statuts : la liste restait vide alors que des dossiers avaient passé l'entretien.
    const eligibles = this.candidatures.filter((c) =>
      (STATUTS_A_DELIBERER as readonly string[]).includes(c.statut) || estAdmission(c.statut) || c.statut === 'NON_ADMIS'
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
        case 'EN_ATTENTE': return this.issue(l) === 'attente';
        case 'ADMIS': return this.issue(l) === 'admis';
        case 'LISTE_ATTENTE': return this.issue(l) === 'liste';
        case 'REJETES': return this.issue(l) === 'non-retenu';
        default: return true;
      }
    });
  }

  /** Issue d'un dossier à la délibération : de quel côté de la décision il se trouve. */
  issue(l: LigneDeliberation): 'attente' | 'admis' | 'liste' | 'non-retenu' {
    const s = l.candidature.statut;
    if (estAdmission(s)) return 'admis';
    if (s === 'NON_ADMIS') return 'non-retenu';
    if (s === 'LISTE_ATTENTE') return 'liste';
    return 'attente';
  }

  libelleIssue(l: LigneDeliberation): string {
    switch (this.issue(l)) {
      case 'admis': return 'Admis';
      case 'non-retenu': return 'Non retenu';
      case 'liste': return 'Liste complémentaire';
      default: return 'En attente';
    }
  }

  /** Reste-t-il une décision à prendre ? (admettre, rejeter — et, après l'entretien, liste complémentaire). */
  aDeliberer(l: LigneDeliberation): boolean {
    return (STATUTS_A_DELIBERER as readonly string[]).includes(l.candidature.statut);
  }

  /** La liste complémentaire se décide à l'issue de l'entretien : un dossier qui y est déjà n'y retourne pas. */
  peutMettreEnListeAttente(l: LigneDeliberation): boolean {
    return l.candidature.statut === 'ENTRETIEN_EFFECTUE';
  }

  get nbEnAttente(): number {
    return this.lignes.filter((l) => this.issue(l) === 'attente').length;
  }

  get nbAdmis(): number {
    return this.lignes.filter((l) => this.issue(l) === 'admis').length;
  }

  get nbRejetes(): number {
    return this.lignes.filter((l) => this.issue(l) === 'non-retenu').length;
  }

  get nbListeAttente(): number {
    return this.lignes.filter((l) => this.issue(l) === 'liste').length;
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
    this.confirmation.demanderSaisie({
      titre: 'Ne pas retenir ce candidat',
      message: `Dossier ${l.candidature.numeroDossier} — ${l.candidature.candidatPrenom} ${l.candidature.candidatNom}.\nLe candidat sera informé de la décision du jury.`,
      libelleConfirmer: 'Confirmer le rejet',
      destructif: true,
      saisie: { libelle: 'Motif', obligatoire: true, placeholder: 'Ex. : résultats insuffisants à l\'entretien' },
    }).subscribe((motif) => {
      if (!motif) return;
      this.actionEnCours = true;
      this.recrutementService.rejeterDossier(l.candidature.id, motif).subscribe({
        next: () => {
          this.actionEnCours = false;
          this.charger();
        },
        error: (err) => {
          this.actionEnCours = false;
          this.notifications.erreur(err.error?.message || 'Échec du rejet.');
        },
      });
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
