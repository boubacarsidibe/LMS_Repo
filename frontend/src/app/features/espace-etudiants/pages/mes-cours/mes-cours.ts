import { Component, ViewChild, ElementRef} from '@angular/core';

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
    {
      code: 'ECO-402',
      titre: 'Théorie et Pratique Économiques',
      nbEtudiants: 42,
      nbRessources: 12,
      progression: 68,
      couleurBandeau: 'bg-secondary-container'
    },
    {
      code: 'MGT-301',
      titre: 'Management des Organisations',
      nbEtudiants: 38,
      nbRessources: 8,
      progression: 45,
      couleurBandeau: 'bg-primary-fixed-dim'
    }
  ];
  ouvrirExplorateur() {
    this.fileInput.nativeElement.click();
  }

  onFichierSelectionne(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      console.log(`${input.files.length} fichier(s) sélectionné(s)`, input.files);
      // TODO : logique d'upload réelle vers le backend
    }
  }
}