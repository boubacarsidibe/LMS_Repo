export interface MessageForum {
  id: number;
  auteur: string;
  initiales: string;
  contenu: string;
  date: string;
  pieceJointeNom?: string | null;
}

export interface SujetForum {
  id: number;
  auteur: string;
  initiales: string;
  couleurAvatar: string;
  couleurTexte: string;
  cours: string;
  module: string;
  titre: string;
  apercu: string;
  date: string;
  statut: 'nouveau' | 'urgent' | 'resolu';
  messages: MessageForum[];
}
