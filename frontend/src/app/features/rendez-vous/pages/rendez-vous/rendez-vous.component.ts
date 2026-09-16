import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Auth } from '../../../../core/auth/services/authService';
import {
  Creneau,
  DemandeRendezVousSaisie,
  LIBELLES_MODALITE,
  LIBELLES_STATUT,
  ModaliteRendezVous,
  PersonnelProfil,
  RendezVous,
  StatutRendezVous,
} from '../../models/rendez-vous.models';
import { RendezVousService } from '../../services/rendez-vous.service';

const ROLES_PERSONNEL = ['ROLE_FORMATEUR', 'ROLE_RESPONSABLE_PEDAGOGIQUE', 'ROLE_RESPONSABLE_ADMISSION'];
const ROLES_EXTERNE = ['ROLE_ECOLE_FORMATION', 'ROLE_ENTREPRISE'];

/**
 * Gestion des rendez-vous (TDR §J) : « Chaque utilisateur pourra consulter le
 * profil des membres du personnel de l'INPG et envoyer une demande de
 * rendez-vous en choisissant un créneau disponible [...]. Les membres du
 * personnel auront la possibilité d'accepter, de refuser ou de proposer une
 * autre date. »
 *
 * <p>Un seul composant, partagé par tous les espaces qui y renvoient depuis
 * leur propre navigation (étudiant, formateur, pédagogie, admission, école,
 * entreprise) — même modélisation, pas de triplication par rôle. « Mon agenda »
 * (gestion des créneaux et réponse aux demandes) n'apparaît qu'aux rôles
 * internes établis comme « personnel ».
 *
 * <p>Deux adaptations selon le rôle connecté :
 * <ul>
 *   <li><b>École partenaire / entreprise</b> : leur unique interlocuteur est le
 *       responsable pédagogique (décision client, garde côté serveur). L'écran
 *       ne montre donc que « Demander un rendez-vous » avec ce seul contact,
 *       pré-sélectionné, et « Mes demandes ».</li>
 *   <li><b>Personnel de l'INPG</b> (formateur, responsable pédagogique,
 *       responsable admission) : il ne demande jamais de rendez-vous, il les
 *       reçoit — l'écran ne montre que « Mon agenda » (créneaux + demandes
 *       reçues), sans onglet « Demander ». Un personnel n'a pas besoin de
 *       solliciter un autre personnel de la même façon qu'un candidat ou une
 *       école : c'est lui qui gère les demandes, jamais l'inverse.</li>
 * </ul>
 */
@Component({
  selector: 'app-rendez-vous',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './rendez-vous.component.html',
})
export class RendezVousComponent implements OnInit {
  private rendezVousService = inject(RendezVousService);
  private auth = inject(Auth);

  libellesModalite = LIBELLES_MODALITE;
  libellesStatut = LIBELLES_STATUT;

  private get roles(): string[] {
    return this.auth.currentUser()?.roles ?? [];
  }

  get estPersonnel(): boolean {
    return ROLES_PERSONNEL.some((r) => this.roles.includes(r));
  }

  /** École partenaire ou entreprise : demandeur « externe », interlocuteur = responsable pédagogique. */
  get estExterne(): boolean {
    return ROLES_EXTERNE.some((r) => this.roles.includes(r));
  }

  /** Sous-titre de l'écran, adapté au rôle. */
  get sousTitre(): string {
    if (this.estExterne) {
      return "Demandez un rendez-vous au responsable pédagogique de l'INPG sur l'un de ses créneaux disponibles.";
    }
    if (this.estPersonnel) {
      return 'Gérez vos créneaux de disponibilité et répondez aux demandes de rendez-vous que vous recevez.';
    }
    return "Consultez le personnel de l'INPG et demandez un rendez-vous sur un créneau disponible.";
  }

  // ===== Demander un rendez-vous =====
  personnel: PersonnelProfil[] = [];
  chargementPersonnel = true;
  personnelSelectionne: PersonnelProfil | null = null;
  creneauxLibres: Creneau[] = [];
  chargementCreneaux = false;
  creneauSelectionne: Creneau | null = null;

  saisie: DemandeRendezVousSaisie = this.saisieVierge();
  enregistrement = false;

  mesDemandes: RendezVous[] = [];
  chargementDemandes = true;

