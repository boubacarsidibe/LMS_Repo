import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SUJETS_FORUM_MOCK, SujetForum } from '../../data/forum.mock';
import { Auth } from '../../../../core/auth/services/authService';

@Component({
  selector: 'app-forum',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './forum.html',
  styleUrl: './forum.css',
})
export class Forum {
  sujets: SujetForum[] = SUJETS_FORUM_MOCK;
  sujetSelectionne: SujetForum = this.sujets[0];
  filtreActif: 'tous' | 'non-lus' | 'prioritaires' = 'tous';
  nouveauMessage: string = '';
  marquerCommeSolution: boolean = false;
  private currentUserName: string | null = null;

  constructor(private auth: Auth) {
    const user = this.auth.currentUser();
    this.currentUserName = user ? user.username : null;
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
    if (!this.nouveauMessage.trim()) return;

    const auteur = this.currentUserName || 'Anonyme';
    const initiales = auteur.split(' ').map(n => n.charAt(0)).slice(0,2).join('').toUpperCase();

    this.sujetSelectionne.messages.push({
      auteur,
      initiales,
      date: "à l'instant",
      contenu: this.nouveauMessage
    });

    if (this.marquerCommeSolution) {
      this.sujetSelectionne.statut = 'resolu';
    }

    console.log('Réponse publiée (mock, pas encore persistée en base) :', this.nouveauMessage);
    // TODO : appel API réel vers le backend une fois l'endpoint disponible

    this.nouveauMessage = '';
    this.marquerCommeSolution = false;
  }
}