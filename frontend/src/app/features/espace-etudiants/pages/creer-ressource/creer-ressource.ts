import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Cours } from '../../models/cours';
import { CoursService } from '../../services/cours.service';

@Component({
  selector: 'app-creer-ressource',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './creer-ressource.html',
})
export class CreerRessource implements OnInit {
  cours: Cours[] = [];
  coursId: number | null = null;
  chargement = true;

  constructor(
    private coursService: CoursService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.coursService.getAll().subscribe({
      next: (cours) => {
        this.cours = cours || [];
        this.chargement = false;
      },
      error: () => {
        this.cours = [];
        this.chargement = false;
      },
    });
  }

  continuer(): void {
    if (!this.coursId) return;

    this.router.navigate(['/espace-etudiant/formateur/mes-cours'], {
      queryParams: { cours: this.coursId },
    });
  }
}
