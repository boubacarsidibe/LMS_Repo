import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subscription, forkJoin } from 'rxjs';
import { OptionCombobox, UiComboboxComponent } from '../../../../shared/components/ui/ui-combobox.component';
import { UiBadgeComponent } from '../../../../shared/components/ui/ui-badge.component';
import {
  ApercuCertificats,
  Certificat,
  EtudiantApercuCertificat,
  ModeCertificats,
  PedagogieService,
  PromotionDetail,
  ResultatCertificatsLot,
} from '../../services/pedagogie.service';

/** Civilité et naissance telles qu'elles seront envoyées pour un étudiant. */
interface IdentiteSaisie {
  civilite: string;
  dateNaissance: string;
}

/**
 * Délivrance et registre des certificats.
 *
 * <p>Cette attribution relevait du formateur. Elle engage pourtant l'établissement
 * et non l'enseignant : le document porte désormais « INPG — Direction pédagogique »
 * et non le nom de la personne connectée.
 *
 * <p>La délivrance se fait par promotion : on choisit d'abord la promotion
 * (obligatoire), puis « tous les étudiants », « tous sauf… » ou « un seul étudiant »
 * (un retardataire, une délivrance isolée). La formation n'est jamais saisie — une promotion suit une seule formation, c'est elle qui donne
 * l'intitulé imprimé.
 */
@Component({
  selector: 'app-pedagogie-certificats',
  standalone: true,
  imports: [CommonModule, FormsModule, UiComboboxComponent, UiBadgeComponent],
  templateUrl: './pedagogie-certificats.component.html',
})
export class PedagogieCertificatsComponent implements OnInit {
  private pedagogie = inject(PedagogieService);
  private requeteApercu?: Subscription;

  chargement = true;
  certificats: Certificat[] = [];
  promotions: PromotionDetail[] = [];
  optionsPromotions: OptionCombobox[] = [];

  formulaireOuvert = false;
  promotionId: number | null = null;
  apercu: ApercuCertificats | null = null;
  chargementApercu = false;
  erreurApercu = '';
  mode: ModeCertificats = 'TOUS';
  exclusIds: number[] = [];
  /** Mode « un seul étudiant » : l'étudiant choisi. */
  etudiantId: number | null = null;
  dateDebut = '';
  dateFin = '';
  codeSession = '';
  /** Par identifiant d'étudiant ; préremplie avec ce que la fiche connaît déjà. */
  identites: Record<number, IdentiteSaisie> = {};
  enregistrement = false;
  telechargementEnCoursId: number | null = null;

  readonly civilites = ['Monsieur', 'Madame', 'Mademoiselle'];

  messageErreur = '';
  messageSucces = '';
  /** Étudiants écartés lors de la dernière délivrance collective, avec la raison. */
  refusesDernierLot: ResultatCertificatsLot['refuses'] = [];

  ngOnInit(): void {
    this.charger();
  }

  private charger(): void {
    this.chargement = true;
    forkJoin({
      certificats: this.pedagogie.getCertificats(),
      promotions: this.pedagogie.getPromotions(),
    }).subscribe({
      next: ({ certificats, promotions }) => {
        this.certificats = certificats || [];
        this.promotions = (promotions as unknown as PromotionDetail[]) || [];
        this.optionsPromotions = this.promotions.map((p) => ({
          valeur: p.id,
          libelle: p.libelle,
          detail: `${p.formationIntitule ?? 'Aucune formation rattachée'} · ${p.effectif} étudiant(s)`,
        }));
        this.chargement = false;
      },
      error: () => {
        this.chargement = false;
        this.messageErreur = 'Le registre n’a pas pu être chargé.';
      },
    });
  }

  ouvrirFormulaire(): void {
    this.requeteApercu?.unsubscribe();
    this.promotionId = null;
    this.apercu = null;
    this.chargementApercu = false;
    this.erreurApercu = '';
    this.mode = 'TOUS';
    this.exclusIds = [];
    this.etudiantId = null;
    this.dateDebut = '';
    this.dateFin = '';
    this.codeSession = '';
    this.identites = {};
    this.formulaireOuvert = true;
    this.messageErreur = '';
    this.messageSucces = '';
    this.refusesDernierLot = [];
  }

