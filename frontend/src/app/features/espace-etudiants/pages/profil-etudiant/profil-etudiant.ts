import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ETUDIANTS_MOCK, Etudiant } from '../../data/etudiants.mock';

@Component({
  selector: 'app-profil-etudiant',
  standalone: false,
  templateUrl: './profil-etudiant.html',
  styleUrl: './profil-etudiant.css',
})
export class ProfilEtudiant implements OnInit {
  etudiant: Etudiant | undefined;

  constructor(private route: ActivatedRoute) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.etudiant = ETUDIANTS_MOCK.find(e => e.id === id);
  }
}