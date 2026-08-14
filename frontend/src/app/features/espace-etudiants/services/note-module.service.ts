import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { NoteModule } from '../models/note-module';

@Injectable({ providedIn: 'root' })
export class NoteModuleService {
  private api = `${environment.apiUrl}/espace-etudiant`;

  constructor(private http: HttpClient) {}

  getByEtudiant(etudiantId: number): Observable<NoteModule[]> {
    return this.http.get<NoteModule[]>(`${this.api}/formateur/etudiants/${etudiantId}/notes`);
  }

  ajouter(etudiantId: number, coursId: number, module: string, note: number, commentaire: string): Observable<NoteModule> {
    return this.http.post<NoteModule>(`${this.api}/formateur/etudiants/${etudiantId}/notes`, {
      coursId,
      module,
      note,
      commentaire,
    });
  }

  supprimer(noteId: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/formateur/notes/${noteId}`);
  }
}
