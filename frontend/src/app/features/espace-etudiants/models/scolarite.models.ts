/**
 * Types de l'espace étudiant, alignés sur les DTO du backend
 * (`inpg.sn.lms.espace_etudiant.dto`).
 */

export type TypeAnnonce = 'ANNONCE' | 'MESSAGE' | 'MEMO';

export interface PromotionDetail {
  id: number;
  code: string;
  libelle: string;
  dateDebut: string | null;
  dateFin: string | null;
  enCours: boolean;
  programme: string | null;
  activitesParascolaires: string | null;
  formationCode: string | null;
  formationIntitule: string | null;
  formationDescription: string | null;
  effectif: number;
  nbCours: number;
}

/** Réponse de `GET /scolarite/ma-promotion`. */
export interface MaPromotion {
  matricule: string;
  promotion: PromotionDetail | Record<string, never>;
  message?: string;
}

/** Camarade de promotion — nom et matricule seulement, jamais l'email ni le téléphone. */
export interface Camarade {
  id: number;
  nom: string;
  matricule: string | null;
}

export interface CoursEtudiant {
  id: number;
  code: string;
  titre: string;
  description: string;
  semestre: string;
  volumeHoraire: number;
  imageUrl: string | null;
  formateurs: string[];
  nbSupports: number;
  nbSupportsConsultes: number;
  /** Pourcentage 0-100, dérivé des supports consultés. */
  progression: number;
  maNote: number | null;
}

export interface SupportEtudiant {
  id: number;
  titre: string;
  typeRessource: string;
  url: string;
  datePublication: string | null;
  consulte: boolean;
}

export interface SeanceEtudiant {
  id: number;
  titre: string;
  type: string;
  dateHeure: string;
  dureeMinutes: number;
  lieuOuLien: string | null;
  coursId: number | null;
  coursTitre: string | null;
}

export interface Annonce {
  id: number;
  titre: string;
  contenu: string;
  type: TypeAnnonce;
  datePublication: string;
  auteur: string;
  promotionLibelle: string | null;
  epinglee: boolean;
  lue: boolean;
}

export interface ForumSujet {
  id: number;
  titre: string;
  coursId: number | null;
  coursTitre: string | null;
  promotionId: number | null;
  promotionLibelle: string | null;
  auteur: string;
  dateCreation: string;
  epingle: boolean;
  clos: boolean;
  nbMessages: number;
  dateDernierMessage: string;
}

export interface ForumMessage {
  id: number;
  auteur: string;
  contenu: string;
  dateCreation: string;
  estAuteur: boolean;
  reponseAId?: number | null;
  reponseAAuteur?: string | null;
  reponseAExtrait?: string | null;
  /** Nom à afficher pour le téléchargement ; absent si le message n'a pas de pièce
   *  jointe, ou si le fichier annoncé a disparu du serveur. */
  pieceJointeNom?: string | null;
}

export interface FilForum {
  sujet: ForumSujet;
  messages: ForumMessage[];
}

export interface AlbumPhoto {
  id: number;
  titre: string;
  description: string | null;
  dateEvenement: string | null;
  promotionLibelle: string | null;
  nbPhotos: number;
  couvertureUrl: string | null;
}

export interface Photo {
  id: number;
  url: string;
  legende: string | null;
  /** Nom du fichier d'origine, pour un téléchargement avec une extension correcte. */
  nomOriginal: string | null;
  dateAjout: string;
}

export interface ContenuAlbum {
  album: AlbumPhoto;
  photos: Photo[];
}

export interface EvaluationCours {
  id: number;
  coursId: number;
  coursTitre: string;
  note: number;
  commentaire: string | null;
  dateSoumission: string;
}

export interface SyntheseEvaluation {
  coursId: number;
  coursTitre: string;
  moyenne: number | null;
  nombreEvaluations: number;
}

/**
 * Relevé de notes de l'étudiant, groupé par cours.
 *
 * <p>À ne pas confondre avec `maNote` de CoursEtudiant, qui porte l'appréciation
 * que l'étudiant donne au cours — pas la note qu'il reçoit.
 */
export interface NoteModule {
  id: number;
  module: string;
  note: number;
  commentaire: string | null;
  dateEvaluation: string | null;
  coursId: number | null;
  coursCode: string | null;
  coursTitre: string | null;
  formateurNom: string | null;
}

export interface NotesCours {
  coursId: number;
  coursCode: string;
  coursTitre: string;
  /** null si aucune note n'a encore été saisie dans ce cours. */
  moyenne: number | null;
  /** Poids de ce cours dans la moyenne générale. Renvoyé pour que le relevé soit vérifiable. */
  coefficient: number;
  notes: NoteModule[];
}

/**
 * Relevé DÉLIVRÉ par la direction pédagogique : un document.
 *
 * <p>À distinguer de {@link ReleveNotes}, qui est le relevé courant, recalculé à
 * chaque affichage. Les chiffres de celui-ci sont figés à sa date d'émission : les
 * deux peuvent donc différer, et c'est justement l'intérêt.
 */
export interface ReleveDelivre {
  id: number;
  reference: string;
  etudiantNom: string;
  matricule: string | null;
  promotionLibelle: string | null;
  periode: string;
  dateEmission: string;
  delivrePar: string;
  moyenneGenerale: number | null;
  nombreNotes: number;
  lignes: { coursCode: string; coursTitre: string; coefficient: number; moyenne: number | null }[];
}

export interface ReleveNotes {
  moyenneGenerale: number | null;
  nombreNotes: number;
  parCours: NotesCours[];
}

/**
 * Support recemment ouvert, propose pour reprendre son travail.
 *
 * <p>Un suivi equivalent vivait dans le localStorage du navigateur : il enregistrait
 * des formations du catalogue, pas le travail reel, et disparaissait au changement
 * d'appareil. La trace est desormais serveur.
 */
export interface Reprise {
  supportId: number;
  supportTitre: string;
  typeRessource: string | null;
  url: string | null;
  coursId: number;
  coursTitre: string;
  dateConsultation: string;
}
