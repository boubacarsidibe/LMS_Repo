import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Formation } from '../../models/formation';
import { FormationService } from '../../services/formation.service';
import { mesFormationsMock } from '../../data/formations.data';

@Component({
  selector: 'app-mes-formations',
  standalone: false,
  templateUrl: './mes-formations.html',
  styleUrl: './mes-formations.css',
})
export class MesFormations implements OnInit {
  formations: Formation[] = [];
  filtre: string = 'En cours';
  recherche: string = '';

  constructor(private formationService: FormationService, private router: Router) {}

  ngOnInit(): void {
    this.loadFormations();
  }

  private loadFormations(): void {
    this.formationService.getAll().subscribe({
      next: (data) => (this.formations = data || mesFormationsMock),
      error: () => (this.formations = mesFormationsMock),
    });
  }

  get formationsFiltres(): Formation[] {
    return this.formations.filter((f) =>
      `${f.intitule || ''} ${f.code || ''}`.toLowerCase().includes(this.recherche.toLowerCase())
    );
  }

  continuer(formation: Formation) {
    // navigation vers le catalogue / détail — adapter si une route dédiée existe
    this.router.navigate(['/recrutement/catalogue']);
  }
}
