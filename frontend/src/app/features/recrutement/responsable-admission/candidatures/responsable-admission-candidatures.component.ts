import { Component, HostListener, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { forkJoin, Observable } from 'rxjs';
import { CandidatureResponse, FormatRapport, RecrutementService, ResultatRelance } from '../../../../core/recrutement/services/recrutement.service';
import { downloadBlob } from '../../../../shared/utils/download-file';
import { ColonneExport, ExportColumnsModalComponent } from '../../../../shared/components/export-columns-modal/export-columns-modal.component';
import { NotificationService } from '../../../../shared/components/ui/notification.service';
import { ConfirmationService } from '../../../../shared/components/ui/confirmation.service';
import {
  actionSuivante as actionSuivanteDe,
  attenteDuDossier,
  CIBLES_DE_RECUL,
  classeStatut as classeStatutDe,
  GROUPES_STATUTS,
  libelleStatut as libelleStatutDe,
  peutAnnuler as peutAnnulerDe,
  peutReculerVers as peutReculerVersDe,
  peutRejeter as peutRejeterDe,
  statutsDuFiltre,
} from '../../../../core/recrutement/statuts-candidature';

/**
 * Clé d'un onglet (« A_INSTRUIRE », « TEST »… voir GROUPES_STATUTS) ou statut précis (« SOUMIS ») : un onglet regroupe
 * plusieurs des vingt statuts du serveur. L'ancien type ne connaissait que les cinq statuts d'avant le 22/09.
 */
type StatutFiltre = string;

@Component({
  selector: 'app-responsable-admission-candidatures',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, ExportColumnsModalComponent],
  templateUrl: './responsable-admission-candidatures.component.html',
  styleUrl: './responsable-admission-candidatures.component.css',
})
export class ResponsableAdmissionCandidaturesComponent implements OnInit {
  candidatures: CandidatureResponse[] = [];
  loading = false;
  errorMessage = '';

  recherche = '';
  filtreStatut: StatutFiltre = 'TOUS';
  filtreSessionId: number | null = null;

  /** Recalculées explicitement (pas des getters) : un getter renvoyant un nouveau
   * tableau à chaque cycle de détection de changements provoque NG0103 (boucle
   * infinie de rafraîchissement), notamment via le <select ngModel> des sessions. */
  sessionsDisponibles: { id: number; libelle: string }[] = [];
  candidaturesFiltrees: CandidatureResponse[] = [];

  /**
   * Pagination de la liste. Faite côté navigateur : la liste complète est déjà chargée,
   * et les onglets comme la recherche comptent sur l'ensemble des dossiers, pas sur la
   * page affichée. Elle évite le mur de plusieurs centaines de lignes d'un seul tenant.
   */
  page = 1;
  readonly taillePage = 10;

