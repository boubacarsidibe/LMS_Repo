import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { RecrutementService, CandidatureResponse } from '../../../core/recrutement/services/recrutement.service';
import { FooterComponent } from '../../../shared/components/footer/footer.component';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';

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
  imports: [CommonModule, FormsModule, RouterModule, FooterComponent, NavbarComponent],
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
      case this.STATUT_REJETE: return 2;        // Bloqué au Test Académique
      case this.STATUT_ADMIS1: return 2;        // Test Académique
      case this.STATUT_ADMIS2: return 3;        // Entretien
      case this.STATUT_ADMIS3: return 3;        // Entretien (terminé)
      default: return 1;
    }
  }

  /** Indique si l'étape "step" (0-3) est complétée */
  isStepCompleted(stepIndex: number): boolean {
    const actif = this.activeStepIndex;
    return stepIndex < actif;
  }

  /** Nom du responsable admissions */
  responsableAdmissions = 'Claire Vallet';

  /** Score au test académique */
  scoreTest = 88;
  seuilScore = 60;
  scoreObtenu = 42; // pour rejet

  /** Date d'entretien */
  dateEntretien = 'Mardi 24 Oct.';
  heureEntretien = '14h30';
  lieuEntretien = 'INPG Campus Dakar';
  adresseEntretien = 'VDN / Almadies, Dakar, Sénégal';

  get mapUrl(): string {
    return 'https://www.google.com/maps/search/' + encodeURIComponent(this.adresseEntretien || '');
  }

  constructor(private recrutementService: RecrutementService) {}

  ngOnInit(): void {
    this.recrutementService.getMesCandidatures().subscribe({
      next: (data) => {
        // Prendre la candidature la plus récente (tous statuts)
        this.candidature = data.length > 0 ? data[data.length - 1] : null;
        this.loading = false;
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
        return 'Malgré tout l\'intérêt que nous portons à votre candidature pour le Bachelor in Strategic Management, nous regrettons de vous informer que vos résultats au test ne nous permettent pas de poursuivre le processus d\'admission pour cette session.';
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

  /** Confirmation de présence à l'entretien */
  confirmerPresence(): void {
    console.log('Confirmation de présence à l\'entretien');
    // TODO: appel API
  }

  /** Demande de report de créneau */
  demanderReport(): void {
    console.log('Demande de report de créneau');
    // TODO: appel API
  }

  /** Télécharger convocation PDF */
  telechargerConvocation(): void {
    console.log('Téléchargement de la convocation');
    // TODO: appel API
  }

  /** Ajouter à l'agenda */
  ajouterAgenda(): void {
    console.log('Ajout à l\'agenda');
    // TODO: implémentation
  }
}