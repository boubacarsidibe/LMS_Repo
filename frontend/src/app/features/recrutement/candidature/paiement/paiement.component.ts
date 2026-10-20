import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { RecrutementService, CandidatureResponse, FraisDossier, PaiementResponse } from '../../../../core/recrutement/services/recrutement.service';
import { FooterComponent } from '../../../../shared/components/footer/footer.component';
import { downloadBlob } from '../../../../shared/utils/download-file';
import { NotificationService } from '../../../../shared/components/ui/notification.service';

@Component({
  selector: 'app-paiement',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, FooterComponent],
  templateUrl: './paiement.component.html'
})
export class PaiementComponent implements OnInit {
  candidature: CandidatureResponse | null = null;
  candidatureId: number | null = null;
  paiement: PaiementResponse | null = null;
  /** Frais annoncés avant tout règlement ; une fois la tentative enregistrée, c'est le montant persisté qui fait foi. */
  fraisPrevus: FraisDossier | null = null;
  loading = false;
  telechargementRecu = false;
  actualisationEnCours = false;
  currentDate = '';
  /** Bascule vers le formulaire même si un paiement EN_ATTENTE/ECHEC existe déjà — permet de changer de moyen. */
  modifierMethode = false;

  // Méthode de paiement
  paymentMethod = 'mobile_money';
  selectedOperator = 'wave';

  mobileOperators = [
    { code: 'wave', name: 'Wave', logo: 'https://lh3.googleusercontent.com/aida-public/AB6AXuCu5Fx6MGPwXW1O6nXZUIge8lJW5ETKGegBrzOxfjTArVgilG1l6cVQzr73utkVc2reBbvFe1yzClqxmu0utfk8CrZd0HvguvXgRa1U8Yr5CArrl1dlgQoJHVOv0Om_iY78aycE8tjPh__lfbRhx93Z7JeYR8r9vmBWdbTsqgbQUgJlDVlPT30k1dKoma5woDthMs9XhYRylfBt94rrYtZOfPZlL5PWSyMQnhgPez76zbbE9aoieDMN' },
    { code: 'orange', name: 'Orange', logo: 'https://lh3.googleusercontent.com/aida-public/AB6AXuCS72PLqCwDeZ6ADiQq5TPqsToYiGsQlsOhaUDl_qSSEhXsbE2idfScJxM0b1gq6exDxMhmJGWcESqRXI_7ApJN8qflXqh2qtgV-3XmIExGSoNRgUBesD69RHbLHT4Ci89rSAfwF5z5bCcnbnCfHyT6x0oWS3H69P3L58-DKRRlHTcbA8avZ1e-PxKIaMSj6w76IVmpQ2wAOQ-uouoQvIrfNdSziOj2EymKUH19uGyQNTlB-m84xxi2' },
    { code: 'mtn', name: 'MTN', logo: 'https://lh3.googleusercontent.com/aida-public/AB6AXuB6osJqLr9ZkEOAQJ5KMm95-zApcBkptb6UU_w_LM2Ma5Wip8U9emKPr1pduKMaOoKglLE8nAZG95uJ8ofHpEA-RVVUlaflkRElODOC-58FQTycjhOaWpIa4Wt3yWmzCpV0AuuVs17X9akL3wG5Or7zWSd7_a_3XYTelayBrHJpyutM5bAtiQ0t9YZiMB2IZW2XWyig8XVxY4b-4TBHxBOnrMjjDYgRczBnFkjxqeSOivTZykZYchL5' },
    { code: 'free', name: 'Free', logo: 'https://lh3.googleusercontent.com/aida-public/AB6AXuCkUjVymICefMY8aerJhlNHovbVuhqrUOCVh-IdpWcWMe5lWnUTmPuj5dNx64-F80RUwer8MJjih3HP9-ND8OI4liwofu0EDYZfBprvCf3IgD68NfhkqacmyeY10hpDMkMX-GWXJ8JHda8b8lOZe7hHzk7-rJXHh1BGG4FVS81nWAKVcAsmNHNs01NTEctltstklG1tBQ6PZEoxC-_9jDfRdbSryGPNWQMbu4Y42vvi2URvZfWbDCku' },
  ];

  constructor(
    private route: ActivatedRoute,
    private recrutementService: RecrutementService,
    private router: Router,
    private notifications: NotificationService
  ) { }

  get paiementReussi(): boolean {
    // Seul SUCCES existe côté serveur (EnumStatutPaiement : EN_ATTENTE, SUCCES, ECHEC) ;
    // le 'CONFIRME' qui figurait ici ne correspondait à aucune valeur émise.
    return this.paiement?.statut === 'SUCCES';
  }

  /** Tentative enregistrée côté serveur, en attente de confirmation par l'admission. */
  get paiementEnAttente(): boolean {
    return this.paiement?.statut === 'EN_ATTENTE';
  }