  /** Choisir (ou changer) la promotion recharge tout ce qui en dépend : formation, période, étudiants. */
  surPromotion(selection: number[]): void {
    this.promotionId = selection[0] ?? null;
    this.requeteApercu?.unsubscribe();
    this.apercu = null;
    this.erreurApercu = '';
    this.exclusIds = [];
    this.etudiantId = null;
    this.identites = {};
    if (this.promotionId == null) {
      return;
    }
    this.chargementApercu = true;
    this.requeteApercu = this.pedagogie.getApercuCertificats(this.promotionId).subscribe({
      next: (apercu) => {
        this.apercu = apercu;
        // Période de la promotion proposée par défaut ; celle d'une formation précise se corrige.
        this.dateDebut = apercu.dateDebut ?? '';
        this.dateFin = apercu.dateFin ?? '';
        this.identites = Object.fromEntries(
          apercu.etudiants.map((e) => [e.id, { civilite: e.civilite ?? '', dateNaissance: e.dateNaissance ?? '' }])
        );
        this.chargementApercu = false;
      },
      error: (erreur) => {
        this.chargementApercu = false;
        this.erreurApercu = erreur.error?.message || 'Les étudiants de cette promotion n’ont pas pu être chargés.';
      },
    });
  }

  /** Le champ renvoie une liste : vide quand on efface le choix. */
  surEtudiant(selection: number[]): void {
    this.etudiantId = selection[0] ?? null;
  }

  choisirMode(mode: ModeCertificats): void {
    this.mode = mode;
  }

  get formationManquante(): boolean {
    return !!this.apercu && !this.apercu.formationIntitule;
  }

  /** Les étudiants de la promotion pour le choix des exclus. */
  get optionsEtudiants(): OptionCombobox[] {
    return (this.apercu?.etudiants ?? []).map((e) => ({
      valeur: e.id,
      libelle: e.nom,
      detail: e.matricule ?? undefined,
    }));
  }

  /** Étudiants visés : toute la promotion, moins les exclus en « tous sauf », ou le seul étudiant choisi. */
  get beneficiaires(): EtudiantApercuCertificat[] {
    const etudiants = this.apercu?.etudiants ?? [];
    if (this.mode === 'UN_SEUL') {
      return etudiants.filter((e) => e.id === this.etudiantId);
    }
    const exclus = this.mode === 'TOUS_SAUF' ? new Set(this.exclusIds) : new Set<number>();
    return etudiants.filter((e) => !exclus.has(e.id));
  }

  /** Ceux qui recevront réellement un certificat : programme terminé, pas déjà certifiés sur cette période. */
  get destinataires(): EtudiantApercuCertificat[] {
    return this.beneficiaires.filter((e) => e.eligible && !this.dejaDelivre(e));
  }

  get ecartes(): EtudiantApercuCertificat[] {
    return this.beneficiaires.filter((e) => !e.eligible || this.dejaDelivre(e));
  }

  /** Destinataires dont la civilité, imprimée par le modèle INPG, reste à choisir. */
  get sansCivilite(): EtudiantApercuCertificat[] {
    return this.destinataires.filter((e) => !this.identites[e.id]?.civilite);
  }

  get periodeInversee(): boolean {
    return !!this.dateDebut && !!this.dateFin && this.dateFin < this.dateDebut;
  }

  get saisieValide(): boolean {
    return (
      this.promotionId != null &&
      !!this.apercu &&
      !this.formationManquante &&
      !!this.dateDebut &&
      !!this.dateFin &&
      !this.periodeInversee &&
      (this.mode !== 'TOUS_SAUF' || this.exclusIds.length > 0) &&
      (this.mode !== 'UN_SEUL' || this.etudiantId != null) &&
      this.destinataires.length > 0 &&
      this.sansCivilite.length === 0
    );
  }

