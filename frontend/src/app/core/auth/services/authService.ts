import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { catchError, map, Observable, of, tap } from 'rxjs';
import { AuthResponse } from '../models/auth-response';
import { User } from '../models/user.model';

@Injectable({
  providedIn: 'root',
})
export class Auth {
  constructor(
    private http: HttpClient
  ) { }

  currentUser = signal<User | null>(null);
  private apiUrl = 'http://localhost:8088/api/auth';



  getToken(): string | null {
    return localStorage.getItem('token');
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, { email, password });
  }

  setSession(user: User, token: string): void {
    localStorage.setItem('token', token);
    this.currentUser.set(user);
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  // Fermer la session
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


  register(username: string, email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, { username, email, password });
  }

  verifyOtp(email: string, code: string): Observable<boolean> {
    const payload = { email, code };

    return this.http.post<{ message: string }>(`${this.apiUrl}/verify-otp`, payload).pipe(

      map(() => true),

      catchError((error) => {
        console.warn('Vérification OTP échouée :', error.error?.message || error.message);
        return of(false);
      })
    );
  }
  getOtp(email: string): Observable<boolean> {
    return this.http.post<boolean>(`${this.apiUrl}/send-otp`, { email });
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
      tap((user) => this.currentUser.set(user)), // Met à jour le signal si la requête réussit
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
}
