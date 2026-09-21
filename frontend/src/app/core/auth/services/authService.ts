import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { catchError, map, Observable, of, tap } from 'rxjs';
import { AuthResponse } from '../models/auth-response';
import { ProfilCompte, TypeInscription, User } from '../models/user.model';
import { environment } from '../../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class Auth {
  constructor(
    private http: HttpClient
  ) { }

  currentUser = signal<User | null>(null);
  private apiUrl = `${environment.apiUrl}/auth`;

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  getUserByEmail(email: string): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/user`, { params: { email } });
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, { email, password });
  }

  setSession(authResponse: AuthResponse): void {
    localStorage.setItem('token', authResponse.token);
    if (authResponse.user) {
      this.currentUser.set(authResponse.user);
    }
  }

  /**
   * École, entreprise ou candidat formateur — ou null pour tout le reste
   * (candidat/étudiant, personnel INPG, visiteur).
   *
   * <p>Le rôle métier fait foi dès qu'il existe. Tant qu'il n'existe pas (rôle
   * accordé seulement à la première soumission), c'est le type choisi à
   * l'inscription qui parle : sans lui, une école qui s'est inscrite sans encore
   * rien déposer n'avait aucun rôle et se retrouvait traitée comme un candidat —
   * accueil, onglets et menu du profil tournés vers les admissions étudiantes.
   * Un compte qui porte déjà un autre rôle (étudiant, personnel…) n'est jamais
   * reclassé d'après son type.
   */
  getProfilCompte(): ProfilCompte | null {
    const roles = this.getUserRoles();
    if (roles.includes('ROLE_ECOLE_FORMATION')) return 'ecole';
    if (roles.includes('ROLE_ENTREPRISE')) return 'entreprise';
    if (roles.includes('ROLE_CANDIDAT_FORMATEUR')) return 'formateur';
    if (roles.length > 0) return null;
    switch (this.currentUser()?.typeInscription) {
      case 'ECOLE': return 'ecole';
      case 'ENTREPRISE': return 'entreprise';
      case 'FORMATEUR': return 'formateur';
      default: return null;
    }
  }

  /**
   * URL de l'espace propre à l'utilisateur connecté, ou null s'il n'a ni rôle
   * métier ni type d'inscription qui le désigne (candidat sans dossier).
   * Utilisée par redirectByRole pour la redirection post-connexion.
   */
  getHomeUrl(): string | null {
    const roles = this.getUserRoles();
    if (roles.includes('ROLE_RESPONSABLE_ADMISSION')) {
      return '/responsable-admission';
    } else if (roles.includes('ROLE_RESPONSABLE_PEDAGOGIQUE')) {
      return '/pedagogie';
    } else if (roles.includes('ROLE_ECOLE_FORMATION')) {
      return '/accreditation';
    } else if (roles.includes('ROLE_ENTREPRISE')) {
      return '/espace-entreprise';
    } else if (roles.includes('ROLE_EVALUATEUR_ACCREDITATION')) {
      return '/evaluateur-accreditation';
    } else if (roles.includes('ROLE_CANDIDAT')) {
      return '/recrutement/suivi';
    } else if (roles.includes('ROLE_CANDIDAT_FORMATEUR')) {
      return '/candidature-formateur';
    } else if (roles.includes('ROLE_ETUDIANT')) {
      return '/espace-etudiant/dashboard';
    } else if (roles.includes('ROLE_FORMATEUR')) {
      return '/espace-etudiant/formateur/dashboard-formateur';
    } else if (roles.includes('ROLE_ALUMNI')) {
      return '/espace-alumni/mon-profil';
    }
    // Aucun rôle métier encore : on suit le type d'inscription, comme le fait la
    // page d'inscription juste après la création du compte. Le formateur arrive
    // sur les offres — il doit en choisir une avant de pouvoir candidater.
    switch (this.getProfilCompte()) {
      case 'ecole': return '/accreditation';
      case 'entreprise': return '/espace-entreprise';
      case 'formateur': return '/offres-formateurs';
      default: return null;
    }
  }

  /** Redirige l'utilisateur vers sa page d'accueil selon son rôle */
  redirectByRole(router: any): void {
    router.navigate([this.getHomeUrl() ?? '/accueil']);
  }

  getUserRoles(): string[] {
    const user = this.currentUser();
    return (user as any)?.roles || [];
  }

  isResponsableAdmission(): boolean {
    return this.getUserRoles().includes('ROLE_RESPONSABLE_ADMISSION');
  }

  isFormateur(): boolean {
    return this.getUserRoles().includes('ROLE_FORMATEUR');
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  clearSession(): void {
    localStorage.removeItem('token');
    this.currentUser.set(null);
  }

  setToken(token: string | null): void {
    if (token) {
      localStorage.setItem('token', token);
    } else {
      localStorage.removeItem('token');
    }
  }

  register(username: string, email: string, password: string, typeInscription: TypeInscription = 'CANDIDAT'): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, { username, email, password, typeInscription })
      .pipe(
        tap((resp) => this.setSession(resp))
      );
  }

  getOtp(email: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/send-otp`, { email });
  }

  checkToken(): Observable<boolean> {
    const token = this.getToken();

    if (!token) {
      this.clearSession();
      return of(false);
    }

    return this.http.get(`${this.apiUrl}/verify`, {
      headers: { Authorization: `Bearer ${token}` }
    }).pipe(
      map(() => true),
      catchError(() => {
        this.clearSession();
        return of(false);
      })
    );
  }

  getCurrentUser(): Observable<User | null> {
    const token = this.getToken();

    if (!token) {
      return of(null);
    }

    return this.http.get<User>(`${this.apiUrl}/current-user`, {
      headers: { Authorization: `Bearer ${token}` }
    }).pipe(
      tap((user) => this.currentUser.set(user)),
      catchError(() => of(null))
    );
  }

  logout(): Observable<string> {
    return this.http.post(`${this.apiUrl}/logout`, {}, { responseType: 'text' }).pipe(
      tap(() => {
        this.currentUser.set(null);
        this.clearSession();
      }),
    );
  }

  ChangePassword(currentPassword: string, newPassword: string): Observable<string> {
    return this.http.post(`${this.apiUrl}/change-password`, { currentPassword, newPassword }, { responseType: 'text' }).pipe(
      tap(() => {
        console.log('Mot de passe changé avec succès.');
      }),
      catchError((error) => {
        console.error('Erreur lors du changement de mot de passe :', error.error?.message || error.message);
        throw error;
      })
    );
  }

  isCandidate(): boolean {
    const user = this.currentUser();
    if (!user) return false;
    return user.roles.includes('ROLE_CANDIDAT');
  }

  createCandiadat(nationnalite: string, dateDeNaissance: string) {
    const user = this.getCurrentUser();
    if (!user) return null;
    return {
      dateDeNaissance: dateDeNaissance,
      nationalite: nationnalite,
      user: user
    };
  }
  resetPassword(email: string, code: string, newPassword: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/reset-password`, {
      email,
      code,
      newPassword,
    });
  }
}
