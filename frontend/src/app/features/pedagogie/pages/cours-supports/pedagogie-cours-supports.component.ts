import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { PedagogieService, SupportDetail } from '../../services/pedagogie.service';
import { Cours } from '../../../espace-etudiants/models/cours';
import { ConfirmationService } from '../../../../shared/components/ui/confirmation.service';

/**
 * Supports d'un cours, côté responsable pédagogique : publication et retrait.
 * Ce sont exactement les supports que l'étudiant voit sur
 * /espace-etudiant/cours/:id.
 */
@Component({
  selector: 'app-pedagogie-cours-supports',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './pedagogie-cours-supports.component.html',
})
export class PedagogieCoursSupportsComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private pedagogie = inject(PedagogieService);
  private confirmation = inject(ConfirmationService);

  coursId = 0;
  cours: Cours | null = null;
  supports: SupportDetail[] = [];
  chargement = true;

  typeRessource = 'Cours';
  fichier: File | null = null;
  publication = false;
  erreur: string | null = null;

  typesRessource = ['Cours', 'TD', 'TP', 'Exercice', 'Video', 'Lien'];

  ngOnInit(): void {
    this.coursId = Number(this.route.snapshot.paramMap.get('id'));
    if (!this.coursId) {
      this.erreur = 'Cours introuvable.';
      this.chargement = false;
      return;
    }
    this.pedagogie.getCours().subscribe({
      next: (liste) => {
        this.cours = (liste || []).find((c) => c.id === this.coursId) || null;
        this.chargement = false;
      },
      error: () => (this.chargement = false),
    });
    this.chargerSupports();
  }

  private chargerSupports(): void {
    this.pedagogie.getSupports(this.coursId).subscribe({
      next: (data) => (this.supports = data || []),
      error: () => (this.supports = []),
    });
  }

  onFichierChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.fichier = input.files && input.files.length > 0 ? input.files[0] : null;
  }

  publier(): void {
    if (!this.fichier || this.publication) return;
    this.publication = true;
    this.erreur = null;

    this.pedagogie.publierSupport(this.coursId, this.typeRessource, this.fichier).subscribe({
      next: () => {
        this.publication = false;
        this.fichier = null;
        // Réinitialise le champ fichier : sans cela, republier le même fichier
        // ne déclenche pas de nouvel événement change.
        const input = document.getElementById('fichier') as HTMLInputElement | null;
        if (input) input.value = '';
        this.chargerSupports();
      },
      error: (err) => {
        this.publication = false;
        this.erreur = err?.error?.message || "La publication du support a échoué.";
      },
    });
  }

  supprimer(support: SupportDetail): void {
    this.confirmation.demander({
      titre: 'Retirer la ressource',
      message: `« ${support.titre} » sera retirée de ce cours.`,
      libelleConfirmer: 'Retirer',
      destructif: true,
    }).subscribe((confirme) => {
      if (!confirme) return;
      this.pedagogie.supprimerSupport(support.id).subscribe({
        next: () => this.chargerSupports(),
        error: () => (this.erreur = 'La suppression a échoué.'),
      });
    });
  }
}
