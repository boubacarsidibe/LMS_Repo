import { Component, HostListener, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MessageForum, SujetForum } from '../../models/sujet';
import { SujetService } from '../../services/sujet.service';
import { versHtmlForum } from '../../utils/formatage-forum';
import { ConfirmationService } from '../../../../shared/components/ui/confirmation.service';

@Component({
  selector: 'app-forum',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './forum.html',
  styleUrl: './forum.css',
})
export class Forum implements OnInit {
  sujets: SujetForum[] = [];
  sujetSelectionne: SujetForum | null = null;
  filtreActif: 'tous' | 'non-lus' | 'prioritaires' = 'tous';
  nouveauMessage: string = '';
  marquerCommeSolution: boolean = false;
  isSending = false;
  errorMessage = '';
  optionsOuvertes = false;
  fichierSelectionne: File | null = null;
  telechargementEnCoursId: number | null = null;
  readonly caractereCode = '`';

  constructor(private sujetService: SujetService,
    private confirmation: ConfirmationService
  ) {}

  ngOnInit(): void {
    this.sujetService.getAll().subscribe({
      next: (data) => {
        this.sujets = data;
        this.sujetSelectionne = this.sujets[0] ?? null;
      },
      error: () => {
        this.sujets = [];
        this.sujetSelectionne = null;
      },
    });
  }

  /**
   * Même définition que le filtre « Non lus » ci-dessous (statut nouveau) :
   * cette carte affichait un « 05 » codé en dur, sans lien avec les sujets
   * réellement chargés — contradiction flagrante pour un formateur dont la
   * liste est vide.
   */
  get sujetsNonLus(): number {
    return this.sujets.filter(s => s.statut === 'nouveau').length;
  }

  get sujetsFiltres(): SujetForum[] {
    if (this.filtreActif === 'non-lus') {
      return this.sujets.filter(s => s.statut === 'nouveau');
    }
    if (this.filtreActif === 'prioritaires') {
      return this.sujets.filter(s => s.statut === 'urgent');
    }
    return this.sujets;
  }

  selectionnerSujet(sujet: SujetForum) {
    this.sujetSelectionne = sujet;
    this.nouveauMessage = '';
    this.marquerCommeSolution = false;
    this.errorMessage = '';
    this.optionsOuvertes = false;
    this.fichierSelectionne = null;
  }

  onFichierSelectionne(event: Event) {
    const input = event.target as HTMLInputElement;
    this.fichierSelectionne = input.files?.[0] ?? null;
  }

  retirerFichier() {
    this.fichierSelectionne = null;
  }

  convertirEnHtml(contenu: string): string {
    return versHtmlForum(contenu);
  }

  appliquerFormat(prefixe: string, suffixe: string, placeholder: string, textarea: HTMLTextAreaElement): void {
    const debut = textarea.selectionStart;
    const fin = textarea.selectionEnd;
    const selection = this.nouveauMessage.substring(debut, fin) || placeholder;
    const avant = this.nouveauMessage.substring(0, debut);
    const apres = this.nouveauMessage.substring(fin);
    this.nouveauMessage = avant + prefixe + selection + suffixe + apres;
    this.repositionnerCurseur(textarea, debut + prefixe.length, debut + prefixe.length + selection.length);
  }

  insererLien(textarea: HTMLTextAreaElement): void {
    const debut = textarea.selectionStart;
    const fin = textarea.selectionEnd;
    const texteLien = this.nouveauMessage.substring(debut, fin) || 'texte du lien';
    const avant = this.nouveauMessage.substring(0, debut);
    const apres = this.nouveauMessage.substring(fin);
    const insertion = `[${texteLien}](https://)`;
    this.nouveauMessage = avant + insertion + apres;
    const debutUrl = debut + texteLien.length + 3;
    this.repositionnerCurseur(textarea, debutUrl, debutUrl + 8);
  }

  insererListe(textarea: HTMLTextAreaElement): void {
    const debut = textarea.selectionStart;
    const avant = this.nouveauMessage.substring(0, debut);
    const apres = this.nouveauMessage.substring(debut);
    const sautDeLigne = avant.length === 0 || avant.endsWith('\n') ? '' : '\n';
    const insertion = `${sautDeLigne}- `;
    this.nouveauMessage = avant + insertion + apres;
    const position = debut + insertion.length;
    this.repositionnerCurseur(textarea, position, position);
  }

