import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { NavbarComponent } from './navbar.component';
import { ContexteNavbarService } from './contexte-navbar.service';
import { Auth } from '../../../core/auth/services/authService';
import { TypeInscription } from '../../../core/auth/models/user.model';

/**
 * Le navbar public est partagé par tous les publics : candidats étudiants, écoles,
 * entreprises, candidats formateurs. Onglet propre, libellé sous le nom et menu du
 * cercle de profil suivent le type du compte connecté — retour de recette : une
 * école ou une entreprise y retrouvait les onglets et le menu des admissions
 * étudiantes (« Sessions ouvertes », « Mes candidatures »).
 */
describe('NavbarComponent', () => {
  let fixture: ComponentFixture<NavbarComponent>;
  let component: NavbarComponent;
  let auth: Auth;

  const connecter = (roles: string[], typeInscription?: TypeInscription) =>
    auth.currentUser.set({ id: 1, username: 'compte_test', email: 'compte@inpg.sn', roles, typeInscription });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NavbarComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    auth = TestBed.inject(Auth);
    fixture = TestBed.createComponent(NavbarComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => localStorage.removeItem('token'));

  /** Ouvre le menu du cercle de profil et renvoie ses liens (sans « Déconnexion », qui n'a pas d'href). */
  function liensDuMenu(): string[] {
    localStorage.setItem('token', 'jeton-test');
    fixture.detectChanges();
    fixture.nativeElement.querySelector('#profile-btn').click();
    fixture.detectChanges();
    const liens = fixture.nativeElement.querySelectorAll('#profile-dropdown a[href]') as NodeListOf<HTMLAnchorElement>;
    return Array.from(liens).map((a) => a.getAttribute('href') ?? '');
  }

  function ongletsDuCentre(): string[] {
    const liens = fixture.nativeElement.querySelectorAll('nav.md\\:flex a') as NodeListOf<HTMLAnchorElement>;
    return Array.from(liens).map((a) => (a.textContent ?? '').trim());
  }

  describe('onglet propre à chaque public', () => {
    it('candidat/étudiant : Sessions ouvertes', () => {
      connecter([], 'CANDIDAT');
      expect(component.ongletSpecifique).toEqual({ libelle: 'Sessions ouvertes', lien: '/recrutement/sessions' });
    });

    it('école : Accréditation, même sans avoir rien soumis', () => {
      connecter([], 'ECOLE');
      expect(component.ongletSpecifique).toEqual({ libelle: 'Accréditation', lien: '/accreditation' });
    });

    it('entreprise : Espace entreprise, même sans avoir rien soumis', () => {
      connecter([], 'ENTREPRISE');
      expect(component.ongletSpecifique).toEqual({ libelle: 'Espace entreprise', lien: '/espace-entreprise' });
    });

    it('candidat formateur : Offres de recrutement', () => {
      connecter(['ROLE_CANDIDAT_FORMATEUR']);
      expect(component.ongletSpecifique).toEqual({ libelle: 'Offres de recrutement', lien: '/offres-formateurs' });
    });

    it('visiteur : l\'onglet suit la vitrine consultée (offres de recrutement formateur)', () => {
      TestBed.inject(ContexteNavbarService).contexte.set('formateur');
      expect(component.ongletSpecifique.libelle).toBe('Offres de recrutement');
    });

    it('le type du compte prime sur la page : une école qui consulte les sessions garde son onglet', () => {
      connecter([], 'ECOLE');
      TestBed.inject(ContexteNavbarService).contexte.set('etudiant');
      expect(component.ongletSpecifique.libelle).toBe('Accréditation');
    });

    it('le navbar garde les mêmes onglets communs pour tous', () => {
      connecter([], 'ENTREPRISE');
      fixture.detectChanges();
      expect(ongletsDuCentre()).toEqual(['Institut', 'Formations', 'Espace entreprise', 'Contact']);
    });
  });

  describe('libellé sous le nom', () => {
    it('suit le type du compte tant qu\'aucun rôle métier n\'existe', () => {
      connecter([], 'ECOLE');
      expect(component.getUserRoleLabel()).toBe('ÉCOLE DE FORMATION');
      connecter([], 'ENTREPRISE');
      expect(component.getUserRoleLabel()).toBe('ENTREPRISE PARTENAIRE');
      connecter([], 'FORMATEUR');
      expect(component.getUserRoleLabel()).toBe('CANDIDAT FORMATEUR');
    });

    it('reste celui du rôle pour les autres comptes', () => {
      connecter(['ROLE_CANDIDAT']);
      expect(component.getUserRoleLabel()).toBe('CANDIDAT');
      connecter(['ROLE_RESPONSABLE_ADMISSION']);
      expect(component.getUserRoleLabel()).toBe('RESPONSABLE ADMISSION');
    });
  });

  describe('cloche de notifications', () => {
    const cloche = () => fixture.nativeElement.querySelector('#notif-btn');

    it("absente pour une école, une entreprise ou un candidat formateur : c'est celle des candidats", () => {
      localStorage.setItem('token', 'jeton-test');
      for (const type of ['ECOLE', 'ENTREPRISE', 'FORMATEUR'] as const) {
        connecter([], type);
        fixture.detectChanges();
        expect(cloche()).toBeNull();
      }
    });

    it('présente pour un candidat étudiant', () => {
      localStorage.setItem('token', 'jeton-test');
      connecter(['ROLE_CANDIDAT'], 'CANDIDAT');
      fixture.detectChanges();
      expect(cloche()).not.toBeNull();
    });
  });

  describe('menu du cercle de profil', () => {
    it('école : son espace et son profil, pas « Mes candidatures »', () => {
      connecter([], 'ECOLE');
      expect(liensDuMenu()).toEqual(['/accreditation', '/accreditation/mon-profil']);
    });

    it('entreprise : son espace, sa messagerie et son profil', () => {
      connecter([], 'ENTREPRISE');
      expect(liensDuMenu()).toEqual([
        '/espace-entreprise',
        '/espace-entreprise/messagerie',
        '/espace-entreprise/mon-profil',
      ]);
    });

    it('candidat formateur : sa candidature et son profil', () => {
      connecter([], 'FORMATEUR');
      expect(liensDuMenu()).toEqual(['/candidature-formateur', '/candidature-formateur/mon-profil']);
    });

    it('candidat étudiant : tableau de bord et candidatures, comme avant', () => {
      connecter(['ROLE_CANDIDAT'], 'CANDIDAT');
      expect(liensDuMenu()).toEqual(['/recrutement/catalogue', '/recrutement/sessions']);
    });

    it('un étudiant qui consulte les offres formateur garde SON menu (le compte prime sur la page)', () => {
      connecter(['ROLE_CANDIDAT'], 'CANDIDAT');
      TestBed.inject(ContexteNavbarService).contexte.set('formateur');
      expect(liensDuMenu()).toEqual(['/recrutement/catalogue', '/recrutement/sessions']);
    });
  });
});