  /** Onglets du parcours, dans l'ordre où un dossier les traverse. */
  readonly onglets: { statut: StatutFiltre; libelle: string }[] =
    GROUPES_STATUTS.map((g) => ({ statut: g.cle, libelle: g.libelle }));

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.candidaturesFiltrees.length / this.taillePage));
  }

  get candidaturesPage(): CandidatureResponse[] {
    const debut = (this.page - 1) * this.taillePage;
    return this.candidaturesFiltrees.slice(debut, debut + this.taillePage);
  }

  get premierAffiche(): number {
    return this.candidaturesFiltrees.length === 0 ? 0 : (this.page - 1) * this.taillePage + 1;
  }

  get dernierAffiche(): number {
    return Math.min(this.page * this.taillePage, this.candidaturesFiltrees.length);
  }

  /** Numéros de page à afficher, resserrés autour de la page courante au-delà de 7 pages. */
  get pagesAffichees(): number[] {
    const total = this.totalPages;
    if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1);
    const autour = [1, 2, this.page - 1, this.page, this.page + 1, total - 1, total]
      .filter((p) => p >= 1 && p <= total);
    return [...new Set(autour)].sort((a, b) => a - b);
  }

  allerPage(p: number): void {
    if (p < 1 || p > this.totalPages) return;
    this.page = p;
    // L'ancre du Shift+clic désigne une ligne de l'ancienne page : la conserver
    // ferait cocher un intervalle sans rapport avec ce que l'on voit.
    this.dernierIndexClique = null;
  }

  candidatureSelectionnee: CandidatureResponse | null = null;
  actionEnCours = false;
  exporting = false;
  exportingDocuments = false;
  generatingRapport = false;
  exportModalOpen = false;
  generatingReport = false;
  relanceEnCours = false;
  relanceMessage = '';
  menuActionsOuvert = false;
  menuRapportsOuvert = false;

  /** Dossiers ADMIS dont l'inscription (étape 6, QMS-009) reste à finaliser. */
  idsEnAttenteInscription = new Set<number>();

  /**
   * Sélection multiple (checkboxes, comme dans un explorateur de fichiers) : ids des
   * candidatures cochées. Un clic Shift étend la sélection depuis le dernier élément
   * cliqué au lieu de la remplacer, à l'image d'un gestionnaire de fichiers natif.
   */
  selection = new Set<number>();
  menuContextuel: { x: number; y: number; candidature: CandidatureResponse } | null = null;
  private dernierIndexClique: number | null = null;
  rejetGroupeEnCours = false;
  retourConfirmationOpen = false;
  retourCibles: CandidatureResponse[] = [];
  retourStatutCible: string | null = null;
  motDePasseRetour = '';
  retourErreur = '';

  readonly colonnesExport: ColonneExport[] = [
    { key: 'numeroDossier', label: 'N° Dossier' },
    { key: 'nom', label: 'Nom' },
    { key: 'prenom', label: 'Prénom' },
    { key: 'email', label: 'Email' },
    { key: 'session', label: 'Session' },
    { key: 'statut', label: 'Statut' },
    { key: 'dateSoumission', label: 'Date de soumission' },
    { key: 'motifRejet', label: 'Motif de rejet' },
    { key: 'genre', label: 'Genre' },
    { key: 'niveauEtude', label: "Niveau d'études" },
    { key: 'profession', label: 'Profession' },
    { key: 'anneesExperience', label: "Années d'expérience" },
    { key: 'doc_CV', label: 'Document — CV' },
    { key: 'doc_DIPLOME', label: 'Document — Diplôme' },
    { key: 'doc_RELEVE_NOTES', label: 'Document — Relevé de notes' },
    { key: 'doc_PIECE_IDENTITE', label: "Document — Pièce d'identité" },
    { key: 'doc_LETTRE_MOTIVATION', label: 'Document — Lettre de motivation' },
  ];
  readonly colonnesExportParDefaut = ['numeroDossier', 'nom', 'prenom', 'email', 'session', 'statut', 'dateSoumission', 'motifRejet'];

  constructor(private recrutementService: RecrutementService, private route: ActivatedRoute, private router: Router,
              private notifications: NotificationService, private confirmation: ConfirmationService) {}

  ngOnInit(): void {
    const sessionParam = this.route.snapshot.queryParamMap.get('session');
    if (sessionParam) this.filtreSessionId = Number(sessionParam);
    const statutParam = this.route.snapshot.queryParamMap.get('statut');
    if (statutParam && statutsDuFiltre(statutParam) !== undefined) this.filtreStatut = statutParam;
    const dossierParam = this.route.snapshot.queryParamMap.get('dossier');
    if (dossierParam) this.recherche = dossierParam;
    this.chargerCandidatures();
  }

  private chargerCandidatures(): void {
    this.loading = true;
    this.errorMessage = '';
    this.recrutementService.getAllCandidatures().subscribe({
      next: (data) => {
        this.candidatures = data || [];
        this.loading = false;
        this.recalculerSessionsDisponibles();
        this.recalculerFiltres();
      },
      error: () => {
        this.candidatures = [];
        this.loading = false;
        this.errorMessage = 'Impossible de charger les candidatures.';
        this.recalculerSessionsDisponibles();
        this.recalculerFiltres();
      },
    });
    this.recrutementService.getCandidaturesEnAttenteInscription().subscribe({
      next: (data) => (this.idsEnAttenteInscription = new Set((data || []).map((c) => c.id))),
      error: () => (this.idsEnAttenteInscription = new Set()),
    });
  }

  /** Sessions distinctes présentes dans les candidatures chargées, pour le filtre. */
  private recalculerSessionsDisponibles(): void {
    const vues = new Map<number, string>();
    for (const c of this.candidatures) {
      if (!vues.has(c.sessionCandidatureId)) vues.set(c.sessionCandidatureId, c.sessionCandidatureLibelle);
    }
    this.sessionsDisponibles = Array.from(vues.entries())
      .map(([id, libelle]) => ({ id, libelle }))
      .sort((a, b) => a.libelle.localeCompare(b.libelle));
  }

  /** Recalcule la liste filtrée — à appeler après toute modification de candidatures,
   * recherche, filtreStatut ou filtreSessionId. */
  recalculerFiltres(): void {
    const q = this.recherche.trim().toLowerCase();
    const statutsRetenus = statutsDuFiltre(this.filtreStatut) ?? null;
    this.candidaturesFiltrees = this.candidatures.filter((c) => {
      if (statutsRetenus && !statutsRetenus.includes(c.statut as never)) return false;
      if (this.filtreSessionId !== null && c.sessionCandidatureId !== this.filtreSessionId) return false;
      if (!q) return true;
      return (
        (c.candidatNom || '').toLowerCase().includes(q) ||
        (c.candidatPrenom || '').toLowerCase().includes(q) ||
        (c.candidatEmail || '').toLowerCase().includes(q) ||
        (c.numeroDossier || '').toLowerCase().includes(q)
      );
    });
    // Changer de filtre remet en page 1 : rester en page 5 d'une liste qui n'en compte
    // plus que deux afficherait un tableau vide sans expliquer pourquoi.
    this.page = 1;
    this.dernierIndexClique = null;
  }

  /** Remet la liste à son état par défaut : tous les statuts, toutes les sessions, sans recherche. */
  reinitialiserFiltres(): void {
    this.recherche = '';
    this.filtreStatut = 'TOUS';
    this.filtreSessionId = null;
    this.recalculerFiltres();
  }

  get filtresActifs(): boolean {
    return this.recherche.trim() !== '' || this.filtreStatut !== 'TOUS' || this.filtreSessionId !== null;
  }

  get nombreFiltresActifs(): number {
    return Number(this.recherche.trim() !== '')
      + Number(this.filtreStatut !== 'TOUS')
      + Number(this.filtreSessionId !== null);
  }

  effacerRecherche(): void {
    this.recherche = '';
    this.recalculerFiltres();
  }

  basculerMenuActions(): void {
    this.menuActionsOuvert = !this.menuActionsOuvert;
    this.menuRapportsOuvert = false;
  }

  basculerMenuRapports(): void {
    this.menuRapportsOuvert = !this.menuRapportsOuvert;
    this.menuActionsOuvert = false;
  }

  fermerMenus(): void {
    this.menuActionsOuvert = false;
    this.menuRapportsOuvert = false;
  }

  setFiltreStatut(statut: StatutFiltre): void {
    this.filtreStatut = statut;
    this.recalculerFiltres();
  }

  compter(statut: StatutFiltre): number {
    const statuts = statutsDuFiltre(statut);
    if (!statuts) return this.candidatures.length;
    return this.candidatures.filter((c) => statuts.includes(c.statut as never)).length;
  }

  /** Statuts à transmettre au serveur pour l'export : ceux de l'onglet, séparés par des virgules. */
  private get filtreExport(): string | undefined {
    return statutsDuFiltre(this.filtreStatut)?.join(',') || undefined;
  }

  /** Même définition qu'ApplicationController#isDossierIncomplet côté serveur. */
  private dossierIncomplet(c: CandidatureResponse): boolean {
    return c.piecesJustificatives.length === 0 || c.piecesJustificatives.some((p) => !p.estConforme);
  }

  /** Dossier sans aucune pièce jointe : la présélection est bloquée côté backend (CandidatureService#changerStatut). */
  dossierVide(c: CandidatureResponse): boolean {
    return c.piecesJustificatives.length === 0;
  }

  get nombreDossiersIncomplets(): number {
    return this.candidatures.filter((c) => this.dossierIncomplet(c)).length;
  }

  libelleStatut(statut: string): string {
    return libelleStatutDe(statut);
  }

  classeStatut(statut: string): string {
    return classeStatutDe(statut);
  }

  /** La présélection est le geste qu'un dossier vide ne peut pas recevoir. */
  bloqueParDossierVide(c: CandidatureResponse): boolean {
    return actionSuivanteDe(c.statut)?.cle === 'VALIDER_PRESELECTION' && this.dossierVide(c);
  }

  /** Libellé + geste selon l'étape actuelle du workflow (cf. CandidatureService#estTransitionValide côté backend). */
  actionSuivante(c: CandidatureResponse): { libelle: string; action: () => void } | null {
    const geste = actionSuivanteDe(c.statut);
    if (!geste) return null;
    switch (geste.cle) {
      case 'VALIDER_PRESELECTION': return { libelle: geste.libelle, action: () => this.validerPreselection(c) };
      case 'ADMETTRE_TEST': return { libelle: geste.libelle, action: () => this.admettreTest(c) };
      case 'ADMETTRE_ENTRETIEN': return { libelle: geste.libelle, action: () => this.admettreEntretien(c) };
      case 'ADMETTRE_FINAL': return { libelle: geste.libelle, action: () => this.admettreFinal(c) };
      case 'FINALISER_INSCRIPTION':
        // Étape 6 de QMS-009 : la sélection (admettre-final) n'ouvre pas l'accès étudiant elle-même.
        return this.idsEnAttenteInscription.has(c.id)
          ? { libelle: geste.libelle, action: () => this.finaliserInscription(c) }
          : null;
    }
  }

  /** Ce qu'attend un dossier qui n'offre aucun geste (infobulle de la ligne). */
  attente(c: CandidatureResponse): string | null {
    return this.actionSuivante(c) ? null : attenteDuDossier(c.statut);
  }

  peutRejeter(c: CandidatureResponse): boolean {
    return peutRejeterDe(c.statut);
  }

  /** Une fois l'accès étudiant ouvert (INSCRIPTION_FINALISEE), la décision n'est plus annulable ici. */
  peutAnnuler(c: CandidatureResponse): boolean {
    return peutAnnulerDe(c.statut, c.statutPrecedent);
  }

  /** Cibles valides pour « faire reculer vers » — mêmes règles que CandidatureService#reculerStatut côté backend. */
  readonly ciblesReculer: { valeur: StatutFiltre; libelle: string }[] = [...CIBLES_DE_RECUL];

  private peutReculerVers(c: CandidatureResponse, cible: string): boolean {
    return peutReculerVersDe(c.statut, cible);
  }

  /** Dossiers actuellement cochés, centralisés pour que le panneau d'actions décrive la sélection réelle. */
  get candidaturesSelectionnees(): CandidatureResponse[] {
    return this.candidatures.filter((c) => this.selection.has(c.id));
  }

  get nombreRejetablesSelection(): number {
    return this.candidaturesSelectionnees.filter((c) => this.peutRejeter(c)).length;
  }

  get nombreAnnulablesSelection(): number {
    return this.candidaturesSelectionnees.filter((c) => this.peutAnnuler(c)).length;
  }

  /** Étapes réellement accessibles depuis au moins un dossier coché, avec leur nombre de dossiers concernés. */
  get ciblesReculerSelection(): Array<{ valeur: StatutFiltre; libelle: string; count: number }> {
    return this.ciblesReculer
      .map((cible) => ({
        ...cible,
        count: this.candidaturesSelectionnees.filter((c) => this.peutReculerVers(c, cible.valeur)).length,
      }))
      .filter((cible) => cible.count > 0);
  }

  /** Résumé court des statuts sélectionnés, utile pour vérifier le contexte avant une action groupée. */
  get resumeStatutsSelection(): string {
    const groupes = new Map<string, number>();
    for (const candidature of this.candidaturesSelectionnees) {
      const label = this.libelleStatut(candidature.statut);
      groupes.set(label, (groupes.get(label) || 0) + 1);
    }
    return Array.from(groupes.entries())
      .map(([label, count]) => count > 1 ? `${label} (${count})` : label)
      .join(' · ');
  }

  /** Phrase affichée dans le panneau afin d'expliquer immédiatement ce qui est faisable. */
  get resumeActionsSelection(): string {
    const possibilites: string[] = [];
    if (this.nombreRejetablesSelection) {
      possibilites.push(`rejet de ${this.nombreRejetablesSelection} dossier${this.nombreRejetablesSelection > 1 ? 's' : ''}`);
    }
    if (this.nombreAnnulablesSelection) {
      possibilites.push(`annulation de ${this.nombreAnnulablesSelection} décision${this.nombreAnnulablesSelection > 1 ? 's' : ''}`);
    }
    if (this.ciblesReculerSelection.length) {
      possibilites.push('retour à une étape antérieure');
    }
    return possibilites.length
      ? `Actions disponibles : ${possibilites.join(' · ')}.`
      : 'Aucune action de statut n’est disponible pour cette sélection.';
  }

  voirDetail(c: CandidatureResponse): void {
    this.router.navigate(['/responsable-admission/candidatures', c.id]);
  }

  // ===================== Sélection multiple =====================

  estSelectionne(id: number): boolean {
    return this.selection.has(id);
  }

  /**
   * La case d'en-tête porte sur la page affichée, pas sur la liste entière.
   *
   * <p>Elle couvrait auparavant les candidatures filtrées au complet : cliquer dessus
   * au-dessus de dix lignes visibles en sélectionnait cent vingt-quatre, et cocher les
   * dix lignes visibles ne la cochait pas. Une case au-dessus d'une colonne ne peut
   * désigner que ce qu'elle surplombe.
   */
  get toutesSelectionnees(): boolean {
    const page = this.candidaturesPage;
    return page.length > 0 && page.every((c) => this.selection.has(c.id));
  }

  /**
   * Coche/décoche une ligne ; Shift+clic étend la sélection depuis la dernière ligne cliquée.
   *
   * <p>L'index reçu est celui de la ligne <strong>dans la page</strong> : c'est la page qu'il
   * faut parcourir. Indexer la liste filtrée entière faisait cocher, depuis la page 2, des
   * candidats restés en page 1.
   */
  basculerSelection(id: number, index: number, event: MouseEvent): void {
    const page = this.candidaturesPage;
    if (event.shiftKey && this.dernierIndexClique !== null) {
      const [debut, fin] = [this.dernierIndexClique, index].sort((a, b) => a - b);
      for (let i = debut; i <= fin; i++) {
        if (page[i]) this.selection.add(page[i].id);
      }
    } else if (this.selection.has(id)) {
      this.selection.delete(id);
    } else {
      this.selection.add(id);
    }
    this.dernierIndexClique = index;
  }

  selectionnerAuClicDroit(id: number, index: number, event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.basculerSelection(id, index, event);
    const candidature = this.candidaturesPage.find((item) => item.id === id);
    if (candidature) this.menuContextuel = { x: event.clientX, y: event.clientY, candidature };
  }

  fermerMenuContextuel(): void { this.menuContextuel = null; }

  executerActionContextuelle(action: () => void): void {
    this.fermerMenuContextuel();
    action();
  }

  ouvrirDossierContextuel(): void {
    if (!this.menuContextuel) return;
    const candidature = this.menuContextuel.candidature;
    this.fermerMenuContextuel();
    this.voirDetail(candidature);
  }

  actionSuivanteContextuelle(): void {
    if (!this.menuContextuel) return;
    const action = this.actionSuivante(this.menuContextuel.candidature);
    this.fermerMenuContextuel();
    action?.action();
  }

  rejeterContextuel(): void {
    if (!this.menuContextuel) return;
    const candidature = this.menuContextuel.candidature;
    this.fermerMenuContextuel();
    this.rejeter(candidature);
  }

  @HostListener('document:click')
  fermerMenuAuClic(): void { this.fermerMenuContextuel(); }

  basculerToutSelectionner(): void {
    const page = this.candidaturesPage;
    if (this.toutesSelectionnees) {
      page.forEach((c) => this.selection.delete(c.id));
    } else {
      page.forEach((c) => this.selection.add(c.id));
    }
  }

  /** Sélectionne tous les dossiers filtrés, y compris ceux des autres pages — geste explicite. */
  selectionnerTousLesResultats(): void {
    this.candidaturesFiltrees.forEach((c) => this.selection.add(c.id));
  }

  /** Vrai quand la page entière est cochée mais que d'autres résultats restent hors page. */
  get peutEtendreSelection(): boolean {
    return this.toutesSelectionnees && this.selection.size < this.candidaturesFiltrees.length;
  }

  viderSelection(): void {
    this.selection.clear();
    this.dernierIndexClique = null;
  }

  /** Rejette en une fois tous les dossiers sélectionnés qui peuvent encore l'être, avec un motif commun. */
  rejeterSelection(): void {
    const cibles = this.candidatures.filter((c) => this.selection.has(c.id) && this.peutRejeter(c));
    if (cibles.length === 0) {
      this.notifications.erreur('Aucun dossier sélectionné ne peut être rejeté (déjà admis, déjà rejeté ou brouillon).');
      return;
    }
    this.confirmation.demanderSaisie({
      titre: cibles.length > 1 ? `Rejeter ${cibles.length} dossiers` : 'Rejeter ce dossier',
      message: 'Le même motif est conservé sur chaque dossier et chaque candidat en est informé.',
      libelleConfirmer: 'Rejeter',
      destructif: true,
      saisie: { libelle: 'Motif du rejet', obligatoire: true, placeholder: 'Ex. : pièces justificatives incomplètes' },
    }).subscribe((motif) => {
      if (motif) this.rejeterCibles(cibles, motif);
    });
  }

  private rejeterCibles(cibles: CandidatureResponse[], motif: string): void {
    this.rejetGroupeEnCours = true;
    forkJoin(cibles.map((c) => this.recrutementService.rejeterDossier(c.id, motif))).subscribe({
      next: () => {
        this.rejetGroupeEnCours = false;
        this.viderSelection();
        this.chargerCandidatures();
      },
      error: (err) => {
        this.rejetGroupeEnCours = false;
        this.notifications.erreur(err.error?.message || 'Échec du rejet groupé — certains dossiers ont peut-être tout de même été rejetés.');
        this.viderSelection();
        this.chargerCandidatures();
      },
    });
  }

  /** Ouvre une confirmation réutilisée pour les corrections individuelles ou groupées. */
  private demanderRetour(cibles: CandidatureResponse[], statutCible: string | null = null): void {
    this.retourCibles = cibles;
    this.retourStatutCible = statutCible;
    this.motDePasseRetour = '';
    this.retourErreur = '';
    this.retourConfirmationOpen = true;
  }

  fermerRetour(): void {
    if (this.actionEnCours || this.rejetGroupeEnCours) return;
    this.retourConfirmationOpen = false;
  }

  confirmerRetour(): void {
    if (!this.motDePasseRetour) {
      this.retourErreur = 'Saisissez votre mot de passe pour confirmer cette correction.';
      return;
    }
    const groupe = this.retourCibles.length > 1;
    if (groupe) this.rejetGroupeEnCours = true;
    else this.actionEnCours = true;
    const appels = this.retourCibles.map((c) => this.retourStatutCible
      ? this.recrutementService.reculerStatut(c.id, this.retourStatutCible, this.motDePasseRetour)
      : this.recrutementService.annulerDerniereAction(c.id, this.motDePasseRetour));
    forkJoin(appels).subscribe({
      next: () => {
        this.rejetGroupeEnCours = false;
        this.actionEnCours = false;
        this.retourConfirmationOpen = false;
        this.viderSelection();
        this.chargerCandidatures();
      },
      error: (err) => {
        this.rejetGroupeEnCours = false;
        this.actionEnCours = false;
        this.retourErreur = err.error?.message || 'La correction n’a pas pu être appliquée.';
      },
    });
  }

  annulerSelection(): void {
    const cibles = this.candidatures.filter((c) => this.selection.has(c.id) && this.peutAnnuler(c));
    if (cibles.length === 0) {
      this.notifications.erreur('Aucun dossier sélectionné ne peut être annulé.');
      return;
    }
    this.demanderRetour(cibles);
  }

  reculerSelectionVers(cible: string): void {
    const cibles = this.candidatures.filter((c) => this.selection.has(c.id) && this.peutReculerVers(c, cible));
    if (cibles.length === 0) {
      this.notifications.erreur('Aucun dossier sélectionné ne peut reculer vers ce statut.');
      return;
    }
    this.demanderRetour(cibles, cible);
  }

  annuler(c: CandidatureResponse): void {
    if (c.statutPrecedent) this.demanderRetour([c]);
  }

  private executer(obs: Observable<CandidatureResponse>): void {
    this.actionEnCours = true;
    obs.subscribe({
      next: () => {
        this.actionEnCours = false;
        this.chargerCandidatures();
      },
      error: (err) => {
        this.actionEnCours = false;
        this.notifications.erreur(err.error?.message || "Échec de l'action sur la candidature.");
      },
    });
  }

  validerPreselection(c: CandidatureResponse): void {
    this.executer(this.recrutementService.validerPreselection(c.id));
  }

  admettreTest(c: CandidatureResponse): void {
    this.executer(this.recrutementService.admettreTestAcademique(c.id));
  }

  admettreEntretien(c: CandidatureResponse): void {
    this.executer(this.recrutementService.admettreEntretien(c.id));
  }

  admettreFinal(c: CandidatureResponse): void {
    this.executer(this.recrutementService.admettreFinal(c.id));
  }

  /** Étape 6 de QMS-009 : ouvre l'accès étudiant d'un dossier définitivement admis. */
  finaliserInscription(c: CandidatureResponse): void {
    this.actionEnCours = true;
    this.recrutementService.finaliserInscription(c.id).subscribe({
      next: () => {
        this.actionEnCours = false;
        this.chargerCandidatures();
      },
      error: (err) => {
        this.actionEnCours = false;
        this.notifications.erreur(err.error?.message || "Échec de la finalisation de l'inscription.");
      },
    });
  }

  rejeter(c: CandidatureResponse): void {
    this.confirmation.demanderSaisie({
      titre: 'Rejeter ce dossier',
      message: `Dossier ${c.numeroDossier} — ${c.candidatPrenom} ${c.candidatNom}.\nLe motif est conservé au dossier et le candidat en est informé.`,
      libelleConfirmer: 'Rejeter le dossier',
      destructif: true,
      saisie: { libelle: 'Motif du rejet', obligatoire: true, placeholder: 'Ex. : pièces justificatives incomplètes' },
    }).subscribe((motif) => {
      if (!motif) return;
      this.actionEnCours = true;
      this.recrutementService.rejeterDossier(c.id, motif).subscribe({
        next: () => {
          this.actionEnCours = false;
          this.chargerCandidatures();
        },
        error: (err) => {
          this.actionEnCours = false;
          this.notifications.erreur(err.error?.message || 'Échec du rejet de la candidature.');
        },
      });
    });
  }

  exporter(colonnes: string[]): void {
    this.exporting = true;
    this.recrutementService.exporterCandidatures(this.filtreExport, colonnes).subscribe({
      next: (blob) => {
        downloadBlob(blob, `candidatures_recrutement_${new Date().toISOString().slice(0, 10)}.xlsx`);
        this.exporting = false;
      },
      error: () => {
        this.exporting = false;
        this.notifications.erreur("Échec de l'export Excel.");
      },
    });
  }

  relancerDossiersIncomplets(): void {
    if (this.nombreDossiersIncomplets === 0) return;
    this.relanceEnCours = true;
    this.relanceMessage = '';
    this.recrutementService.sendReminder().subscribe({
      next: (resultat: ResultatRelance) => {
        this.relanceEnCours = false;
        this.relanceMessage = resultat.echecs.length === 0
          ? `${resultat.envoyes} relance(s) envoyée(s) avec succès.`
          : `${resultat.envoyes} relance(s) envoyée(s), ${resultat.echecs.length} échec(s) : ${resultat.echecs.join(', ')}.`;
      },
      error: () => {
        this.relanceEnCours = false;
        this.relanceMessage = 'Échec de la relance des dossiers incomplets.';
      },
    });
  }

  /** Télécharge les documents (pièces justificatives) des candidatures actuellement filtrées par statut, en .zip. */
  exporterDocuments(): void {
    this.exportingDocuments = true;
    this.recrutementService.exporterDocuments(this.filtreExport).subscribe({
      next: (blob) => {
        downloadBlob(blob, `documents_candidatures_${new Date().toISOString().slice(0, 10)}.zip`);
        this.exportingDocuments = false;
      },
      error: (err) => {
        this.exportingDocuments = false;
        this.notifications.erreur(err.status === 400 ? "Aucun document à exporter pour cette sélection." : "Échec de l'export des documents.");
      },
    });
  }

  /** Rapport qualité (demande responsable admission) : nombre + liste nominative des candidats par étape. */
  genererRapportQualite(): void {
    this.generatingRapport = true;
    this.recrutementService.genererRapportQualiteRecrutement().subscribe({
      next: (blob) => {
        downloadBlob(blob, `rapport_qualite_recrutement_${new Date().toISOString().slice(0, 10)}.pdf`);
        this.generatingRapport = false;
      },
      error: () => {
        this.generatingRapport = false;
        this.notifications.erreur('Échec de la génération du rapport qualité.');
      },
    });
  }

  /** Rapport de synthèse (indicateurs + répartitions) — distinct de l'export ligne par ligne et du rapport qualité ci-dessus. */
  genererRapport(format: FormatRapport): void {
    this.generatingReport = true;
    this.recrutementService.genererRapportCandidatures(format).subscribe({
      next: (blob) => {
        downloadBlob(blob, `rapport_candidatures_${new Date().toISOString().slice(0, 10)}.${format}`);
        this.generatingReport = false;
      },
      error: () => {
        this.generatingReport = false;
        this.notifications.erreur('Échec de la génération du rapport.');
      },
    });
  }
}
