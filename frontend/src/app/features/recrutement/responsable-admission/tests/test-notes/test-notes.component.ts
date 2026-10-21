import { CommonModule } from '@angular/common';
import { Component, HostListener, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import {
  RecrutementService,
  TestAcademiqueResponse,
} from '../../../../../core/recrutement/services/recrutement.service';
import { ConfirmationService } from '../../../../../shared/components/ui/confirmation.service';

interface LigneNote extends TestAcademiqueResponse {
  numeroDossierAffiche: string;
  candidat: string;
}

@Component({
  selector: 'app-test-notes',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './test-notes.component.html',
})
export class TestNotesComponent implements OnInit {
  lignes: LigneNote[] = [];
  original = new Map<number, number | null>();
  recherche = '';
  manquantes = false;
  loading = true;
  saving = false;
  error = '';
  succes = '';
  nomTest = '';
  session = '';
  maximum = 20;

  constructor(
    private route: ActivatedRoute,
    private service: RecrutementService,
    private confirmation: ConfirmationService,
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    const testNavigue = history.state?.test as TestAcademiqueResponse | undefined;
    if (testNavigue?.id === id) {
      this.initialiserEntete(testNavigue);
      this.charger(testNavigue.groupeEpreuveId, testNavigue);
      return;
    }

    this.service.getTest(id).subscribe({
      next: test => {
        this.initialiserEntete(test);
        this.charger(test.groupeEpreuveId, test);
      },
      error: () => {
        this.error = 'Test introuvable.';
        this.loading = false;
      },
    });
  }

  private initialiserEntete(test: TestAcademiqueResponse): void {
    this.nomTest = test.nomEpreuve;
    this.session = test.sessionCandidatureLibelle ?? '';
  }

  private charger(groupeId: string, reference: TestAcademiqueResponse): void {
    if (!groupeId) {
      this.appliquerLignes([reference]);
      this.error = 'Ce test ancien ne possede pas encore de groupe de notation.';
      return;
    }

    this.service.getNotesGroupeTest(groupeId).subscribe({
      next: lignes => this.appliquerLignes(lignes),
      error: e => {
        this.appliquerLignes([reference]);
        this.error = e.error?.message || 'Impossible de charger la liste complete des candidats.';
      },
    });
  }

  private appliquerLignes(lignes: TestAcademiqueResponse[]): void {
    this.lignes = lignes.map(ligne => Object.assign(ligne, {
      numeroDossierAffiche: ligne.numeroDossier || '-',
      candidat: `${ligne.candidatPrenom || ''} ${ligne.candidatNom || ''}`.trim()
        || 'Candidat non disponible',
    }));
    this.original.clear();
    lignes.forEach(ligne => this.original.set(ligne.id, ligne.noteObtenue));
    this.loading = false;
  }

  get modifie(): boolean {
    return this.lignes.some(ligne => ligne.noteObtenue !== this.original.get(ligne.id));
  }

  get affichees(): LigneNote[] {
    const recherche = this.recherche.toLocaleLowerCase().trim();
    return this.lignes
      .filter(ligne => !this.manquantes || ligne.noteObtenue == null)
      .filter(ligne => !recherche
        || `${ligne.candidat} ${ligne.numeroDossierAffiche}`.toLocaleLowerCase().includes(recherche));
  }

  get nombreSaisies(): number {
    return this.lignes.filter(ligne => ligne.noteObtenue != null).length;
  }

  enregistrer(): void {
    const changes: Record<number, number | null> = {};
    this.lignes
      .filter(ligne => ligne.noteObtenue !== this.original.get(ligne.id))
      .forEach(ligne => changes[ligne.id] = ligne.noteObtenue);
    if (!Object.keys(changes).length || !this.lignes[0]?.groupeEpreuveId) return;

    this.saving = true;
    this.error = '';
    this.succes = '';
    this.service.enregistrerNotesGroupeTest(this.lignes[0].groupeEpreuveId, changes).subscribe({
      next: lignes => {
        this.appliquerLignes(lignes);
        this.saving = false;
        this.succes = 'Notes enregistrees.';
      },
      error: e => {
        this.saving = false;
        this.error = e.error?.message || 'Enregistrement impossible. Vos saisies sont conservees.';
      },
    });
  }

  annuler(): void {
    const revenir = () => this.lignes.forEach(ligne => ligne.noteObtenue = this.original.get(ligne.id) ?? null);
    if (!this.modifie) {
      revenir();
      return;
    }
    this.confirmation.demander({
      titre: 'Annuler les modifications',
      message: 'Les notes saisies et non enregistrées seront perdues.',
      libelleConfirmer: 'Annuler mes modifications',
      libelleAnnuler: 'Continuer la saisie',
      destructif: true,
    }).subscribe((confirme) => {
      if (confirme) revenir();
    });
  }

  @HostListener('window:beforeunload', ['$event'])
  quitter(event: BeforeUnloadEvent): void {
    if (this.modifie) event.preventDefault();
  }
}
