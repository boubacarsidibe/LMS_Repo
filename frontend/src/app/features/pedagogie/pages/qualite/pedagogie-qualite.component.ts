import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { PedagogieService, Qualite } from '../../services/pedagogie.service';

/**
 * Suivi qualité de l'enseignement (TDR §3.7).
 *
 * <p>Les avis des étudiants existaient déjà, mais seul le formateur concerné voyait
 * les siens : personne ne disposait d'une vue d'ensemble, alors que c'est elle qui
 * permet de repérer un cours en difficulté. Le serveur trie les cours les moins bien
 * notés en tête — un classement alphabétique aurait noyé le cours à problème au
 * milieu des autres.
 */
@Component({
  selector: 'app-pedagogie-qualite',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './pedagogie-qualite.component.html',
})
export class PedagogieQualiteComponent implements OnInit {
  private pedagogie = inject(PedagogieService);

  chargement = true;
  qualite: Qualite | null = null;

  ngOnInit(): void {
    this.pedagogie.getQualite().subscribe({
      next: (qualite) => {
        this.qualite = qualite;
        this.chargement = false;
      },
      error: () => (this.chargement = false),
    });
  }

  /** Un cours au plus égal à 3 sur 5 appelle une réaction. */
  aSurveiller(moyenne: number | null): boolean {
    return moyenne !== null && moyenne <= 3;
  }
}
