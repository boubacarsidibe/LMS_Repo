import { Injectable, signal } from '@angular/core';

/**
 * Contexte affiché par le lien central du navbar partagé (app-navbar) :
 * « Sessions ouvertes » (étudiant) ou « Offres de recrutement » (formateur
 * candidat). Porté par un service plutôt qu'un simple @Input() par page,
 * parce que /institut et /contact sont des destinations COMMUNES aux deux
 * parcours (mêmes routes, atteintes depuis l'un ou l'autre) : un @Input()
 * fixé route par route aurait remis « Sessions ouvertes » à un formateur
 * candidat cliquant sur « Institut » depuis la vitrine des offres, aussi
 * absurde que l'inverse pour un étudiant. Un signal partagé, mis à jour par
 * les pages qui SAVENT dans quel parcours elles sont (offres-publiques,
 * catalogue/sessions/candidature/suivi étudiants), conserve la bonne valeur
 * sur les pages neutres entre les deux.
 */
@Injectable({ providedIn: 'root' })
export class ContexteNavbarService {
  readonly contexte = signal<'etudiant' | 'formateur'>('etudiant');
}
