import { Component, ViewChild, ElementRef } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

interface Cours {
  code: string;
  titre: string;
  nbEtudiants: number;
  nbRessources: number;
  progression: number;
  couleurBandeau: string;
}

@Component({
  selector: 'app-mes-cours',
  standalone: false,
  templateUrl: './mes-cours.html',
  styleUrl: './mes-cours.css',
})
export class MesCours {
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  cours: Cours[] = [
    { code: 'ECO-402', titre: 'Théorie et Pratique Économiques', nbEtudiants: 42, nbRessources: 12, progression: 68, couleurBandeau: 'bg-secondary-container' },
    { code: 'MGT-301', titre: 'Management des Organisations', nbEtudiants: 38, nbRessources: 8, progression: 45, couleurBandeau: 'bg-primary-fixed-dim' }
  ];

  depotForm: FormGroup;
  fichierSelectionne: File | null = null;

  constructor(private fb: FormBuilder) {
    this.depotForm = this.fb.group({
      typeRessource: ['pdf', Validators.required],
      coursCible: ['', Validators.required],
      datePublication: ['', Validators.required]
    });
  }

  ouvrirExplorateur() {
    this.fileInput.nativeElement.click();
  }

  onFichierSelectionne(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.fichierSelectionne = input.files[0];
    }
  }

  onSubmit() {
    if (this.depotForm.invalid || !this.fichierSelectionne) {
      this.depotForm.markAllAsTouched();
      return;
    }
    const payload = {
      ...this.depotForm.value,
      fichier: this.fichierSelectionne
    };
    console.log('Ressource à publier :', payload);
    // TODO : appel API réel vers le backend une fois l'endpoint disponible
  }
    recherche: string = '';
    semestreSelectionne: string = 'Semestre 1 - 2024';
    cohorteSelectionnee: string = 'Toutes les Cohortes';

    get coursFiltres(): Cours[] {
      return this.cours.filter(c => {
        const correspondRecherche = 
          c.titre.toLowerCase().includes(this.recherche.toLowerCase()) ||
          c.code.toLowerCase().includes(this.recherche.toLowerCase());
        return correspondRecherche;
      });
}
}