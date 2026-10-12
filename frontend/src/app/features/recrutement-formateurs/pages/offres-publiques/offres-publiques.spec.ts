import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { Auth } from '../../../../core/auth/services/authService';
import { FooterComponent } from '../../../../shared/components/footer/footer.component';
import { NavbarComponent } from '../../../../shared/components/navbar/navbar.component';
import { OffreFormateur } from '../../models/recrutement-formateur.models';
import { RecrutementFormateurService } from '../../services/recrutement-formateur.service';
import { OffresPubliquesComponent } from './offres-publiques';

function offre(id: number, extra: Partial<OffreFormateur> = {}): OffreFormateur {
  return {
    id,
    intitule: `Poste ${id}`,
    domaine: null,
    descriptionMissions: null,
    profilRecherche: null,
    typeContrat: 'VACATAIRE',
    lieu: null,
    nbPostes: 1,
    formationId: null,
    intituleFormation: null,
    datePublication: '2026-10-01',
    dateLimiteCandidature: null,
    statut: 'PUBLIEE',
    dateCreation: '2026-10-01T10:00:00',
    nombreCandidatures: 0,
    fichePosteNom: null,
    ...extra,
  };
}

describe('OffresPubliquesComponent', () => {
  let fixture: ComponentFixture<OffresPubliquesComponent>;
  const el = () => fixture.nativeElement as HTMLElement;
  const cartes = () => Array.from(el().querySelectorAll('main ul.grid > li'));
  const titres = () => cartes().map((c) => c.querySelector('h2')!.textContent!.trim());
  const texte = () => el().textContent!.replace(/\s+/g, ' ');

  function afficher(offres: Observable<OffreFormateur[]>, connecte = true): void {
    TestBed.configureTestingModule({
      imports: [OffresPubliquesComponent],
      providers: [
        provideRouter([]),
        {
          provide: RecrutementFormateurService,
          useValue: { getOffres: () => offres, urlFichePoste: (id: number) => `/api/fiche/${id}` },
        },
        { provide: Auth, useValue: { isLoggedIn: () => connecte } },
      ],
    });
    TestBed.overrideComponent(OffresPubliquesComponent, {
      remove: { imports: [NavbarComponent, FooterComponent] },
      add: { schemas: [NO_ERRORS_SCHEMA] },
    });
    fixture = TestBed.createComponent(OffresPubliquesComponent);
    fixture.detectChanges();
  }

  it("liste les offres ouvertes — sans date limite ou à échéance future — et masque celles dont l'échéance est passée", () => {
    afficher(
      of([
        offre(1),
        offre(2, { dateLimiteCandidature: '2099-01-01' }),
        offre(3, { dateLimiteCandidature: '2020-01-01' }),
      ])
    );
    expect(titres()).toEqual(['Poste 1', 'Poste 2']);
  });

  it("annonce le délai : « sans date limite » ou la date en français avec les jours restants", () => {
    afficher(of([offre(1), offre(2, { dateLimiteCandidature: '2099-12-12' })]));
    const [sans, avec] = cartes().map((c) => c.textContent!.replace(/\s+/g, ' '));
    expect(sans).toContain('sans date limite');
    expect(avec).toContain('Clôture le 12 décembre 2099');
    expect(avec).toMatch(/\d+ jours restants/);
  });

  it("propose la fiche de poste uniquement aux offres qui en ont une, vers le bon lien", () => {
    afficher(of([offre(1), offre(2, { fichePosteNom: 'fiche.pdf' })]));
    const [sans, avec] = cartes();
    expect(sans.textContent).not.toContain('Fiche de poste');
    const lien = avec.querySelector('a[target="_blank"]') as HTMLAnchorElement;
    expect(lien.textContent).toContain('Fiche de poste (PDF)');
    expect(lien.getAttribute('href')).toBe('/api/fiche/2');
  });

  it('invite un visiteur non connecté à se connecter, pas un utilisateur connecté', () => {
    afficher(of([offre(1)]), false);
    expect(texte()).toContain('Un compte est nécessaire pour déposer une candidature');
    expect(el().querySelector('a[href="/auth/login"]')).not.toBeNull();
    fixture.destroy();
    TestBed.resetTestingModule();

    afficher(of([offre(1)]), true);
    expect(texte()).not.toContain('Un compte est nécessaire');
  });

  it("dit clairement qu'il n'y a aucune offre ouverte", () => {
    afficher(of([]));
    expect(cartes()).toEqual([]);
    expect(texte()).toContain("Aucune offre de recrutement n'est ouverte pour le moment");
  });

  it("signale une erreur de chargement sans l'afficher comme « aucune offre »", () => {
    afficher(throwError(() => new Error('réseau')));
    expect(el().querySelector('[role="alert"]')!.textContent).toContain('Impossible de charger les offres');
    expect(texte()).not.toContain("Aucune offre de recrutement n'est ouverte");
  });
});
