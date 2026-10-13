import { Observable, firstValueFrom } from 'rxjs';

/** Une pièce prête à partir vers le dossier déjà déposé. */
export interface PieceAEnvoyer {
  /** Libellé lisible par le candidat : « Relevé de notes — année 1, bulletin 2 (releve.pdf) ». */
  libelle: string;
  file: File;
  /** Valeur de `EnumTypeDocument` côté serveur : PIECE_IDENTITE, CV, RELEVE_NOTES, DIPLOME. */
  typeDocument: string;
  /** Description libre (obligatoire pour un diplôme). */
  description?: string | null;
  /** Moyenne déclarée, pour un relevé de notes. */
  moyenne?: number | null;
}

export interface EchecEnvoi {
  piece: PieceAEnvoyer;
  /** Message à montrer au candidat, tiré de la réponse du serveur quand elle en porte un. */
  raison: string;
  /** Vrai si un nouvel essai ne peut rien changer (fichier refusé, dossier figé) : inutile de le proposer. */
  definitif: boolean;
}

export interface BilanEnvoi {
  recues: PieceAEnvoyer[];
  echecs: EchecEnvoi[];
}

export interface OptionsEnvoi {
  /** Nombre d'essais par pièce, le premier compris. */
  tentatives?: number;
  /** Attente avant le n-ième nouvel essai, en millisecondes (1 pour le premier nouvel essai). */
  delaiMs?: (nouvelEssai: number) => number;
  /** Pièces en route en même temps : peu, pour ne pas saturer une connexion mobile. */
  simultanes?: number;
  /** Appelée à chaque pièce terminée, réussie ou non. */
  surProgression?: (faites: number, total: number) => void;
  /** Substituable dans les tests : par défaut, une vraie attente. */
  attendre?: (ms: number) => Promise<void>;
}

const TENTATIVES_PAR_DEFAUT = 3;
const SIMULTANES_PAR_DEFAUT = 2;
const delaiParDefaut = (nouvelEssai: number): number => 800 * nouvelEssai * nouvelEssai;
const attendreParDefaut = (ms: number): Promise<void> => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * Une panne qui peut passer d'elle-même : réseau coupé ou délai dépassé (statut 0), requête expirée, trop de
 * requêtes, erreur du serveur. Tout le reste — fichier refusé (400), accès (403), dossier figé (409), fichier trop
 * gros (413) — donnerait la même réponse au prochain essai.
 *
 * <p>Réessayer est sans danger : le serveur reconnaît une pièce déjà reçue (même contenu, même description) et
 * répond « déjà reçue » au lieu d'en créer une seconde. Un envoi dont la réponse s'est perdue en route, mais qui
 * avait abouti, ne se retrouve donc jamais en double.
 */
export function estPanneTransitoire(erreur: unknown): boolean {
  const statut = (erreur as { status?: number } | null)?.status;
  if (statut === undefined || statut === null) {
    return true;
  }
  return statut === 0 || statut === 408 || statut === 425 || statut === 429 || statut >= 500;
}

/** Ce que le serveur a répondu, sinon une phrase qui dit quoi faire. */
export function raisonEchec(erreur: unknown): string {
  const e = erreur as { status?: number; error?: { message?: string } } | null;
  const messageServeur = e?.error?.message;
  if (typeof messageServeur === 'string' && messageServeur.trim()) {
    return messageServeur.trim();
  }
  switch (e?.status) {
    case 0:
      return 'Connexion interrompue.';
    case 413:
      return 'Fichier trop volumineux.';
    case 401:
      return 'Votre session a expiré : reconnectez-vous.';
    default:
      return e?.status ? `Erreur ${e.status} du serveur.` : 'Envoi interrompu.';
  }
}

/**
 * Envoie les pièces vers le dossier, quelques-unes à la fois, en reprenant les pannes passagères.
 *
 * <p>Ne lève jamais : le bilan dit ce qui est arrivé et ce qui reste à envoyer. Le formulaire s'appuie dessus pour ne
 * dire « tout est transmis » que quand c'est vrai — un envoi qui échoue ne doit pas laisser le candidat croire que
 * son dossier est complet, ni le renvoyer sans explication vers un écran qui lui redemande ses documents.
 */
export async function envoyerPieces(
  pieces: PieceAEnvoyer[],
  envoyer: (piece: PieceAEnvoyer) => Observable<unknown>,
  options: OptionsEnvoi = {},
): Promise<BilanEnvoi> {
  const tentatives = Math.max(1, options.tentatives ?? TENTATIVES_PAR_DEFAUT);
  const delai = options.delaiMs ?? delaiParDefaut;
  const attendre = options.attendre ?? attendreParDefaut;
  const simultanes = Math.max(1, options.simultanes ?? SIMULTANES_PAR_DEFAUT);

  const bilan: BilanEnvoi = { recues: [], echecs: [] };
  let prochaine = 0;
  let terminees = 0;

  const envoyerAvecReprises = async (piece: PieceAEnvoyer): Promise<void> => {
    for (let essai = 1; ; essai++) {
      try {
        await firstValueFrom(envoyer(piece));
        bilan.recues.push(piece);
        return;
      } catch (erreur) {
        const peutReessayer = estPanneTransitoire(erreur) && essai < tentatives;
        if (!peutReessayer) {
          bilan.echecs.push({ piece, raison: raisonEchec(erreur), definitif: !estPanneTransitoire(erreur) });
          return;
        }
        await attendre(delai(essai));
      }
    }
  };

  const ouvrier = async (): Promise<void> => {
    while (prochaine < pieces.length) {
      const piece = pieces[prochaine++];
      await envoyerAvecReprises(piece);
      terminees++;
      options.surProgression?.(terminees, pieces.length);
    }
  };

  await Promise.all(Array.from({ length: Math.min(simultanes, pieces.length) }, ouvrier));
  return bilan;
}
