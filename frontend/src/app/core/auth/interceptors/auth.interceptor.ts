import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { Auth } from '../services/authService';
import { environment } from '../../../../environments/environment';

/**
 * Intercepteur fonctionnel Angular 21 qui attache le token JWT à toutes les
 * requêtes destinées à l'API INPG, et gère la session expirée/invalide de façon centralisée.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(Auth);
  const router = inject(Router);
  const token = auth.getToken();

  const apiUrl = new URL(environment.apiUrl, window.location.origin);
  const requestUrl = new URL(req.url, window.location.origin);
  const apiPath = apiUrl.pathname.replace(/\/$/, '');
  const cibleApi = requestUrl.origin === apiUrl.origin
    && (requestUrl.pathname === apiPath || requestUrl.pathname.startsWith(`${apiPath}/`));

  const authReq = token && cibleApi
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // On ne réagit que si un token avait été envoyé : un 401 sans token
      // correspond à un échec de connexion normal (mauvais mot de passe), que
      // chaque formulaire gère déjà lui-même — pas à une session expirée.
      //
      // Le back renvoie 401 (et non 403) dès que le jeton est absent, illisible
      // ou expiré (voir SecurityConfig.authenticationEntryPoint). Un 403, lui,
      // reste un vrai refus de droit sur un compte valide : on le laisse aux
      // écrans, qui affichent leur propre message.
      if (error.status === 401 && token) {
        auth.clearSession();
        if (!router.url.startsWith('/auth/login')) {
          router.navigate(['/auth/login'], { queryParams: { expired: 1 } });
        }
      }
      return throwError(() => error);
    })
  );
};
