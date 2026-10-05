import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import {
  AppreciationCritere,
  AvisValeur,
  CritereAccreditation,
  DemandeAccreditationEvaluateur,
  LIBELLES_APPRECIATION,
  LIBELLES_AVIS,
  LIBELLES_NIVEAU_PROGRAMME,
  LIBELLES_STATUT_DEMANDE,
  LIBELLES_TYPE_DOCUMENT,
  LIBELLES_TYPE_STRUCTURE,
  STATUTS_FAVORABLES,
  StatutDemandeAccreditation,
  TYPES_DOCUMENT_RECOMMANDES,
  TYPES_DOCUMENT_REQUIS,
  TypeDocumentAccreditation,
} from '../../../accreditation/models/accreditation.models';
import { UiEmptyStateComponent } from '../../../../shared/components/ui/ui-empty-state.component';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import { MediaProtegeService } from '../../../espace-etudiants/services/media-protege.service';
import { Auth } from '../../../../core/auth/services/authService';
import { EvaluateurAccreditationService } from '../../services/evaluateur-accreditation.service';

type CibleDecision = 'ACCREDITE' | 'ACCREDITE_SOUS_RESERVE' | 'REJETE';

interface SectionGrille {
  nom: string;
  criteres: CritereAccreditation[];
  jugees: number;
  total: number;
}

/**
 * Instruction d'une demande d'accréditation de programme (Cadre de référence
 * DRCQ) : coordonnées de l'école, programme visé, pièces attendues, grille du
 * référentiel appliquée section par section, avis collégial, puis décision —
 * accréditation pleine, accréditation conditionnelle (plan d'actions) ou refus,
 * avec une **seule** synthèse communiquée à l'école.
 */
@Component({
  selector: 'app-evaluateur-demandes',
  standalone: true,
  imports: [CommonModule, FormsModule, UiSpinnerComponent, UiEmptyStateComponent],
  templateUrl: './demandes.html',
})
export class EvaluateurDemandesComponent implements OnInit {
  private evaluateurService = inject(EvaluateurAccreditationService);
  private mediaProtege = inject(MediaProtegeService);
  private auth = inject(Auth);
  private route = inject(ActivatedRoute);

  demandes: DemandeAccreditationEvaluateur[] = [];
  toutesDemandes: DemandeAccreditationEvaluateur[] = [];
  selection: DemandeAccreditationEvaluateur | null = null;
  chargement = true;
  chargementDetail = false;
  filtreStatut: StatutDemandeAccreditation | undefined;

  libellesStatut = LIBELLES_STATUT_DEMANDE;
  libellesType = LIBELLES_TYPE_DOCUMENT;
  libellesAvis = LIBELLES_AVIS;
  libellesAppreciation = LIBELLES_APPRECIATION;
  libellesNiveau = LIBELLES_NIVEAU_PROGRAMME;
  libellesTypeStructure = LIBELLES_TYPE_STRUCTURE;
  statuts: StatutDemandeAccreditation[] = [
    'SOUMISE',
    'EN_COURS_EVALUATION',
    'ACCREDITE',
    'ACCREDITE_SOUS_RESERVE',
    'REJETE',
  ];
  readonly typesRequis = TYPES_DOCUMENT_REQUIS;
  readonly typesRecommandes = TYPES_DOCUMENT_RECOMMANDES;
  readonly appreciations: AppreciationCritere[] = ['CONFORME', 'NON_CONFORME', 'SANS_OBJET'];
  readonly favorables = STATUTS_FAVORABLES;

  // Avis individuel (trace collégiale interne)
  avisSaisi: AvisValeur | null = null;
  avisCommentaire = '';
  avisEnCours = false;
  erreurAvis = '';

  // Commentaires en cours d'édition dans la grille de critères (clé = critereId)
  grilleCommentaires: Record<number, string> = {};

  // Décision : bloc de confirmation avec une seule synthèse
  decisionCible: CibleDecision | null = null;
  syntheseDecision = '';
  planActionsDecision = '';
  decisionEnCours = false;
  erreurDecision = '';

  ngOnInit(): void {
    this.charger();
    // ?id=… : lien direct depuis le tableau de bord vers un dossier précis, plutôt
    // que de forcer un second clic dans la liste une fois sur cet écran.
    const idCible = Number(this.route.snapshot.queryParamMap.get('id')) || null;
    this.evaluateurService.getDemandes(undefined).subscribe({
      next: (demandes) => {
        this.toutesDemandes = demandes;
        const cible = idCible ? demandes.find((d) => d.id === idCible) : null;
        if (cible) {
          this.selectionner(cible);
        }
      },
      error: () => (this.toutesDemandes = []),
    });
  }

