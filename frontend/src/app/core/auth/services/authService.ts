import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { catchError, finalize, map, Observable, of, tap } from 'rxjs';
import { Router } from '@angular/router';
import { AuthResponse } from '../models/auth-response';
import { User } from '../models/user.model';
import { environment } from '../../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class Auth {
  constructor(
    private http: HttpClient
  ) { }

  private router = inject(Router);

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
   * URL de l'espace propre au rôle de l'utilisateur connecté, ou null s'il n'a
   * encore aucun rôle métier (compte tout juste créé). Utilisée par
   * redirectByRole pour la redirection post-connexion.
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
    } else if (roles.includes('ROLE_ETUDIANT')) {
      return '/espace-etudiant/dashboard';
    } else if (roles.includes('ROLE_FORMATEUR')) {
      return '/espace-etudiant/formateur/dashboard-formateur';
    } else if (roles.includes('ROLE_ALUMNI')) {
      return '/espace-alumni/mon-profil';
    }
    return null;
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

  register(username: string, email: string, password: string, typeInscription: 'CANDIDAT' | 'ECOLE' | 'ENTREPRISE' = 'CANDIDAT'): Observable<AuthResponse> {
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
      map(() => this.getToken() === token),
      catchError((error) => {
        if (error.status === 401 && this.getToken() === token) this.clearSession();
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
      map((user) => this.getToken() === token ? user : null),
      tap((user) => {
        if (user) this.currentUser.set(user);
      }),
      catchError(() => of(null))
    );
  }

  logout(): Observable<string> {
    const token = this.getToken();
    const clear = () => {
      if (this.getToken() === token) this.clearSession();
    };
    return this.http.post(`${this.apiUrl}/logout`, {}, { responseType: 'text' }).pipe(
      tap({ next: clear, error: clear }),
      finalize(clear),
    );
  }

  ChangePassword(currentPassword: string, newPassword: string): Observable<{ message: string }> {
    const token = this.getToken();
    return this.http.post<{ message: string }>(`${this.apiUrl}/change-password`, { currentPassword, newPassword }).pipe(
      tap(() => {
        if (this.getToken() !== token) return;
        this.clearSession();
        this.router.navigate(['/auth/login'], { queryParams: { passwordChanged: 1 } });
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