  // ===== Mon agenda (personnel) =====
  mesCreneaux: Creneau[] = [];
  demandesRecues: RendezVous[] = [];
  chargementAgenda = true;
  formulaireCreneauOuvert = false;
  nouveauDebut = '';
  nouveauFin = '';

  propositionOuvertePourId: number | null = null;
  propositionDebut = '';
  propositionFin = '';
  commentaireReponse = '';

  messageErreur = '';
  messageSucces = '';

  ngOnInit(): void {
    // Le personnel de l'INPG ne demande jamais de rendez-vous : inutile de
    // charger l'annuaire du personnel ou un historique de demandes qui restera
    // toujours vide pour lui.
    if (this.estPersonnel) {
      this.chargerAgenda();
      return;
    }
    this.chargerPersonnel();
    this.chargerMesDemandes();
  }

  private saisieVierge(): DemandeRendezVousSaisie {
    return { creneauId: null, modalite: 'EN_LIGNE', lieuOuLien: '', motif: '' };
  }

  private reinitialiserMessages(): void {
    this.messageErreur = '';
    this.messageSucces = '';
  }

  // ===== Demander un rendez-vous =====

  chargerPersonnel(): void {
    this.chargementPersonnel = true;
    const monId = this.auth.currentUser()?.id;
    // Écoles / entreprises : on ne propose que le responsable pédagogique.
    this.rendezVousService.getPersonnel(this.estExterne ? 'PEDAGOGIE' : undefined).subscribe({
      next: (personnel) => {
        this.personnel = personnel.filter((p) => p.id !== monId);
        this.chargementPersonnel = false;
        // Interlocuteur unique : on le sélectionne d'emblée pour aller droit aux créneaux.
        if (this.estExterne && this.personnel.length === 1) {
          this.choisirPersonnel(this.personnel[0]);
        }
      },
      error: () => {
        this.personnel = [];
        this.chargementPersonnel = false;
      },
    });
  }

  choisirPersonnel(p: PersonnelProfil): void {
    this.personnelSelectionne = p;
    this.creneauSelectionne = null;
    this.saisie = this.saisieVierge();
    this.chargementCreneaux = true;
    this.rendezVousService.getCreneauxLibres(p.id).subscribe({
      next: (creneaux) => {
        this.creneauxLibres = creneaux;
        this.chargementCreneaux = false;
      },
      error: () => {
        this.creneauxLibres = [];
        this.chargementCreneaux = false;
      },
    });
  }

  choisirCreneau(c: Creneau): void {
    this.creneauSelectionne = c;
    this.saisie = { ...this.saisieVierge(), creneauId: c.id };
    this.reinitialiserMessages();
  }

  get demandeValide(): boolean {
    return !!this.saisie.creneauId
      && (this.saisie.modalite !== 'PRESENTIEL' || !!this.saisie.lieuOuLien.trim());
  }

  envoyerDemande(): void {
    if (!this.demandeValide || this.enregistrement) {
      return;
    }
    this.enregistrement = true;
    this.reinitialiserMessages();
    this.rendezVousService.demanderRendezVous(this.saisie).subscribe({
      next: () => {
        this.enregistrement = false;
        this.messageSucces = 'Votre demande de rendez-vous a été envoyée.';
        this.personnelSelectionne = null;
        this.creneauSelectionne = null;
        this.creneauxLibres = [];
        this.saisie = this.saisieVierge();
        this.chargerMesDemandes();
      },
      error: (erreur) => {
        this.enregistrement = false;
        this.messageErreur = erreur.error?.message || "L'envoi de la demande a échoué.";
      },
    });
  }

  chargerMesDemandes(): void {
    this.chargementDemandes = true;
    this.rendezVousService.getMesDemandes().subscribe({
      next: (demandes) => {
        this.mesDemandes = demandes;
        this.chargementDemandes = false;
      },
      error: () => {
        this.mesDemandes = [];
        this.chargementDemandes = false;
      },
    });
  }

  annulerDemande(rdv: RendezVous): void {
    if (!confirm('Annuler cette demande de rendez-vous ?')) {
      return;
    }
    this.reinitialiserMessages();
    this.rendezVousService.annulerDemande(rdv.id).subscribe({
      next: () => this.chargerMesDemandes(),
      error: (erreur) => (this.messageErreur = erreur.error?.message || "L'annulation a échoué."),
    });
  }

