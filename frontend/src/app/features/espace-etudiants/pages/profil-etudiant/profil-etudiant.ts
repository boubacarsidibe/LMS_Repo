import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Etudiant } from '../../data/etudiants.mock';
import { EtudiantService } from '../../services/etudiant.service';

@Component({
  selector: 'app-profil-etudiant',
  standalone: false,
  templateUrl: './profil-etudiant.html',
  styleUrl: './profil-etudiant.css',
})
export class ProfilEtudiant implements OnInit {
  etudiant: Etudiant | undefined;

  constructor(private route: ActivatedRoute, private etudiantService: EtudiantService) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.etudiantService.getById(id).subscribe({
        next: e => this.etudiant = e,
        error: () => this.etudiant = undefined
      });
    }
  }
}