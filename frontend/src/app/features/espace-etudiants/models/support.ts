export interface Support {
  id: string;
  titre?: string;
  url?: string;
  /** pdf, lien, video... sert a choisir l'icone. */
  typeRessource?: string | null;
  /** Date a partir de laquelle la ressource est visible des etudiants. */
  datePublication?: string | null;
  /** Vrai si cette date est encore dans le futur : seuls les formateurs du cours la voient. */
  programme?: boolean;
  coursId?: number | null;
  coursCode?: string | null;
}
