import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import {
  EtudiantScolarite,
  PedagogieService,
  PromotionDetail,
  ReleveCourant,
  ReleveDelivre,
} from '../../services/pedagogie.service';

/**
 * Délivrance des relevés de notes.
 *
 * <p>L'étudiant voyait déjà ses notes en continu, mais rien ne constituait un
 * document : ni référence, ni date, ni trace de ce qui avait été remis. Le
 * responsable pédagogique, à qui revient la délivrance, ne pouvait même pas consulter
 * le relevé d'un étudiant — il aurait fallu délivrer sans pouvoir vérifier.
 *
 * <p>Deux notions cohabitent ici, et l'écran doit les tenir distinctes : le relevé
 * <strong>courant</strong>, recalculé, et le relevé <strong>délivré</strong>, figé.
 * Les confondre ferait croire qu'un document remis change tout seul.
 */
@Component({
  selector: 'app-pedagogie-releves',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './pedagogie-releves.component.html',
})
export class PedagogieRelevesComponent implements OnInit {
  private pedagogie = inject(PedagogieService);

  chargement = true;
  releves: ReleveDelivre[] = [];
  etudiants: EtudiantScolarite[] = [];
  promotions: PromotionDetail[] = [];

  messageSucces = '';
  messageErreur = '';

  // --- Consultation du relevé courant, avant délivrance ---
  etudiantConsulte: EtudiantScolarite | null = null;
  releveCourant: ReleveCourant | null = null;
  consultation = false;

  // --- Délivrance ---
  formulaireOuvert = false;
  periode = '';
  promotionCiblee: number | null = null;
  selection = new Set<number>();
  delivrance = false;

  ngOnInit(): void {
    this.charger();
  }

  private charger(): void {
    this.chargement = true;
    forkJoin({
      releves: this.pedagogie.getReleves(),
      etudiants: this.pedagogie.getEtudiants(),
      promotions: this.pedagogie.getPromotions(),
    }).subscribe({
      next: ({ releves, etudiants, promotions }) => {
        this.releves = releves || [];
        this.etudiants = etudiants || [];
        this.promotions = promotions || [];
        this.chargement = false;
      },
      error: () => {
        this.messageErreur = 'Les relevés n’ont pas pu être chargés.';
        this.chargement = false;
      },
    });
  }

  private reinitialiserMessages(): void {
    this.messageSucces = '';
    this.messageErreur = '';
  }

  // ===================== Consultation =====================

  consulter(etudiant: EtudiantScolarite): void {
    this.reinitialiserMessages();
    this.etudiantConsulte = etudiant;
    this.releveCourant = null;
    this.consultation = true;
    this.pedagogie.getReleveCourant(etudiant.id).subscribe({
      next: (releve) => {
        this.releveCourant = releve;
        this.consultation = false;
      },
      error: () => {
        this.consultation = false;
        this.messageErreur = 'Le relevé de cet étudiant n’a pas pu être chargé.';
      },
    });
  }

  fermerConsultation(): void {
    this.etudiantConsulte = null;
    this.releveCourant = null;
  }

  // ===================== Délivrance =====================

  ouvrirFormulaire(): void {
    this.reinitialiserMessages();
    this.periode = this.periodeSuggeree();
    this.promotionCiblee = null;
    this.selection = new Set();
    this.formulaireOuvert = true;
  }

  /**
   * « Semestre 1 » de septembre à janvier, « Semestre 2 » ensuite.
   *
   * <p>Une simple suggestion, modifiable : la saisir à la main chaque fois inviterait
   * aux fautes de frappe, et deux relevés portant « S1 2025/26 » et « Semestre 1 —
   * 2025/2026 » ne se regrouperaient plus.
   */
  private periodeSuggeree(): string {
    const aujourdhui = new Date();
    const mois = aujourdhui.getMonth() + 1;
    const debut = mois >= 9 ? aujourdhui.getFullYear() : aujourdhui.getFullYear() - 1;
    const semestre = mois >= 9 || mois === 1 ? 1 : 2;
    return `Semestre ${semestre} — ${debut}/${debut + 1}`;
  }

  fermerFormulaire(): void {
    this.formulaireOuvert = false;
  }

  estSelectionne(id: number): boolean {
    return this.selection.has(id);
  }

  basculer(id: number): void {
    if (this.selection.has(id)) {
      this.selection.delete(id);
    } else {
      this.selection.add(id);
    }
  }

  /** Étudiants proposés : ceux de la promotion visée, ou tous si aucune ne l'est. */
  get etudiantsProposes(): EtudiantScolarite[] {
    if (this.promotionCiblee === null) {
      return this.etudiants;
    }
    const libelle = this.promotions.find((p) => p.id === this.promotionCiblee)?.libelle;
    return this.etudiants.filter((e) => e.cohorte === libelle);
  }

  get saisieValide(): boolean {
    return !!this.periode.trim() && (this.selection.size > 0 || this.promotionCiblee !== null);
  }

  /**
   * Étudiants visés qui ont déjà un relevé pour la période saisie.
   *
   * <p>Calculé sur le registre déjà chargé, sans appel supplémentaire. Réémettre est
   * légitime — duplicata, relevé corrigé — mais un double clic produit exactement le
   * même résultat qu'une réémission voulue. D'où l'avertissement, jamais un blocage :
   * on ne supprime pas un document délivré, autant le faire sciemment.
   */
  get dejaDelivres(): string[] {
    const periode = this.periode.trim().toLowerCase();
    if (!periode) {
      return [];
    }
    const vises =
      this.selection.size > 0
        ? this.etudiantsProposes.filter((e) => this.selection.has(e.id))
        : this.etudiantsProposes;
    return vises
      .filter((e) =>
        this.releves.some((r) => r.etudiantId === e.id && r.periode.trim().toLowerCase() === periode)
      )
      .map((e) => e.nom);
  }

  delivrer(): void {
    if (!this.saisieValide || this.delivrance) {
      return;
    }
    this.delivrance = true;
    this.reinitialiserMessages();

    this.pedagogie
      .delivrerReleves(this.periode.trim(), [...this.selection], this.promotionCiblee)
      .subscribe({
        next: (resultat) => {
          this.delivrance = false;
          this.fermerFormulaire();
          const nb = resultat.delivres.length;
          const reemis = resultat.reemissions?.length ?? 0;
          this.messageSucces =
            `${nb} relevé${nb > 1 ? 's' : ''} délivré${nb > 1 ? 's' : ''}` +
            (reemis > 0 ? `, dont ${reemis} en réémission.` : '.');
          if (resultat.sansNote.length > 0) {
            // Signalé, pas caché : un relevé vide n'attesterait de rien, mais le
            // responsable doit savoir qui n'en a pas reçu, et pourquoi.
            this.messageErreur =
              `Sans note, donc sans relevé : ${resultat.sansNote.join(', ')}.`;
          }
          this.charger();
        },
        error: (erreur) => {
          this.delivrance = false;
          this.messageErreur = erreur?.error?.message || 'La délivrance a échoué.';
        },
      });
  }
}