  /** Ce qui empêche de délivrer, dit en clair sous le bouton. */
  get obstacle(): string {
    if (this.promotionId == null) {
      return 'Choisissez d’abord la promotion.';
    }
    if (!this.apercu) {
      return '';
    }
    if (this.formationManquante) {
      return 'Cette promotion n’est rattachée à aucune formation : rattachez-la d’abord à sa formation.';
    }
    if (this.mode === 'TOUS_SAUF' && this.exclusIds.length === 0) {
      return 'Choisissez au moins un étudiant à exclure, ou passez à « Tous les étudiants ».';
    }
    if (this.mode === 'UN_SEUL' && this.etudiantId == null) {
      return 'Choisissez l’étudiant à certifier.';
    }
    if (!this.dateDebut || !this.dateFin) {
      return 'Indiquez le début et la fin de la formation.';
    }
    if (this.periodeInversee) {
      return 'La fin de la formation précède son début.';
    }
    if (this.destinataires.length === 0) {
      return 'Aucun étudiant ne peut recevoir de certificat pour l’instant.';
    }
    if (this.sansCivilite.length > 0) {
      return `Choisissez la civilité de ${this.sansCivilite.length} étudiant(s) : le certificat l’imprime avant le nom.`;
    }
    return '';
  }

  /** Aperçu du code d'un certificat, pour que le numéro d'ordre ajouté ne surprenne pas. */
  get exempleCode(): string {
    const code = this.codeSession.trim();
    return code && this.mode !== 'UN_SEUL' ? `${code}/01, ${code}/02…` : '';
  }

  /** Même règle que le serveur : même étudiant, même formation, même période. */
  dejaDelivre(etudiant: EtudiantApercuCertificat): boolean {
    const intitule = this.apercu?.formationIntitule?.trim().toLowerCase();
    if (!intitule || !this.dateDebut || !this.dateFin) {
      return false;
    }
    // Le registre sert les dates en JJ/MM/AAAA, le formulaire les tient en AAAA-MM-JJ.
    const debut = this.enJour(this.dateDebut);
    const fin = this.enJour(this.dateFin);
    return this.certificats.some(
      (c) =>
        c.etudiantId === etudiant.id &&
        c.intituleFormation.trim().toLowerCase() === intitule &&
        c.dateDebut === debut &&
        c.dateFin === fin
    );
  }

  private enJour(iso: string): string {
    const [annee, mois, jour] = iso.split('-');
    return `${jour}/${mois}/${annee}`;
  }

  delivrer(): void {
    if (!this.saisieValide || this.enregistrement || this.promotionId == null) {
      return;
    }
    this.enregistrement = true;
    this.messageErreur = '';
    this.pedagogie
      .delivrerCertificatsLot({
        promotionId: this.promotionId,
        mode: this.mode,
        exclusIds: this.mode === 'TOUS_SAUF' ? this.exclusIds : [],
        etudiantId: this.mode === 'UN_SEUL' ? this.etudiantId : null,
        dateDebut: this.dateDebut,
        dateFin: this.dateFin,
        codeSession: this.codeSession.trim() || null,
        identites: this.destinataires.map((e) => ({
          etudiantId: e.id,
          civilite: this.identites[e.id]?.civilite || null,
          dateNaissance: this.identites[e.id]?.dateNaissance || null,
        })),
      })
      .subscribe({
        next: (resultat) => {
          this.enregistrement = false;
          this.formulaireOuvert = false;
          this.refusesDernierLot = resultat.refuses;
          const n = resultat.delivres.length;
          this.messageSucces =
            n > 0
              ? `${n} certificat(s) délivré(s) pour la promotion ${this.apercu?.promotionLibelle ?? ''}.`
              : 'Aucun certificat n’a été délivré.';
          this.charger();
        },
        error: (erreur) => {
          this.enregistrement = false;
          this.messageErreur = erreur.error?.message || 'La délivrance a échoué.';
        },
      });
  }

  /** Télécharge le PDF au modèle INPG (le jeton passe par l'intercepteur). */
  telecharger(certificat: Certificat): void {
    this.telechargementEnCoursId = certificat.id;
    this.messageErreur = '';
    this.pedagogie.telechargerCertificatPdf(certificat.id).subscribe({
      next: (blob) => {
        this.telechargementEnCoursId = null;
        const url = window.URL.createObjectURL(blob);
        const lien = document.createElement('a');
        lien.href = url;
        lien.download = `INPG-certificat-${certificat.reference}.pdf`;
        lien.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.telechargementEnCoursId = null;
        this.messageErreur = 'Le certificat n’a pas pu être téléchargé.';
      },
    });
  }
}
