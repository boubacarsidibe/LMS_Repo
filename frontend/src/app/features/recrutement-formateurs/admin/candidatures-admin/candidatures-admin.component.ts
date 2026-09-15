import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import { UiEmptyStateComponent } from '../../../../shared/components/ui/ui-empty-state.component';
import { MediaProtegeService } from '../../../espace-etudiants/services/media-protege.service';
import {
  CandidatureFormateur,
  LIBELLES_DECISION,
  LIBELLES_NIVEAU_LANGUE,
  LIBELLES_STATUT_CANDIDATURE,
  LIBELLES_TYPE_DOCUMENT,
  StatutCandidatureFormateur,
} from '../../models/recrutement-formateur.models';
import { AdminRecrutementFormateurService } from '../../services/admin-recrutement-formateur.service';

/**
 * Espace admission — onglet Formateurs, sous-page « Candidatures » : tri et
 * sélection (QMS-007 §3-4). La décision RETENU crée le compte formateur (côté
 * back). Liste + panneau d'instruction, patron accréditation/demandes.
 */
@Component({
  selector: 'app-candidatures-admin-formateurs',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, UiSpinnerComponent, UiEmptyStateComponent],
  template: `
    <div class="p-margin-desktop space-y-stack-lg max-w-container-max mx-auto">
      <header>
        <p class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-widest mb-2 block">
          Recrutement des formateurs — QMS-007
        </p>
        <h1 class="font-display text-display text-on-surface leading-none -ml-1">Candidatures</h1>
      </header>

      <nav class="flex gap-2 border-b border-outline-variant">
        <a routerLink="/responsable-admission/recrutement-formateurs/offres"
           routerLinkActive="border-secondary text-primary"
           class="px-4 py-2 border-b-2 border-transparent font-label-md text-label-md text-on-surface-variant hover:text-on-surface transition-colors no-underline">
          Offres
        </a>
        <a routerLink="/responsable-admission/recrutement-formateurs/candidatures"
           routerLinkActive="border-secondary text-primary"
           class="px-4 py-2 border-b-2 border-transparent font-label-md text-label-md text-on-surface-variant hover:text-on-surface transition-colors no-underline">
          Candidatures
        </a>
      </nav>

      <div class="flex flex-wrap gap-3">
        <select [(ngModel)]="filtreStatut" (ngModelChange)="charger()"
          class="p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors">
          <option [ngValue]="undefined">Tous les statuts</option>
          <option *ngFor="let s of statuts" [ngValue]="s">{{ libellesStatut[s] }}</option>
        </select>
      </div>

      <app-ui-spinner *ngIf="chargement" message="Chargement des candidatures…"></app-ui-spinner>

      <app-ui-empty-state
        *ngIf="!chargement && candidatures.length === 0"
        icone="folder_shared"
        titre="Aucune candidature"
        description="Aucune candidature ne correspond à ce filtre."
      ></app-ui-empty-state>

      <div class="grid lg:grid-cols-2 gap-base" *ngIf="!chargement && candidatures.length > 0">
        <!-- Liste -->
        <ul class="space-y-base">
          <li *ngFor="let c of candidatures" (click)="selectionner(c)"
              class="cursor-pointer bg-surface-container-lowest rounded-xl border p-stack-lg shadow-sm transition-colors"
              [ngClass]="selection?.id === c.id ? 'border-secondary' : 'border-outline-variant hover:border-secondary'">
            <div class="flex items-center justify-between gap-2">
              <h2 class="font-label-md text-label-md text-on-surface truncate">{{ c.prenom }} {{ c.nom }}</h2>
              <span class="px-2 py-0.5 rounded-full font-label-sm text-label-sm shrink-0"
                [ngClass]="badge(c)">{{ etiquette(c) }}</span>
            </div>
            <p class="font-body-sm text-body-sm text-on-surface-variant mt-1 truncate">
              {{ c.intituleOffre }} · déposée le {{ c.dateSoumission | date: 'd MMM y' }}
            </p>
          </li>
        </ul>

        <!-- Panneau -->
        <div *ngIf="selection" class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg space-y-4 h-fit">
          <div class="flex items-center justify-between gap-2 flex-wrap">
            <h2 class="font-headline-sm text-headline-sm text-primary">{{ selection.prenom }} {{ selection.nom }}</h2>
            <span class="px-2 py-0.5 rounded-full font-label-sm text-label-sm" [ngClass]="badge(selection)">{{ etiquette(selection) }}</span>
          </div>

          <dl class="grid grid-cols-1 sm:grid-cols-2 gap-2 text-body-sm font-body-sm bg-surface-container-low rounded-lg p-3">
            <div><dt class="text-on-surface-variant uppercase tracking-wider text-label-sm font-label-sm">Offre</dt>
              <dd class="text-on-surface">{{ selection.intituleOffre || '—' }}</dd></div>
            <div><dt class="text-on-surface-variant uppercase tracking-wider text-label-sm font-label-sm">Téléphone</dt>
              <dd class="text-on-surface">{{ selection.telephone }}</dd></div>
            <div><dt class="text-on-surface-variant uppercase tracking-wider text-label-sm font-label-sm">E-mail</dt>
              <dd class="text-on-surface break-all">{{ selection.email }}</dd></div>
            <div><dt class="text-on-surface-variant uppercase tracking-wider text-label-sm font-label-sm">Diplôme</dt>
              <dd class="text-on-surface">{{ selection.diplomePrincipal || '—' }}</dd></div>
            <div><dt class="text-on-surface-variant uppercase tracking-wider text-label-sm font-label-sm">Expérience</dt>
              <dd class="text-on-surface">{{ selection.anneesExperience != null ? selection.anneesExperience + ' an(s)' : '—' }}</dd></div>
          </dl>

          <div *ngIf="selection.lettreMotivation">
            <p class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider">Lettre de motivation</p>
            <p class="font-body-sm text-body-sm text-on-surface whitespace-pre-line mt-1">{{ selection.lettreMotivation }}</p>
          </div>

          <!-- État civil et adresse -->
          <div *ngIf="civilRenseigne(selection)">
            <p class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider mb-1">État civil et adresse</p>
            <dl class="grid grid-cols-1 sm:grid-cols-2 gap-2 text-body-sm font-body-sm bg-surface-container-low rounded-lg p-3">
              <div *ngIf="selection.civilite"><dt class="text-on-surface-variant text-label-sm font-label-sm">Civilité</dt><dd class="text-on-surface">{{ selection.civilite }}</dd></div>
              <div *ngIf="selection.genre"><dt class="text-on-surface-variant text-label-sm font-label-sm">Genre</dt><dd class="text-on-surface">{{ selection.genre }}</dd></div>
              <div *ngIf="selection.dateNaissance"><dt class="text-on-surface-variant text-label-sm font-label-sm">Naissance</dt><dd class="text-on-surface">{{ selection.dateNaissance | date: 'd MMM y' }}<span *ngIf="selection.lieuNaissance"> — {{ selection.lieuNaissance }}</span></dd></div>
              <div *ngIf="selection.nationalite"><dt class="text-on-surface-variant text-label-sm font-label-sm">Nationalité</dt><dd class="text-on-surface">{{ selection.nationalite }}<span *ngIf="selection.nationaliteSecondaire"> / {{ selection.nationaliteSecondaire }}</span></dd></div>
              <div *ngIf="selection.niveauEtude"><dt class="text-on-surface-variant text-label-sm font-label-sm">Niveau d'étude</dt><dd class="text-on-surface">{{ selection.niveauEtude }}</dd></div>
              <div *ngIf="selection.telephoneSecondaire"><dt class="text-on-surface-variant text-label-sm font-label-sm">Tél. secondaire</dt><dd class="text-on-surface">{{ selection.telephoneSecondaire }}</dd></div>
              <div *ngIf="selection.emailAlternatif"><dt class="text-on-surface-variant text-label-sm font-label-sm">E-mail alternatif</dt><dd class="text-on-surface break-all">{{ selection.emailAlternatif }}</dd></div>
              <div *ngIf="selection.adresseLigne1 || selection.ville" class="sm:col-span-2"><dt class="text-on-surface-variant text-label-sm font-label-sm">Adresse</dt>
                <dd class="text-on-surface">{{ selection.adresseLigne1 }}<span *ngIf="selection.adresseLigne2"> {{ selection.adresseLigne2 }}</span><span *ngIf="selection.ville">, {{ selection.ville }}</span><span *ngIf="selection.paysResidence"> ({{ selection.paysResidence }})</span></dd>
              </div>
            </dl>
          </div>

          <!-- Domaines d'expertise -->
          <div *ngIf="selection.domainesExpertise.length > 0">
            <p class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider mb-1">Domaines d'expertise</p>
            <div class="flex flex-wrap gap-1.5">
              <span *ngFor="let d of selection.domainesExpertise" class="px-2.5 py-1 rounded-full bg-secondary-container text-on-secondary-container font-label-sm text-label-sm">{{ d }}</span>
            </div>
          </div>

          <!-- Formations -->
          <div *ngIf="selection.formations.length > 0">
            <p class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider mb-1">Formation</p>
            <ul class="space-y-1.5">
              <li *ngFor="let f of selection.formations" class="p-2.5 rounded-lg bg-surface-container-low font-body-sm text-body-sm text-on-surface">
                <span class="font-label-sm text-label-sm">{{ f.intituleDiplome || f.niveauEtude || 'Formation' }}</span>
                <span *ngIf="f.etablissement" class="text-on-surface-variant"> — {{ f.etablissement }}</span>
                <span *ngIf="f.dateObtention" class="text-on-surface-variant"> ({{ f.dateObtention | date: 'y' }})</span>
              </li>
            </ul>
          </div>

          <!-- Expériences -->
          <div *ngIf="selection.experiences.length > 0">
            <p class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider mb-1">Expérience professionnelle</p>
            <ul class="space-y-1.5">
              <li *ngFor="let e of selection.experiences" class="p-2.5 rounded-lg bg-surface-container-low font-body-sm text-body-sm text-on-surface space-y-0.5">
                <p><span class="font-label-sm text-label-sm">{{ e.intitulePoste || 'Poste' }}</span><span *ngIf="e.employeur" class="text-on-surface-variant"> — {{ e.employeur }}</span><span *ngIf="e.posteActuel" class="text-secondary"> (poste actuel)</span></p>
                <p *ngIf="e.nomSuperviseur" class="text-on-surface-variant">Référence : {{ e.nomSuperviseur }}<span *ngIf="e.titreSuperviseur"> ({{ e.titreSuperviseur }})</span><span *ngIf="e.telephoneSuperviseur"> — {{ e.telephoneSuperviseur }}</span><span *ngIf="e.contactAutorise === false" class="text-error"> — contact non autorisé</span></p>
              </li>
            </ul>
          </div>

          <!-- Langues -->
          <div *ngIf="selection.langues.length > 0">
            <p class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider mb-1">Compétences linguistiques</p>
            <div class="flex flex-wrap gap-1.5">
              <span *ngFor="let l of selection.langues" class="px-2.5 py-1 rounded-full bg-surface-container-low border border-outline-variant font-label-sm text-label-sm text-on-surface">
                {{ l.langue }} — {{ libellesNiveauLangue[l.niveau] }}
              </span>
            </div>
          </div>

          <!-- Certifications -->
          <div *ngIf="selection.certifications.length > 0">
            <p class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider mb-1">Certifications</p>
            <ul class="space-y-1.5">
              <li *ngFor="let c of selection.certifications" class="p-2.5 rounded-lg bg-surface-container-low font-body-sm text-body-sm text-on-surface">
                <span class="font-label-sm text-label-sm">{{ c.intitule }}</span>
                <span *ngIf="c.organisme" class="text-on-surface-variant"> — {{ c.organisme }}</span>
                <span *ngIf="c.dateExpiration" class="text-on-surface-variant"> (expire {{ c.dateExpiration | date: 'MMM y' }})</span>
              </li>
            </ul>
          </div>

          <!-- Pièces -->
          <div>
            <h3 class="font-label-md text-label-md text-on-surface mb-2">Pièces du dossier</h3>
            <ul *ngIf="selection.pieces.length > 0" class="divide-y divide-outline-variant/50 border border-outline-variant rounded-lg overflow-hidden">
              <li *ngFor="let p of selection.pieces" class="flex items-center gap-2 p-2.5 font-body-sm text-body-sm">
                <span class="material-symbols-outlined text-[18px] text-on-surface-variant shrink-0">draft</span>
                <span class="flex-1 min-w-0 truncate text-on-surface">
                  {{ libellesType[p.typeDocument] }}<span *ngIf="p.nomFichierOriginal" class="text-on-surface-variant"> — {{ p.nomFichierOriginal }}</span>
                </span>
                <button type="button" (click)="consulter(p.id)" title="Consulter" class="shrink-0 text-outline hover:text-secondary">
                  <span class="material-symbols-outlined text-[18px]">visibility</span>
                </button>
              </li>
            </ul>
            <p *ngIf="selection.pieces.length === 0" class="font-body-sm text-body-sm text-on-surface-variant">Aucune pièce jointe.</p>
          </div>

          <!-- Suivi -->
          <div *ngIf="selection.statut === 'DOSSIER_RECU'" class="pt-3 border-t border-outline-variant">
            <button type="button" (click)="prendreEnCharge()" [disabled]="action"
              class="px-4 py-2 rounded-lg bg-secondary text-on-secondary font-label-md text-label-md hover:opacity-90 transition-opacity disabled:opacity-50">
              Prendre en charge
            </button>
          </div>

          <!-- Décision -->
          <div *ngIf="selection.statut !== 'TERMINE'" class="pt-3 border-t border-outline-variant space-y-3">
            <h3 class="font-label-md text-label-md text-on-surface">Décision</h3>
            <div *ngIf="!decisionCible" class="flex gap-3">
              <button type="button" (click)="ouvrir('RETENU')"
                class="px-4 py-2 rounded-lg bg-secondary text-on-secondary font-label-md text-label-md hover:opacity-90 transition-opacity">
                Retenir
              </button>
              <button type="button" (click)="ouvrir('NON_RETENU')"
                class="px-4 py-2 rounded-lg bg-error text-on-error font-label-md text-label-md hover:opacity-90 transition-opacity">
                Ne pas retenir
              </button>
            </div>
            <div *ngIf="decisionCible" class="p-3 rounded-lg bg-surface-container-low space-y-2">
              <p class="font-label-sm text-label-sm text-on-surface">
                {{ decisionCible === 'RETENU' ? 'Retenir ce candidat' : 'Ne pas retenir ce candidat' }}
                <span *ngIf="decisionCible === 'RETENU'" class="text-on-surface-variant">— un compte formateur sera créé et le candidat notifié.</span>
              </p>
              <textarea [(ngModel)]="motif" name="motif" rows="3"
                [placeholder]="decisionCible === 'NON_RETENU' ? 'Motif du refus (obligatoire)' : 'Note (facultative)'"
                class="w-full p-3 rounded-lg bg-surface-container-lowest border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors resize-y"></textarea>
              <p *ngIf="erreur" class="font-body-sm text-body-sm text-error">{{ erreur }}</p>
              <div class="flex gap-3">
                <button type="button" (click)="confirmer()" [disabled]="action"
                  class="px-4 py-2 rounded-lg font-label-md text-label-md text-on-secondary hover:opacity-90 transition-opacity disabled:opacity-50"
                  [ngClass]="decisionCible === 'RETENU' ? 'bg-secondary' : 'bg-error text-on-error'">
                  {{ action ? 'Envoi…' : 'Confirmer' }}
                </button>
                <button type="button" (click)="decisionCible = null"
                  class="px-4 py-2 rounded-lg border border-outline-variant text-on-surface-variant font-label-md text-label-md hover:bg-surface-container transition-colors">
                  Annuler
                </button>
              </div>
            </div>
          </div>

          <!-- Récap post-décision -->
          <div *ngIf="selection.statut === 'TERMINE'" class="pt-3 border-t border-outline-variant space-y-2">
            <p class="font-label-md text-label-md" [ngClass]="selection.decision === 'RETENU' ? 'text-success' : 'text-error'">
              {{ selection.decision ? libellesDecision[selection.decision] : 'Terminé' }}
              <span class="font-body-sm text-body-sm text-on-surface-variant">le {{ selection.dateDecision | date: 'd MMM y' }}</span>
            </p>
            <p *ngIf="selection.motifDecision" class="p-3 rounded-lg bg-surface-container-low font-body-sm text-body-sm text-on-surface">
              {{ selection.motifDecision }}
            </p>
            <p *ngIf="selection.decision === 'RETENU'" class="font-body-sm text-body-sm text-on-surface-variant">
              {{ selection.formateurCree ? 'Le compte formateur a été créé.' : 'Compte formateur non créé.' }}
            </p>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class CandidaturesAdminFormateursComponent implements OnInit {
  private service = inject(AdminRecrutementFormateurService);
  private mediaProtege = inject(MediaProtegeService);

  candidatures: CandidatureFormateur[] = [];
  selection: CandidatureFormateur | null = null;
  chargement = true;
  action = false;
  erreur = '';
  filtreStatut: StatutCandidatureFormateur | undefined;

  libellesStatut = LIBELLES_STATUT_CANDIDATURE;
  libellesDecision = LIBELLES_DECISION;
  libellesType = LIBELLES_TYPE_DOCUMENT;
  libellesNiveauLangue = LIBELLES_NIVEAU_LANGUE;
  statuts: StatutCandidatureFormateur[] = ['DOSSIER_RECU', 'EN_COURS_DE_TRAITEMENT', 'TERMINE'];

  decisionCible: 'RETENU' | 'NON_RETENU' | null = null;
  motif = '';

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.chargement = true;
    this.service.getCandidatures(this.filtreStatut ? { statut: this.filtreStatut } : undefined).subscribe({
      next: (liste) => {
        this.candidatures = liste;
        this.chargement = false;
      },
      error: () => (this.chargement = false),
    });
  }

  selectionner(c: CandidatureFormateur): void {
    this.decisionCible = null;
    this.motif = '';
    this.erreur = '';
    this.service.getCandidature(c.id).subscribe({
      next: (d) => (this.selection = d),
      error: () => (this.selection = c),
    });
  }

  private remplacer(c: CandidatureFormateur): void {
    this.candidatures = this.candidatures.map((x) => (x.id === c.id ? c : x));
    this.selection = c;
  }

  badge(c: CandidatureFormateur): string {
    if (c.statut === 'DOSSIER_RECU') return 'bg-surface-container text-on-surface-variant';
    if (c.statut === 'EN_COURS_DE_TRAITEMENT') return 'bg-secondary-container text-on-secondary-container';
    if (c.decision === 'RETENU') return 'bg-success/15 text-success';
    if (c.decision === 'NON_RETENU') return 'bg-error-container text-on-error-container';
    return 'bg-surface-container-high text-on-surface';
  }

  etiquette(c: CandidatureFormateur): string {
    if (c.statut === 'TERMINE' && c.decision) return this.libellesDecision[c.decision];
    return this.libellesStatut[c.statut];
  }

  civilRenseigne(c: CandidatureFormateur): boolean {
    return !!(c.civilite || c.genre || c.dateNaissance || c.nationalite || c.niveauEtude
      || c.telephoneSecondaire || c.emailAlternatif || c.adresseLigne1 || c.ville);
  }

  consulter(pieceId: number): void {
    if (!this.selection) return;
    this.mediaProtege.ouvrir(this.service.urlFichierPiece(this.selection.id, pieceId));
  }

  prendreEnCharge(): void {
    if (!this.selection) return;
    this.action = true;
    this.erreur = '';
    this.service.changerStatut(this.selection.id, 'EN_COURS_DE_TRAITEMENT').subscribe({
      next: (d) => {
        this.action = false;
        this.remplacer(d);
      },
      error: (e) => {
        this.action = false;
        this.erreur = e?.error?.message || 'La prise en charge a échoué.';
      },
    });
  }

  ouvrir(cible: 'RETENU' | 'NON_RETENU'): void {
    this.decisionCible = cible;
    this.motif = '';
    this.erreur = '';
  }

  confirmer(): void {
    if (!this.selection || !this.decisionCible) return;
    const motif = this.motif.trim();
    if (this.decisionCible === 'NON_RETENU' && !motif) {
      this.erreur = 'Le motif du refus est obligatoire.';
      return;
    }
    this.action = true;
    this.erreur = '';
    this.service.decider(this.selection.id, this.decisionCible, motif || null).subscribe({
      next: (d) => {
        this.action = false;
        this.decisionCible = null;
        this.remplacer(d);
      },
      error: (e) => {
        this.action = false;
        this.erreur = e?.error?.message || "La décision n'a pas pu être enregistrée.";
      },
    });
  }
}