  /** Tentative précédente soldée en échec — un nouveau règlement reste possible. */
  get paiementEchoue(): boolean {
    return this.paiement?.statut === 'ECHEC';
  }

  get nomComplet(): string {
    return [this.candidature?.candidatPrenom, this.candidature?.candidatNom].filter(Boolean).join(' ');
  }

  get libelleFormation(): string {
    return this.candidature?.sessionCandidatureLibelle || '—';
  }

  get referenceTransaction(): string {
    return this.paiement?.referenceTransaction || '—';
  }

  get montantFormate(): string {
    // Le montant enregistré fait foi ; avant tout règlement, on affiche les frais annoncés par le serveur — le candidat
    // ne doit pas déclarer un paiement sans savoir combien il règle.
    const source = this.paiement ?? this.fraisPrevus;
    if (source?.montant == null) return 'Montant indisponible';
    const devise = source.devise === 'XOF' ? 'FCFA' : source.devise;
    return `${source.montant.toLocaleString('fr-FR')} ${devise}`;
  }

  /** Libellé lisible du moyen de paiement actuellement sélectionné dans le formulaire. */
  private get methodeSelectionnee(): string {
    if (this.paymentMethod === 'transfer') return 'Virement Bancaire';
    return this.mobileOperators.find(op => op.code === this.selectedOperator)?.name || 'Mobile Money';
  }

  get methodePaiement(): string {
    // Une fois le paiement enregistré, la valeur persistée fait foi : après un
    // rechargement de la page, l'état du formulaire ne reflète plus le choix réel.
    if (this.paiement?.methodePaiement) return this.paiement.methodePaiement;
    return this.paiementReussi ? this.methodeSelectionnee : '—';
  }

  ngOnInit(): void {
    this.updateCurrentDate();
    this.route.queryParams.subscribe(params => {
      this.candidatureId = params['candidatureId'] ? +params['candidatureId'] : null;
      if (this.candidatureId) {
        this.chargerCandidature();
      }
    });
  }

  private chargerCandidature(): void {
    if (!this.candidatureId) return;
    this.recrutementService.getCandidature(this.candidatureId).subscribe({
      next: (data) => {
        this.candidature = data;
        this.paiement = data.paiement;
      }
    });
    this.recrutementService.getFraisDossier(this.candidatureId).subscribe({
      next: (frais) => (this.fraisPrevus = frais),
      error: () => (this.fraisPrevus = null),
    });
  }

  /** Revient au formulaire pour régler avec un autre moyen — une tentative EN_ATTENTE/ECHEC reste rejouable côté serveur. */
  essayerAutreMoyen(): void {
    this.modifierMethode = true;
  }

  /** Recharge le statut depuis le serveur — l'admission peut avoir confirmé le règlement hors ligne entre-temps. */
  actualiserStatut(): void {
    if (!this.candidatureId || this.actualisationEnCours) return;
    this.actualisationEnCours = true;
    this.recrutementService.getCandidature(this.candidatureId).subscribe({
      next: (data) => {
        this.candidature = data;
        this.paiement = data.paiement;
        this.actualisationEnCours = false;
      },
      error: () => {
        this.actualisationEnCours = false;
      }
    });
  }

  private updateCurrentDate(): void {
    const now = new Date();
    this.currentDate = now.toLocaleDateString('fr-FR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }

  onMethodChange(): void {
    // Réinitialiser les champs au changement de méthode
  }

  initierPaiement(): void {
    if (!this.candidatureId) return;
    this.loading = true;

    // Ni montant ni devise ne sont envoyés : les frais de dossier sont une donnée
    // métier que le serveur détermine seul. Les transmettre depuis le navigateur
    // laissait un client choisir le prix qu'il paie.
    this.recrutementService.initierPaiement({
      candidatureId: this.candidatureId,
      methodePaiement: this.methodeSelectionnee
    }).subscribe({
      next: (response) => {
        this.paiement = response;
        this.loading = false;
        this.modifierMethode = false;
        this.updateCurrentDate();
        // La vue de confirmation ou d'attente s'affichera automatiquement selon le statut renvoyé.
      },
      error: (err) => {
        this.notifications.erreur('Erreur de paiement: ' + (err.error?.message || 'Erreur inconnue'));
        this.loading = false;
      }
    });
  }

  telechargerRecu(): void {
    if (!this.paiement?.id || this.telechargementRecu) return;
    this.telechargementRecu = true;

    this.recrutementService.telechargerRecuPaiement(this.paiement.id).subscribe({
      next: (blob) => {
        const dossier = this.candidature?.numeroDossier;
        downloadBlob(blob, dossier ? `recu-${dossier}.pdf` : 'recu-paiement.pdf');
        this.telechargementRecu = false;
      },
      error: () => {
        this.telechargementRecu = false;
        this.notifications.erreur('Échec du téléchargement du reçu.');
      }
    });
  }

  accederEspaceCandidat(): void {
    this.router.navigate(['/recrutement/suivi']);
  }
}
