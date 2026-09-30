export interface Contactable {
  id: number;
  nom: string;
  role: string;
}

export interface Conversation {
  id: number;
  autrePersonneId: number | null;
  autrePersonneNom: string;
  dernierMessage: string | null;
  dateDernierMessage: string;
  nonLus: number;
}

export interface MessagePrive {
  id: number;
  auteurNom: string;
  estAuteur: boolean;
  contenu: string;
  dateEnvoi: string;
  pieceJointeNom: string | null;
}

export interface FilMessagerie {
  conversation: Conversation;
  messages: MessagePrive[];
}
