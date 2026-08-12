import { Injectable } from '@angular/core';
import { Auth } from '../../../../core/auth/services/authService';

export interface DashboardPreferences {
  showTotalCandidats: boolean;
  showValidationEnCours: boolean;
  showPreselectionnes: boolean;
  showRejetes: boolean;
  showRepartitionStatuts: boolean;
  showTauxAdmission: boolean;
  showSessionsActives: boolean;
  showDemographie: boolean;
}

const DEFAUT: DashboardPreferences = {
  showTotalCandidats: true,
  showValidationEnCours: true,
  showPreselectionnes: true,
  showRejetes: true,
  showRepartitionStatuts: true,
  showTauxAdmission: true,
  showSessionsActives: true,
  showDemographie: true,
};

/**
 * Préférences d'affichage du dashboard « Vue d'ensemble ».
 *
 * Stockées en localStorage (clé par nom d'utilisateur) faute d'endpoint
 * backend dédié aux préférences utilisateur : ça personnalise l'affichage
 * par navigateur, mais ne suit pas le compte d'un appareil à l'autre.
 */
@Injectable({ providedIn: 'root' })
export class DashboardPreferencesService {
  constructor(private auth: Auth) {}

  private cle(): string {
    const user = this.auth.currentUser();
    return `dashboard_prefs_${user?.username || 'anonyme'}`;
  }

  charger(): DashboardPreferences {
    try {
      const brut = localStorage.getItem(this.cle());
      return brut ? { ...DEFAUT, ...JSON.parse(brut) } : { ...DEFAUT };
    } catch {
      return { ...DEFAUT };
    }
  }

  enregistrer(prefs: DashboardPreferences): void {
    localStorage.setItem(this.cle(), JSON.stringify(prefs));
  }
}