  charger(): void {
    this.chargement = true;
    this.evaluateurService.getDemandes(this.filtreStatut).subscribe({
      next: (demandes) => {
        this.demandes = demandes;
        this.chargement = false;
      },
      error: () => (this.chargement = false),
    });
  }

  compte(statut: StatutDemandeAccreditation): number {
    return this.toutesDemandes.filter((d) => d.statut === statut).length;
  }

  get compteAccreditees(): number {
    return this.toutesDemandes.filter((d) => this.favorables.includes(d.statut)).length;
  }

  private synchroniserCompteurs(demande: DemandeAccreditationEvaluateur): void {
    this.toutesDemandes = this.toutesDemandes.map((d) => (d.id === demande.id ? demande : d));
  }

  // ===================== Sélection =====================

  selectionner(demande: DemandeAccreditationEvaluateur): void {
    this.chargementDetail = true;
    this.reinitialiserFormulaires();
    // On recharge le détail : avis et grille d'un autre évaluateur peuvent avoir bougé.
    this.evaluateurService.getDemande(demande.id).subscribe({
      next: (d) => {
        this.selection = d;
        this.chargementDetail = false;
        this.hydraterFormulaireAvis();
        this.hydraterGrille();
      },
      error: () => {
        this.selection = demande;
        this.chargementDetail = false;
      },
    });
  }

  private reinitialiserFormulaires(): void {
    this.decisionCible = null;
    this.syntheseDecision = '';
    this.planActionsDecision = '';
    this.erreurDecision = '';
    this.erreurAvis = '';
    this.grilleCommentaires = {};
  }

  private remplacer(demande: DemandeAccreditationEvaluateur): void {
    this.demandes = this.demandes.map((d) => (d.id === demande.id ? demande : d));
    this.synchroniserCompteurs(demande);
    this.selection = demande;
    this.hydraterFormulaireAvis();
    this.hydraterGrille();
  }

  // ===================== Pièces =====================

  consulterPiece(pieceId: number): void {
    if (!this.selection) {
      return;
    }
    this.mediaProtege.ouvrir(this.evaluateurService.urlFichierPiece(this.selection.id, pieceId));
  }

  pieceDeType(type: TypeDocumentAccreditation) {
    return this.selection?.pieces.find((p) => p.typeDocument === type);
  }

  get piecesAutres() {
    return this.selection?.pieces.filter((p) => p.typeDocument === 'AUTRE') ?? [];
  }

  get typesRequisManquants(): TypeDocumentAccreditation[] {
    return this.typesRequis.filter((t) => !this.pieceDeType(t));
  }

  // ===================== Avis individuel =====================

  get monAvis() {
    const monId = this.auth.currentUser()?.id;
    return this.selection?.avis.find((a) => a.evaluateurId === monId) ?? null;
  }

  estMonAvis(avis: { evaluateurId: number }): boolean {
    return avis.evaluateurId === this.auth.currentUser()?.id;
  }

  private hydraterFormulaireAvis(): void {
    const mien = this.monAvis;
    this.avisSaisi = mien?.avis ?? null;
    this.avisCommentaire = mien?.commentaire ?? '';
    this.erreurAvis = '';
  }

  deposerAvis(): void {
    if (!this.selection || !this.avisSaisi || this.avisEnCours) {
      return;
    }
    this.avisEnCours = true;
    this.erreurAvis = '';
    this.evaluateurService
      .deposerAvis(this.selection.id, this.avisSaisi, this.avisCommentaire.trim() || null)
      .subscribe({
        next: (d) => {
          this.avisEnCours = false;
          this.remplacer(d);
        },
        error: (e) => {
          this.avisEnCours = false;
          this.erreurAvis = e?.error?.message || "L'avis n'a pas pu être enregistré.";
        },
      });
  }

  // ===================== Grille de critères =====================

  private hydraterGrille(): void {
    this.grilleCommentaires = {};
    for (const e of this.selection?.maGrille ?? []) {
      this.grilleCommentaires[e.critereId] = e.commentaire ?? '';
    }
  }

  appreciationDe(critereId: number): AppreciationCritere | null {
    return this.selection?.maGrille.find((e) => e.critereId === critereId)?.appreciation ?? null;
  }

  /** Critères actifs regroupés par section du référentiel, avec l'avancement de la grille. */
  get sectionsGrille(): SectionGrille[] {
    const jugees = new Set((this.selection?.maGrille ?? []).map((e) => e.critereId));
    const groupes = new Map<string, CritereAccreditation[]>();
    for (const c of this.selection?.criteresActifs ?? []) {
      const nom = c.section?.trim() || 'Autres critères';
      const liste = groupes.get(nom);
      if (liste) {
        liste.push(c);
      } else {
        groupes.set(nom, [c]);
      }
    }
    return [...groupes.entries()].map(([nom, criteres]) => ({
      nom,
      criteres,
      total: criteres.length,
      jugees: criteres.filter((c) => jugees.has(c.id)).length,
    }));
  }