  repondreProposition(rdv: RendezVous, accepter: boolean): void {
    this.reinitialiserMessages();
    this.rendezVousService.repondreProposition(rdv.id, accepter).subscribe({
      next: () => this.chargerMesDemandes(),
      error: (erreur) => (this.messageErreur = erreur.error?.message || 'La réponse a échoué.'),
    });
  }

  // ===== Mon agenda (personnel) =====

  chargerAgenda(): void {
    this.chargementAgenda = true;
    this.rendezVousService.getMesCreneaux().subscribe({
      next: (creneaux) => (this.mesCreneaux = creneaux),
      error: () => (this.mesCreneaux = []),
    });
    this.rendezVousService.getDemandesRecues().subscribe({
      next: (demandes) => {
        this.demandesRecues = demandes;
        this.chargementAgenda = false;
      },
      error: () => {
        this.demandesRecues = [];
        this.chargementAgenda = false;
      },
    });
  }

  ouvrirFormulaireCreneau(): void {
    this.nouveauDebut = '';
    this.nouveauFin = '';
    this.formulaireCreneauOuvert = true;
    this.reinitialiserMessages();
  }

  creerCreneau(): void {
    if (!this.nouveauDebut || !this.nouveauFin) {
      return;
    }
    this.reinitialiserMessages();
    this.rendezVousService.creerCreneau(this.nouveauDebut, this.nouveauFin).subscribe({
      next: () => {
        this.formulaireCreneauOuvert = false;
        this.chargerAgenda();
      },
      error: (erreur) => (this.messageErreur = erreur.error?.message || 'La création du créneau a échoué.'),
    });
  }

  supprimerCreneau(c: Creneau): void {
    if (!confirm('Retirer ce créneau ?')) {
      return;
    }
    this.reinitialiserMessages();
    this.rendezVousService.supprimerCreneau(c.id).subscribe({
      next: () => this.chargerAgenda(),
      error: (erreur) =>
        (this.messageErreur = erreur.error?.message || 'Ce créneau fait l’objet d’une demande, il ne peut pas être retiré.'),
    });
  }

  accepterDemande(rdv: RendezVous): void {
    this.reinitialiserMessages();
    this.rendezVousService.accepter(rdv.id).subscribe({
      next: () => this.chargerAgenda(),
      error: (erreur) => (this.messageErreur = erreur.error?.message || "L'acceptation a échoué."),
    });
  }

  refuserDemande(rdv: RendezVous): void {
    this.reinitialiserMessages();
    this.rendezVousService.refuser(rdv.id).subscribe({
      next: () => this.chargerAgenda(),
      error: (erreur) => (this.messageErreur = erreur.error?.message || 'Le refus a échoué.'),
    });
  }

  ouvrirProposition(rdv: RendezVous): void {
    this.propositionOuvertePourId = rdv.id;
    this.propositionDebut = '';
    this.propositionFin = '';
    this.commentaireReponse = '';
  }

  envoyerProposition(rdv: RendezVous): void {
    if (!this.propositionDebut || !this.propositionFin) {
      return;
    }
    this.reinitialiserMessages();
    this.rendezVousService
      .proposerAutreDate(rdv.id, this.propositionDebut, this.propositionFin, this.commentaireReponse)
      .subscribe({
        next: () => {
          this.propositionOuvertePourId = null;
          this.chargerAgenda();
        },
        error: (erreur) => (this.messageErreur = erreur.error?.message || 'La proposition a échoué.'),
      });
  }

  libelleStatut(statut: StatutRendezVous): string {
    return this.libellesStatut[statut];
  }

  libelleModalite(modalite: ModaliteRendezVous): string {
    return this.libellesModalite[modalite];
  }

  classeStatut(statut: StatutRendezVous): string {
    switch (statut) {
      case 'ACCEPTE':
        return 'bg-secondary-container text-on-secondary-container';
      case 'REFUSE':
      case 'ANNULE':
        return 'bg-error-container text-on-error-container';
      case 'PROPOSITION_AUTRE_DATE':
        return 'bg-primary-fixed text-on-primary-fixed-variant';
      default:
        return 'bg-surface-container text-on-surface-variant';
    }
  }
}
