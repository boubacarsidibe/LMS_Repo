import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import {
  RecrutementService,
  CandidatureResponse,
  ParcoursCandidature,
  EntretienCandidat,
} from '../../../core/recrutement/services/recrutement.service';
import { FooterComponent } from '../../../shared/components/footer/footer.component';
import { CandidatNavbarComponent } from '../../../shared/components/candidat-navbar/candidat-navbar.component';

interface TimelineItem {
  label: string;
  desc: string;
  pending: boolean;
  active: boolean;
  erreur: boolean;
}

@Component({
  selector: 'app-suivi',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, FooterComponent, CandidatNavbarComponent],
  templateUrl: './suivi.component.html'
})
export class SuiviComponent implements OnInit {
  candidature: CandidatureResponse | null = null;
  loading = true;

  steps: string[] = ['Dossier', 'Évaluation', 'Test Académique', 'Entretien'];

  /** Étapes disponibles selon le statut */
  readonly STATUT_SOUMIS = 'SOUMIS';
  readonly STATUT_PRESELECTION = 'EN_COURS_DE_PRESELECTION';
  readonly STATUT_REJETE = 'REJETE';
  readonly STATUT_ADMIS1 = 'ADMIS1';
  readonly STATUT_ADMIS2 = 'ADMIS2';
  readonly STATUT_ADMIS3 = 'ADMIS3';

  /** Index de l'étape active dans le stepper (0 = Dossier, 1 = Évaluation, 2 = Test Académique, 3 = Entretien) */
  get activeStepIndex(): number {
    const statut = this.candidature?.statut;
    if (!statut || statut === 'BROUILLON') return 0;
    switch (statut) {
      case this.STATUT_SOUMIS:
      case this.STATUT_PRESELECTION: return 1; // Évaluation
      case this.STATUT_REJETE: return 2; // Bloqué au Test Académique
      case this.STATUT_ADMIS1: return 3; // Entretien
      case this.STATUT_ADMIS2: return 3;        // Entretien
      case this.STATUT_ADMIS3: return 4; // Terminé
      default: return 1;
    }
  }

  /** Indique si l'étape "step" (0-3) est complétée */
  isStepCompleted(stepIndex: number): boolean {
    return stepIndex < this.activeStepIndex;
  }

  /**
   * Interlocuteur affiché. Aucun nom de personne n'est inventé : le dossier n'est
   * rattaché à aucun agent en base, seul le service l'est.
   */
  readonly responsableAdmissions = 'Service des admissions';

  /**
   * Parcours réel du candidat. Ces valeurs étaient auparavant codées en dur dans
   * le composant — un score de 88, un seuil de 60, un entretien le « Mardi 24 Oct.
   * » — alors que les tables tests_academiques et entretiens contenaient les
   * vraies données et n'étaient jamais lues.
   */
  parcours: ParcoursCandidature | null = null;

  /** Note globale pondérée par les coefficients, ou null si aucune épreuve saisie. */
  get noteGlobale(): number | null {
    const tests = (this.parcours?.tests ?? []).filter((t) => t.noteObtenue != null);
    if (tests.length === 0) return null;
    const poidsTotal = tests.reduce((s, t) => s + (t.coefficient ?? 1), 0);
    if (poidsTotal === 0) return null;
    const somme = tests.reduce((s, t) => s + (t.noteObtenue ?? 0) * (t.coefficient ?? 1), 0);
    return Math.round((somme / poidsTotal) * 100) / 100;
  }

  get seuilScore(): number | null {
    return this.parcours?.seuilReussiteTest ?? null;
  }

  get aDesResultats(): boolean {
    return this.noteGlobale !== null;
  }

  /** Prochain entretien planifié, s'il existe. */
  get entretien(): EntretienCandidat | null {
    const entretiens = this.parcours?.entretiens ?? [];
    return entretiens.length > 0 ? entretiens[entretiens.length - 1] : null;
  }

  get lieuEntretien(): string {
    return this.entretien?.lieuOuLien || 'Lieu communiqué par courriel';
  }

  get mapUrl(): string {
    const lieu = this.entretien?.lieuOuLien;
    return lieu
      ? 'https://www.google.com/maps/search/' + encodeURIComponent(lieu)
      : 'https://www.google.com/maps/search/INPG+Dakar';
  }

  constructor(private recrutementService: RecrutementService) {}

