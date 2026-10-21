import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { LandingComponent } from './landing.component';
import { Auth } from '../../../../core/auth/services/authService';
import { TypeInscription } from '../../../../core/auth/models/user.model';

/**
 * Les boutons de l'accueil parlaient aux seuls candidats étudiants : une école, une
 * entreprise ou un candidat formateur connecté n'y trouvait rien qui mène à son
 * espace (retour de recette). Le héros propose désormais, selon le type du compte,
 * les actions qui le concernent.
 */
describe('LandingComponent (accueil)', () => {
  let fixture: ComponentFixture<LandingComponent>;
  let auth: Auth;

  const connecter = (roles: string[], typeInscription?: TypeInscription) =>
    auth.currentUser.set({ id: 1, username: 'compte_test', email: 'compte@inpg.sn', roles, typeInscription });

  /** Liens (href → libellé) du bloc de boutons du héros. */
  function boutonsDuHero(): Record<string, string> {
    const liens = fixture.nativeElement.querySelectorAll('section:first-of-type a') as NodeListOf<HTMLAnchorElement>;
    const resultat: Record<string, string> = {};
    liens.forEach((a) => {
      const href = a.getAttribute('href');
      if (href) resultat[href] = (a.textContent ?? '').replace(/\s+/g, ' ').trim();
    });
    return resultat;
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LandingComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    auth = TestBed.inject(Auth);
    fixture = TestBed.createComponent(LandingComponent);
  });

  afterEach(() => localStorage.removeItem('token'));

  it('visiteur : les boutons des candidats (formations, postuler)', () => {
    fixture.detectChanges();
    const boutons = boutonsDuHero();
    expect(boutons['/recrutement/sessions']).toContain('Postuler');
    expect(Object.keys(boutons)).not.toContain('/accreditation');
  });

  it('école connectée, même sans rien avoir soumis : les boutons de son espace, pas « Postuler »', () => {
    connecter([], 'ECOLE');
    fixture.detectChanges();
    const boutons = boutonsDuHero();
    expect(boutons['/accreditation']).toContain("Ma demande d'accréditation");
    expect(boutons['/accreditation/rendez-vous']).toContain('Prendre rendez-vous');
    expect(Object.keys(boutons)).not.toContain('/recrutement/sessions');
    expect(fixture.nativeElement.textContent).toContain('Espace école de formation');
  });

  it('entreprise sans ROLE_ENTREPRISE (aucune demande encore soumise) : déposer une demande, rendez-vous, pas la messagerie', () => {
    // Compte tout juste inscrit : la fiche Entreprise n'existe en base qu'à la
    // première demande réellement soumise (EntrepriseController.soumettreDemande).
    // Avant ça, envoyer un message échoue en 403 — le raccourci reste masqué.
    connecter([], 'ENTREPRISE');
    fixture.detectChanges();
    const boutons = boutonsDuHero();
    expect(boutons['/espace-entreprise/nouvelle-demande']).toContain('Déposer une demande');
    expect(Object.keys(boutons)).not.toContain('/espace-entreprise/catalogue');
    expect(Object.keys(boutons)).not.toContain('/espace-entreprise/messagerie');
    expect(boutons['/espace-entreprise/rendez-vous']).toContain('Rendez-vous');
    expect(Object.keys(boutons)).not.toContain('/recrutement/sessions');
  });

  it('entreprise avec ROLE_ENTREPRISE (première demande déjà soumise) : la messagerie apparaît', () => {
    connecter(['ROLE_ENTREPRISE']);
    fixture.detectChanges();
    const boutons = boutonsDuHero();
    expect(boutons['/espace-entreprise/messagerie']).toContain('Messagerie');
  });

  it('candidat formateur : offres de recrutement et sa candidature', () => {
    connecter([], 'FORMATEUR');
    fixture.detectChanges();
    const boutons = boutonsDuHero();
    expect(boutons['/offres-formateurs']).toContain('Voir les offres de recrutement');
    expect(boutons['/candidature-formateur']).toContain('Ma candidature');
  });

  it('candidat étudiant connecté : les boutons habituels', () => {
    connecter(['ROLE_CANDIDAT'], 'CANDIDAT');
    fixture.detectChanges();
    expect(boutonsDuHero()['/recrutement/sessions']).toContain('Postuler');
  });

  it('jeton présent mais compte pas encore chargé : les boutons sont masqués, pas ceux des candidats affichés un instant', () => {
    localStorage.setItem('token', 'jeton-test');
    fixture.detectChanges();
    const rangee = fixture.nativeElement.querySelector('section:first-of-type .flex.flex-col.sm\\:flex-row') as HTMLElement;
    expect(rangee.classList.contains('invisible')).toBe(true);
  });
});
