import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import {
  CritereAdmissionResponse,
  RecrutementService,
  SessionCandidatureResponse,
} from '../../../../../core/recrutement/services/recrutement.service';

/**
 * Page dédiée à la liste des critères personnalisés d'une session. Seuls les
 * critères actifs apparaissent par défaut — ce sont les seuls encore proposés
 * aux candidats ; les critères retirés (voir migration V30 : retrait toujours
 * logique, jamais physique, pour ne pas perdre les réponses déjà données) sont
 * repliés dans une section à part, ouverte à la demande.
 */
@Component({
  selector: 'app-session-criteres',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './session-criteres.component.html',
})
export class SessionCriteresComponent implements OnInit {
  sessionId!: number;
  session: SessionCandidatureResponse | null = null;
  criteres: CritereAdmissionResponse[] = [];
  loading = false;
  errorMessage = '';

  afficherRetires = false;

  filtreEnCours = false;
  filtreMessage = '';

  constructor(
    private recrutementService: RecrutementService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit(): void {
    // Abonnement au paramMap : Angular réutilise l'instance du composant en
    // naviguant entre deux sessions sur la même route, sans rappeler ngOnInit.
    this.route.paramMap.subscribe((params) => {
      this.sessionId = Number(params.get('id'));
      this.afficherRetires = false;
      this.charger();
    });
  }

  private charger(): void {
    this.loading = true;
    this.errorMessage = '';
    this.recrutementService.getSession(this.sessionId).subscribe({
      next: (session) => (this.session = session),
      error: () => (this.errorMessage = 'Impossible de charger la session.'),
    });
    this.recrutementService.getCriteresDeSession(this.sessionId).subscribe({
      next: (data) => {
        this.criteres = data || [];
        this.loading = false;
      },
      error: () => {
        this.criteres = [];
        this.loading = false;
        this.errorMessage = 'Impossible de charger les critères.';
      },
    });
  }

  get criteresActifsListe(): CritereAdmissionResponse[] {
    return this.criteres.filter((c) => c.actif);
  }

  get criteresRetires(): CritereAdmissionResponse[] {
    return this.criteres.filter((c) => !c.actif);
  }

  ajouterCritere(): void {
    this.router.navigate(['/responsable-admission/sessions', this.sessionId, 'criteres', 'nouveau']);
  }

  modifierCritere(critere: CritereAdmissionResponse): void {
    this.router.navigate(['/responsable-admission/sessions', this.sessionId, 'criteres', critere.id]);
  }

  retirer(critere: CritereAdmissionResponse): void {
    if (!confirm(`Retirer le critère « ${critere.libelle} » du formulaire de candidature ?`)) return;
    this.recrutementService.retirerCritere(critere.id).subscribe({
      next: () => this.charger(),
      error: () => alert('Échec du retrait du critère.'),
    });
  }

  reactiver(critere: CritereAdmissionResponse): void {
    this.recrutementService.reactiverCritere(critere.id).subscribe({
      next: () => this.charger(),
      error: () => alert('Échec de la réactivation du critère.'),
    });
  }

  appliquerFiltre(): void {
    if (!confirm(
      'Appliquer le filtre automatique sur les dossiers « Soumis » de cette session ? ' +
      'Chaque dossier qui ne remplit pas les critères sera automatiquement rejeté ; les autres passeront en présélection.'
    )) return;

    this.filtreEnCours = true;
    this.filtreMessage = '';
    this.recrutementService.appliquerFiltreCriteres(this.sessionId).subscribe({
      next: (resultat) => {
        this.filtreEnCours = false;
        this.filtreMessage = `${resultat.total} dossier(s) évalué(s) : ${resultat.preselectionnes} en présélection, ${resultat.rejetes} rejeté(s).`;
      },
      error: (err) => {
        this.filtreEnCours = false;
        this.filtreMessage = err.error?.message || "Échec de l'application du filtre.";
      },
    });
  }
}
