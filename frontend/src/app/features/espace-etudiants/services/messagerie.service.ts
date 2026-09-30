import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Contactable, Conversation, FilMessagerie, MessagePrive } from '../models/messagerie.models';

@Injectable({ providedIn: 'root' })
export class MessagerieService {
  private api = `${environment.apiUrl}/espace-etudiant/messagerie`;

  constructor(private http: HttpClient) {}

  getContactables(): Observable<Contactable[]> {
    return this.http.get<Contactable[]>(`${this.api}/contactables`);
  }

  getConversations(): Observable<Conversation[]> {
    return this.http.get<Conversation[]>(`${this.api}/conversations`);
  }

  demarrerConversation(destinataireId: number, contenu: string): Observable<Conversation> {
    return this.http.post<Conversation>(`${this.api}/conversations`, { destinataireId, contenu });
  }

  getFil(conversationId: number): Observable<FilMessagerie> {
    return this.http.get<FilMessagerie>(`${this.api}/conversations/${conversationId}`);
  }

  repondre(conversationId: number, contenu: string, fichier: File | null = null): Observable<MessagePrive> {
    const payload = new FormData();
    payload.append('contenu', contenu);
    if (fichier) {
      payload.append('fichier', fichier);
    }
    return this.http.post<MessagePrive>(`${this.api}/conversations/${conversationId}/messages`, payload);
  }

  telechargerPieceJointe(messageId: number): Observable<Blob> {
    return this.http.get(`${this.api}/messages/${messageId}/piece-jointe`, { responseType: 'blob' });
  }
}
