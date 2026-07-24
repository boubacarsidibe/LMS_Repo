import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { catchError, map, Observable, of } from 'rxjs';
import { AuthResponse } from '../models/auth-response';

@Injectable({
  providedIn: 'root',
})
export class Auth {
  constructor(
    private http: HttpClient
  ) { }
  private apiUrl = 'http://localhost:8080/api/auth';

  login(username: string, password: string):Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, { username, password });
  }

  checkToken(): Observable<boolean> {
    const token = localStorage.getItem('token');

    if (!token) {
      return of(false);
    }

    return this.http.get<boolean>(`${this.apiUrl}/verify`, { headers: { Authorization: `Bearer ${token}` } }).pipe(
      map(() => true),
      catchError(() => of(false)), catchError(() => {
        localStorage.removeItem('token');

        return of(false);
      }
      ))

  }
}
