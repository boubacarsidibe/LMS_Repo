import { Injectable } from '@angular/core';
import { Formation } from '../models/formation';

const STORAGE_KEY = 'espace_etudiant_formations_recentes';
const MAX_ENTRIES = 5;

/**
 * Suivi côté client des formations réellement consultées par l'étudiant
 * (clic sur "Continuer le cours"). Aucune page de contenu de cours n'existe
 * encore côté backend pour horodater un vrai accès serveur : on enregistre
 * donc l'interaction réelle dans le navigateur plutôt que d'afficher une
 * fausse date de consultation.
 */
@Injectable({ providedIn: 'root' })
export class RecentlyViewedService {
  recordView(formation: Formation): void {
    if (!formation || formation.id === undefined) return;
    const current = this.getRecent().filter(f => f.id !== formation.id);
    current.unshift(formation);
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(current.slice(0, MAX_ENTRIES)));
    } catch {
      // stockage indisponible (mode privé, quota) : on ignore silencieusement
    }
  }

  getRecent(): Formation[] {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? JSON.parse(raw) : [];
    } catch {
      return [];
    }
  }
}