  get criteresNonEvalues(): number {
    const jugees = new Set((this.selection?.maGrille ?? []).map((e) => e.critereId));
    return (this.selection?.criteresActifs ?? []).filter((c) => !jugees.has(c.id)).length;
  }

  get avancementGlobalPourcent(): number {
    const total = this.selection?.criteresActifs.length ?? 0;
    if (total === 0) {
      return 0;
    }
    return Math.round(((total - this.criteresNonEvalues) / total) * 100);
  }

  /** Non-conformités relevées, préfixées du code du référentiel (« 1.3 : … »). */
  private get nonConformites(): string[] {
    const codeParId = new Map((this.selection?.criteresActifs ?? []).map((c) => [c.id, c.code]));
    return (this.selection?.maGrille ?? [])
      .filter((e) => e.appreciation === 'NON_CONFORME')
      .map((e) => {
        const code = codeParId.get(e.critereId);
        return (code ? code + ' : ' : '') + e.critereLibelle;
      });
  }

  evaluerCritere(critereId: number, appreciation: AppreciationCritere): void {
    if (!this.selection) {
      return;
    }
    const commentaire = (this.grilleCommentaires[critereId] ?? '').trim() || null;
    this.evaluateurService.evaluerCritere(this.selection.id, critereId, appreciation, commentaire).subscribe({
      next: (d) => this.remplacer(d),
      error: (e) => (this.erreurDecision = e?.error?.message || "L'évaluation du critère a échoué."),
    });
  }

  /** À la sortie du champ observation : ne renvoie que si le critère a déjà une appréciation. */
  sauverObservation(critereId: number): void {
    const appreciation = this.appreciationDe(critereId);
    if (appreciation) {
      this.evaluerCritere(critereId, appreciation);
    }
  }

  // ===================== Décision =====================

  prendreEnCharge(): void {
    if (!this.selection) {
      return;
    }
    this.evaluateurService.prendreEnCharge(this.selection.id).subscribe({
      next: (d) => this.remplacer(d),
      error: (e) => (this.erreurDecision = e?.error?.message || 'La prise en charge a échoué.'),
    });
  }

  ouvrirDecision(cible: CibleDecision): void {
    this.decisionCible = cible;
    this.erreurDecision = '';
    // Pré-remplissage : le commentaire de mon avis, sinon la liste des non-conformités relevées (par code).
    const base = this.monAvis?.commentaire?.trim();
    this.syntheseDecision =
      base ||
      (this.nonConformites.length
        ? 'Non-conformités relevées — ' + this.nonConformites.join(' ; ') + '.'
        : '');
    // Pour une accréditation sous réserve, le plan d'actions part des non-conformités.
    this.planActionsDecision =
      cible === 'ACCREDITE_SOUS_RESERVE' && this.nonConformites.length
        ? this.nonConformites.map((n) => '- ' + n).join('\n')
        : '';
  }

  annulerDecision(): void {
    this.decisionCible = null;
    this.syntheseDecision = '';
    this.planActionsDecision = '';
    this.erreurDecision = '';
  }

  confirmerDecision(): void {
    if (!this.selection || !this.decisionCible || this.decisionEnCours) {
      return;
    }
    const synthese = this.syntheseDecision.trim();
    const plan = this.planActionsDecision.trim();
    if (this.decisionCible === 'REJETE' && !synthese) {
      this.erreurDecision = 'Le motif du rejet est obligatoire.';
      return;
    }
    if (this.decisionCible === 'ACCREDITE_SOUS_RESERVE' && !plan) {
      this.erreurDecision = "Le plan d'actions est obligatoire pour une accréditation sous réserve.";
      return;
    }
    this.decisionEnCours = true;
    this.erreurDecision = '';
    let appel;
    if (this.decisionCible === 'ACCREDITE') {
      appel = this.evaluateurService.accrediter(this.selection.id, synthese || undefined);
    } else if (this.decisionCible === 'ACCREDITE_SOUS_RESERVE') {
      appel = this.evaluateurService.accrediterSousReserve(this.selection.id, plan, synthese || undefined);
    } else {
      appel = this.evaluateurService.rejeter(this.selection.id, synthese);
    }
    appel.subscribe({
      next: (d) => {
        this.decisionEnCours = false;
        this.decisionCible = null;
        this.remplacer(d);
      },
      error: (e) => {
        this.decisionEnCours = false;
        this.erreurDecision = e?.error?.message || "La décision n'a pas pu être enregistrée.";
      },
    });
  }
}