  ngOnInit(): void {
    this.recrutementService.getMesCandidatures().subscribe({
      next: (data) => {
        // Prendre la candidature la plus récente (tous statuts)
        this.candidature = data.length > 0 ? data[data.length - 1] : null;
        this.loading = false;
        if (this.candidature?.id) {
          this.recrutementService.getParcoursCandidature(this.candidature.id).subscribe({
            next: (parcours) => (this.parcours = parcours),
            // Absence de parcours = aucune épreuve encore saisie : les blocs
            // correspondants restent simplement masqués.
            error: () => (this.parcours = null),
          });
        }
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  /** Vérifie si le statut courant correspond à l'un des statuts listés */
  isStatut(...statuts: string[]): boolean {
    return statuts.includes(this.candidature?.statut ?? '');
  }

  get timelineItems(): TimelineItem[] {
    const statut = this.candidature?.statut;
    const items: TimelineItem[] = [
      { label: 'Paiement confirmé', desc: 'Frais de dossier réglés.', pending: false, active: false, erreur: false },
      { label: 'Dossier soumis', desc: 'Tous les documents requis téléchargés.', pending: false, active: false, erreur: false },
      { label: 'Évaluation en cours', desc: 'Le jury examine votre candidature.', pending: false, active: false, erreur: false },
      { label: 'Test Académique', desc: '', pending: true, active: false, erreur: false },
      { label: 'Entretien', desc: 'Rencontre avec le jury.', pending: true, active: false, erreur: false },
    ];

    if (!statut || statut === 'BROUILLON') {
      items[2].pending = true;
      return items;
    }

    if (statut === this.STATUT_REJETE) {
      items[2].active = false;
      items[3].active = false;
      items[3].erreur = true;
      return items;
    }

    // SOUMIS ou EN_COURS_DE_PRESELECTION
    if (statut === this.STATUT_SOUMIS || statut === this.STATUT_PRESELECTION) {
      items[2].pending = false;
      items[2].active = true;
      return items;
    }

    // ADMIS1 : test académique réussi
    if (statut === this.STATUT_ADMIS1) {
      items[2].active = false;
      items[3].pending = false;
      items[3].active = true;
      return items;
    }

    // ADMIS2 : entretien réussi
    if (statut === this.STATUT_ADMIS2) {
      items[2].active = false;
      items[3].pending = false;
      items[3].active = false;
      return items;
    }

    // ADMIS3 : terminé
    if (statut === this.STATUT_ADMIS3) {
      items[2].active = false;
      items[3].pending = false;
      items[3].active = false;
      return items;
    }

    return items;
  }

  getDocIcon(type: string): string {
    switch (type) {
      case 'CV': return 'article';
      case 'PIECE_IDENTITE': return 'badge';
      case 'LETTRE_MOTIVATION': return 'edit_document';
      case 'RELEVE_NOTES': return 'description';
      case 'DIPLOME': return 'school';
      default: return 'description';
    }
  }

  getDocLabel(type: string): string {
    switch (type) {
      case 'CV': return 'CV Professionnel';
      case 'PIECE_IDENTITE': return 'Pièce d\'identité';
      case 'LETTRE_MOTIVATION': return 'Lettre de motivation';
      case 'RELEVE_NOTES': return 'Relevé de notes';
      case 'DIPLOME': return 'Diplôme';
      default: return type;
    }
  }

  getStatutBadgeClass(): string {
    const s = this.candidature?.statut;
    if (s === this.STATUT_REJETE) return 'bg-error-container text-on-error-container';
    if (s === this.STATUT_ADMIS1 || s === this.STATUT_ADMIS2 || s === this.STATUT_ADMIS3) return 'bg-emerald-100 text-emerald-700';
    return 'bg-secondary-container text-on-secondary-container';
  }

  getStatutBadgeLabel(): string {
    const s = this.candidature?.statut;
    switch (s) {
      case this.STATUT_SOUMIS: return 'Candidature en cours';
      case this.STATUT_PRESELECTION: return 'En cours de présélection';
      case this.STATUT_REJETE: return 'Test non concluant';
      case this.STATUT_ADMIS1: return 'Test réussi';
      case this.STATUT_ADMIS2: return 'Entretien réussi';
      case this.STATUT_ADMIS3: return 'ADMIS';
      default: return 'Candidature en cours';
    }
  }

  getHeaderTitle(): string {
    const s = this.candidature?.statut;
    switch (s) {
      case this.STATUT_REJETE: return 'Résultats du Test Académique';
      case this.STATUT_ADMIS1: return 'Résultats du Test Académique';
      case this.STATUT_ADMIS2: return 'Tu y es presque !';
      case this.STATUT_ADMIS3: return 'Félicitations ! Bienvenue à l\'INPG';
      default: return 'Votre dossier est en cours d\'évaluation.';
    }
  }

  getHeaderDescription(): string {
    const s = this.candidature?.statut;
    switch (s) {
      case this.STATUT_SOUMIS:
      case this.STATUT_PRESELECTION:
        return 'Félicitations pour avoir complété la première étape. Notre jury d\'admission examine actuellement vos documents avec la plus grande attention.';
      case this.STATUT_REJETE:
        return 'Malgré tout l\'intérêt que nous portons à votre candidature pour le Bachelor in Strategic Management, nous regrettons de vous informer que vos résultats au test ne nous permettent pas de poursuivre le processus d\'admission pour cette session.';
      case this.STATUT_ADMIS1:
        return 'Félicitations ! Vous avez brillamment réussi le test académique. La prochaine étape est l\'entretien.';
      case this.STATUT_ADMIS2:
        return 'Ton parcours de sélection avance avec succès. Ton dossier a retenu toute notre attention.';
      case this.STATUT_ADMIS3:
        return 'Votre candidature a été officiellement acceptée. Vous faites désormais partie de la promotion 2024-2025.';
      default: return '';
    }
  }

  /** Étape actuelle dans le parcours (pour admis2) */
  get etapeActuelle(): string {
    const s = this.candidature?.statut;
    if (s === this.STATUT_ADMIS2) return 'Étape 3 sur 4';
    if (s === this.STATUT_ADMIS3) return 'Étape 4 sur 4';
    return '';
  }

  // Ces 4 actions n'ont pas encore d'endpoint côté backend : on prévient
  // honnêtement le candidat plutôt que de laisser croire que l'action a été
  // enregistrée (aucun appel réseau, aucun effet réel).

  /** Confirmation de présence à l'entretien */
  confirmerPresence(): void {
    alert('La confirmation de présence en ligne sera bientôt disponible. En attendant, merci de contacter le service admission.');
  }

  /** Demande de report de créneau */
  demanderReport(): void {
    alert('La demande de report en ligne sera bientôt disponible. En attendant, merci de contacter le service admission.');
  }

  /** Télécharger convocation PDF */
  telechargerConvocation(): void {
    alert('Le téléchargement de la convocation sera bientôt disponible.');
  }

  /** Ajouter à l'agenda */
  ajouterAgenda(): void {
    alert('L\'ajout automatique à l\'agenda sera bientôt disponible.');
  }
}