/**
 * Ce que le candidat a déclaré à l'étape « Formation & Parcours », relu pour la fiche d'admission.
 *
 * <p>Le formulaire envoie ces informations en JSON ; elles n'étaient jamais enregistrées (audit du 2026-10-13), et
 * la fiche de l'admission n'avait de toute façon aucun endroit où les lire. Le JSON est conservé tel quel côté
 * serveur — le formulaire reste seul juge de sa structure — donc la lecture est tolérante : une clé inconnue est
 * ignorée, un JSON abîmé ne casse pas l'écran.
 */
export interface ChampDeclare {
  libelle: string;
  valeur: string;
}

export interface FormationDeclaree {
  nom: string;
  etablissement: string;
  diplome: string;
  anneeObtention: string;
  pays: string;
}

export interface ParcoursDeclare {
  champs: ChampDeclare[];
  formations: FormationDeclaree[];
}

const LIBELLES_ACADEMIQUES: ReadonlyArray<[string, string]> = [
  ['niveauActuel', "Niveau d'études actuel"],
  ['etablissement', 'Établissement'],
  ['diplome', 'Dernier diplôme'],
  ['anneeObtention', "Année d'obtention"],
];

const LIBELLES_PROFESSIONNELS: ReadonlyArray<[string, string]> = [['estProfessionnel', 'Activité professionnelle']];

function lireObjet(json: string | null | undefined): Record<string, unknown> {
  if (!json) return {};
  try {
    const lu: unknown = JSON.parse(json);
    return lu !== null && typeof lu === 'object' && !Array.isArray(lu) ? (lu as Record<string, unknown>) : {};
  } catch {
    return {};
  }
}

function texte(valeur: unknown): string {
  return typeof valeur === 'string' || typeof valeur === 'number' ? String(valeur).trim() : '';
}

function champs(objet: Record<string, unknown>, libelles: ReadonlyArray<[string, string]>): ChampDeclare[] {
  return libelles
    .map(([cle, libelle]) => ({ libelle, valeur: texte(objet[cle]) }))
    .filter((c) => c.valeur !== '');
}

export function lireParcoursDeclare(academiques: string | null | undefined, professionnelles: string | null | undefined): ParcoursDeclare {
  const objetAcademique = lireObjet(academiques);
  const formations = Array.isArray(objetAcademique['formationsSuivies']) ? objetAcademique['formationsSuivies'] : [];
  return {
    champs: [...champs(objetAcademique, LIBELLES_ACADEMIQUES), ...champs(lireObjet(professionnelles), LIBELLES_PROFESSIONNELS)],
    formations: formations
      .filter((f): f is Record<string, unknown> => f !== null && typeof f === 'object')
      .map((f) => ({
        nom: texte(f['nom']),
        etablissement: texte(f['etablissement']),
        diplome: texte(f['diplome']),
        anneeObtention: texte(f['anneeObtention']),
        pays: texte(f['pays']),
      }))
      .filter((f) => f.nom !== ''),
  };
}

const LIBELLES_DOCUMENTS: Record<string, string> = {
  PIECE_IDENTITE: "Pièce d'identité",
  CV: 'CV',
  LETTRE_MOTIVATION: 'Lettre de motivation',
  RELEVE_NOTES: 'Relevé de notes',
  DIPLOME: 'Diplôme',
};

/** « PIECE_IDENTITE » est un identifiant du serveur, pas un libellé : la fiche d'admission l'affichait tel quel. */
export function libelleTypeDocument(type: string): string {
  return LIBELLES_DOCUMENTS[type] ?? type;
}