  private repositionnerCurseur(textarea: HTMLTextAreaElement, debut: number, fin: number): void {
    setTimeout(() => {
      textarea.focus();
      textarea.setSelectionRange(debut, fin);
    });
  }

  telechargerPieceJointe(message: MessageForum) {
    if (!message.pieceJointeNom) return;
    this.telechargementEnCoursId = message.id;
    this.sujetService.telechargerPieceJointe(message.id).subscribe({
      next: (blob) => {
        this.telechargementEnCoursId = null;
        const url = window.URL.createObjectURL(blob);
        const lien = document.createElement('a');
        lien.href = url;
        lien.download = message.pieceJointeNom!;
        lien.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.telechargementEnCoursId = null;
      },
    });
  }

  toggleOptions() {
    this.optionsOuvertes = !this.optionsOuvertes;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (target.closest('#options-sujet-btn') || target.closest('#options-sujet-dropdown')) {
      return;
    }
    this.optionsOuvertes = false;
  }

  toggleUrgence() {
    if (!this.sujetSelectionne) return;
    const sujetId = this.sujetSelectionne.id;
    this.sujetService.toggleUrgence(sujetId).subscribe({
      next: (sujetMisAJour) => this.appliquerMiseAJour(sujetId, sujetMisAJour),
    });
  }

  marquerCommeResolu() {
    if (!this.sujetSelectionne) return;
    const sujetId = this.sujetSelectionne.id;
    this.optionsOuvertes = false;
    this.sujetService.marquerResolu(sujetId).subscribe({
      next: (sujetMisAJour) => this.appliquerMiseAJour(sujetId, sujetMisAJour),
    });
  }

  supprimerSujet() {
    if (!this.sujetSelectionne) return;
    const sujetId = this.sujetSelectionne.id;
    this.optionsOuvertes = false;
    this.confirmation.demander({
      titre: 'Supprimer le sujet',
      message: `« ${this.sujetSelectionne.titre} » et toutes ses réponses seront définitivement supprimés.`,
      libelleConfirmer: 'Supprimer',
      destructif: true,
    }).subscribe((confirme) => {
      if (!confirme) return;
      this.sujetService.supprimer(sujetId).subscribe({
        next: () => {
          this.sujets = this.sujets.filter(s => s.id !== sujetId);
          this.sujetSelectionne = this.sujets[0] ?? null;
        },
      });
    });
  }

  private appliquerMiseAJour(sujetId: number, sujetMisAJour: SujetForum) {
    this.sujets = this.sujets.map(s => (s.id === sujetId ? sujetMisAJour : s));
    if (this.sujetSelectionne?.id === sujetId) {
      this.sujetSelectionne = sujetMisAJour;
    }
  }

  classeStatut(statut: string): string {
    switch (statut) {
      case 'nouveau': return 'bg-secondary-container/30 text-on-secondary-container';
      case 'urgent': return 'bg-error-container/20 text-error';
      case 'resolu': return 'bg-surface-container-highest text-on-surface-variant';
      default: return '';
    }
  }

  envoyerReponse() {
    if (!this.nouveauMessage.trim() || !this.sujetSelectionne || this.isSending) return;

    const sujetId = this.sujetSelectionne.id;
    this.isSending = true;
    this.errorMessage = '';
    this.sujetService.repondre(sujetId, this.nouveauMessage.trim(), this.marquerCommeSolution, this.fichierSelectionne).subscribe({
      next: (sujetMisAJour) => {
        this.isSending = false;
        this.sujets = this.sujets.map(s => (s.id === sujetId ? sujetMisAJour : s));
        this.sujetSelectionne = sujetMisAJour;
        this.nouveauMessage = '';
        this.marquerCommeSolution = false;
        this.fichierSelectionne = null;
      },
      error: (error) => {
        this.isSending = false;
        this.errorMessage = error.error?.message || "L'envoi du message a échoué.";
      },
    });
  }
}