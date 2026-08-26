import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MediaProtegeService } from '../../../espace-etudiants/services/media-protege.service';
import { PedagogieService, Qualite, SyntheseEnqueteSuivi } from '../../services/pedagogie.service';

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
  imports: [CommonModule, FormsModule],
  templateUrl: './pedagogie-qualite.component.html',
})
export class PedagogieQualiteComponent implements OnInit {
  private pedagogie = inject(PedagogieService);
  private mediaProtege = inject(MediaProtegeService);

  chargement = true;
  qualite: Qualite | null = null;

  enquetes: SyntheseEnqueteSuivi[] = [];
  chargementEnquetes = true;
  afficherFormulaireEnquete = false;
  nouveauTitre = '';
  nouvelleDescription = '';
  envoiEnCours = false;
  erreurEnquete = '';

  ngOnInit(): void {
    this.pedagogie.getQualite().subscribe({
      next: (qualite) => {
        this.qualite = qualite;
        this.chargement = false;
      },
      error: () => (this.chargement = false),
    });
    this.chargerEnquetes();
  }

  private chargerEnquetes(): void {
    this.pedagogie.getEnquetesSuivi().subscribe({
      next: (enquetes) => {
        this.enquetes = enquetes;
        this.chargementEnquetes = false;
      },
      error: () => (this.chargementEnquetes = false),
    });
  }

  /** Un cours au plus égal à 3 sur 5 appelle une réaction. */
  aSurveiller(moyenne: number | null): boolean {
    return moyenne !== null && moyenne <= 3;
  }

  telechargerRapport(): void {
    this.mediaProtege.ouvrir(this.pedagogie.urlRapportQualite());
  }

  lancerEnquete(): void {
    if (!this.nouveauTitre.trim()) {
      return;
    }
    this.envoiEnCours = true;
    this.erreurEnquete = '';
    this.pedagogie.lancerEnqueteSuivi(this.nouveauTitre.trim(), this.nouvelleDescription.trim() || null).subscribe({
      next: (enquete) => {
        this.enquetes = [enquete, ...this.enquetes];
        this.nouveauTitre = '';
        this.nouvelleDescription = '';
        this.afficherFormulaireEnquete = false;
        this.envoiEnCours = false;
      },
      error: (e) => {
        this.envoiEnCours = false;
        this.erreurEnquete = e?.error?.message || "Le lancement de l'enquête a échoué.";
      },
    });
  }

  cloturerEnquete(enquete: SyntheseEnqueteSuivi): void {
    this.pedagogie.cloturerEnqueteSuivi(enquete.enqueteId).subscribe({
      next: (misAJour) => {
        this.enquetes = this.enquetes.map((e) => (e.enqueteId === misAJour.enqueteId ? misAJour : e));
      },
      error: () => undefined,
    });
  }
}
