/**
 * Les statuts d'une candidature vus par l'admission : libellés, couleurs, regroupements en onglets, et actions.
 *
 * <p>Ce fichier est la **seule** définition. Chaque écran de l'espace admission (liste, fiche, délibération, vue
 * d'ensemble, statistiques, paiements, entretiens, tests) recopiait la sienne avec l'ancien vocabulaire
 * (`EN_COURS_DE_PRESELECTION`, `ADMIS1/2/3`, `REJETE`) que le serveur n'émet plus depuis la refonte du workflow
 * (22/09) : les libellés sortaient en nom technique, les compteurs restaient à zéro, la délibération était vide et,
 * à partir de la présélection, aucun bouton ne s'affichait. La correspondance statut → phase du **candidat** vit à
 * côté, dans `parcours-candidature.ts` ; celle-ci est la lecture de l'**admission**.
 */

/** Les vingt valeurs de `EnumStatutCandidature` (serveur), dans l'ordre du parcours. */
export const STATUTS_CANDIDATURE = [
  'BROUILLON', 'SOUMIS', 'INCOMPLET', 'DOSSIER_VALIDE', 'PRESELECTIONNE', 'PAIEMENT_EN_ATTENTE', 'PAIEMENT_CONFIRME',
  'CONVOQUE_TEST', 'TEST_REUSSI', 'TEST_ECHOUE', 'ABSENT_TEST', 'CONVOQUE_ENTRETIEN', 'ENTRETIEN_EFFECTUE',
  'ABSENT_ENTRETIEN', 'ADMIS', 'LISTE_ATTENTE', 'INSCRIPTION_FINALISEE', 'DOSSIER_REJETE', 'NON_ADMIS', 'DESISTEMENT',
] as const;

export type StatutCandidature = (typeof STATUTS_CANDIDATURE)[number];

/** Regroupement des statuts en étapes que l'admission traite ensemble : un onglet, une barre de statistiques. */
export type GroupeStatut =
  | 'A_EXAMINER' | 'A_COMPLETER' | 'PRESELECTION' | 'TEST' | 'ENTRETIEN' | 'LISTE_ATTENTE' | 'ADMIS' | 'ECARTE';

type Ton = 'examen' | 'attente' | 'progres' | 'liste' | 'positif' | 'negatif' | 'neutre';

const STATUTS: Record<StatutCandidature, { libelle: string; groupe: GroupeStatut | null; ton: Ton }> = {
  BROUILLON: { libelle: 'Brouillon', groupe: null, ton: 'neutre' },
  SOUMIS: { libelle: 'Soumis', groupe: 'A_EXAMINER', ton: 'examen' },
  INCOMPLET: { libelle: 'À compléter', groupe: 'A_COMPLETER', ton: 'attente' },
  DOSSIER_VALIDE: { libelle: 'Dossier validé', groupe: 'A_EXAMINER', ton: 'examen' },
  PRESELECTIONNE: { libelle: 'Présélectionné', groupe: 'PRESELECTION', ton: 'progres' },
  PAIEMENT_EN_ATTENTE: { libelle: 'Paiement en attente', groupe: 'PRESELECTION', ton: 'attente' },
  PAIEMENT_CONFIRME: { libelle: 'Paiement confirmé', groupe: 'PRESELECTION', ton: 'progres' },
  CONVOQUE_TEST: { libelle: 'Convoqué au test', groupe: 'TEST', ton: 'progres' },
  TEST_REUSSI: { libelle: 'Test réussi', groupe: 'TEST', ton: 'progres' },
  TEST_ECHOUE: { libelle: 'Test échoué', groupe: 'TEST', ton: 'negatif' },
  ABSENT_TEST: { libelle: 'Absent au test', groupe: 'TEST', ton: 'attente' },
  CONVOQUE_ENTRETIEN: { libelle: "Convoqué à l'entretien", groupe: 'ENTRETIEN', ton: 'progres' },
  ENTRETIEN_EFFECTUE: { libelle: 'Entretien effectué', groupe: 'ENTRETIEN', ton: 'progres' },
  ABSENT_ENTRETIEN: { libelle: "Absent à l'entretien", groupe: 'ENTRETIEN', ton: 'attente' },
  LISTE_ATTENTE: { libelle: 'Liste complémentaire', groupe: 'LISTE_ATTENTE', ton: 'liste' },
  ADMIS: { libelle: 'Admis', groupe: 'ADMIS', ton: 'positif' },
  INSCRIPTION_FINALISEE: { libelle: 'Inscription finalisée', groupe: 'ADMIS', ton: 'positif' },
  DOSSIER_REJETE: { libelle: 'Dossier rejeté', groupe: 'ECARTE', ton: 'negatif' },
  NON_ADMIS: { libelle: 'Non admis', groupe: 'ECARTE', ton: 'negatif' },
  DESISTEMENT: { libelle: 'Désistement', groupe: 'ECARTE', ton: 'neutre' },
};

/** Jetons de couleur par ton — ceux de la charte, jamais un hexadécimal. */
const CLASSES_TON: Record<Ton, string> = {
  examen: 'bg-secondary-container/30 text-on-secondary-container',
  attente: 'bg-warning-container text-on-warning-container',
  progres: 'bg-primary-fixed text-on-primary-fixed-variant',
  liste: 'bg-primary-container text-on-primary-container',
  positif: 'bg-success/15 text-success-strong',
  negatif: 'bg-error-container/20 text-error',
  neutre: 'bg-surface-container text-on-surface-variant',
};

/** Les groupes dans l'ordre où un dossier les traverse, avec leur libellé d'onglet. */
export const GROUPES_STATUT: ReadonlyArray<{ cle: GroupeStatut; libelle: string; libelleCourt: string }> = [
  { cle: 'A_EXAMINER', libelle: 'À examiner', libelleCourt: 'À examiner' },
  { cle: 'A_COMPLETER', libelle: 'À compléter (en attente du candidat)', libelleCourt: 'À compléter' },
  { cle: 'PRESELECTION', libelle: 'Présélection et règlement', libelleCourt: 'Présélection' },
  { cle: 'TEST', libelle: 'Test académique', libelleCourt: 'Test' },
  { cle: 'ENTRETIEN', libelle: 'Entretien', libelleCourt: 'Entretien' },
  { cle: 'LISTE_ATTENTE', libelle: 'Liste complémentaire', libelleCourt: 'Liste complémentaire' },
  { cle: 'ADMIS', libelle: 'Admis', libelleCourt: 'Admis' },
  { cle: 'ECARTE', libelle: 'Écartés (rejetés, non admis, désistés)', libelleCourt: 'Écartés' },
];

/** Un statut inconnu (ajouté au serveur plus tard) s'affiche tel quel : il dégrade l'écran, il ne le casse pas. */
export function libelleStatut(statut: string | null | undefined): string {
  return statut ? STATUTS[statut as StatutCandidature]?.libelle ?? statut : '';
}

export function classeStatut(statut: string | null | undefined): string {
  const info = statut ? STATUTS[statut as StatutCandidature] : undefined;
  return CLASSES_TON[info?.ton ?? 'neutre'];
}

export function groupeDeStatut(statut: string | null | undefined): GroupeStatut | null {
  return statut ? STATUTS[statut as StatutCandidature]?.groupe ?? null : null;
}

export function statutsDuGroupe(groupe: GroupeStatut): StatutCandidature[] {
  return STATUTS_CANDIDATURE.filter((s) => STATUTS[s].groupe === groupe);
}

export function libelleGroupe(groupe: GroupeStatut): string {
  return GROUPES_STATUT.find((g) => g.cle === groupe)?.libelleCourt ?? groupe;
}

/** Admis : décision d'admission prise, accès étudiant ouvert ou à ouvrir. */
export function estAdmis(statut: string | null | undefined): boolean {
  return statut === 'ADMIS' || statut === 'INSCRIPTION_FINALISEE';
}

/** Écarté par l'INPG : dossier rejeté, ou non admis après évaluation. Le désistement est le choix du candidat. */
export function estRefuse(statut: string | null | undefined): boolean {
  return statut === 'DOSSIER_REJETE' || statut === 'NON_ADMIS';
}

/** Décision de l'INPG prise (admis ou refusé) : la base du taux d'admission. La liste complémentaire n'en est pas une. */
export function estDecide(statut: string | null | undefined): boolean {
  return estAdmis(statut) || estRefuse(statut);
}

/** Dossier retenu et engagé dans la sélection : présélectionné, au test ou à l'entretien. */
export function estEngageDansLaSelection(statut: string | null | undefined): boolean {
  const groupe = groupeDeStatut(statut);
  return groupe === 'PRESELECTION' || groupe === 'TEST' || groupe === 'ENTRETIEN';
}

/** Ce que l'admission peut faire d'un dossier — les valeurs de `CandidatureService#actionsPossibles` (serveur). */
export type ActionAdmission =
  | 'PRESELECTIONNER' | 'DEMANDER_COMPLEMENT' | 'ENREGISTRER_REGLEMENT' | 'CONVOQUER_TEST'
  | 'ENREGISTRER_RESULTAT_TEST' | 'ADMETTRE_ENTRETIEN' | 'ENREGISTRER_RESULTAT_ENTRETIEN' | 'ADMETTRE_FINAL'
  | 'PLACER_LISTE_ATTENTE' | 'FINALISER_INSCRIPTION' | 'REJETER';

/** La part d'une candidature dont dépendent les actions : tout vient du serveur. */
export interface DossierAvecActions {
  statut: string;
  statutPrecedent?: string | null;
  actionsPossibles?: string[] | null;
  reculsPossibles?: string[] | null;
}

export function libelleAction(action: ActionAdmission, statut: string): string {
  switch (action) {
    case 'PRESELECTIONNER': return 'Valider présélection';
    case 'DEMANDER_COMPLEMENT': return 'Demander un complément';
    case 'ENREGISTRER_REGLEMENT': return 'Enregistrer le règlement';
    case 'CONVOQUER_TEST': return statut === 'ABSENT_TEST' ? 'Reconvoquer au test' : 'Convoquer au test';
    case 'ENREGISTRER_RESULTAT_TEST': return 'Résultat du test';
    case 'ADMETTRE_ENTRETIEN': return statut === 'ABSENT_ENTRETIEN' ? "Reconvoquer à l'entretien" : "Convoquer à l'entretien";
    case 'ENREGISTRER_RESULTAT_ENTRETIEN': return "Résultat de l'entretien";
    case 'ADMETTRE_FINAL': return 'Admettre';
    case 'PLACER_LISTE_ATTENTE': return 'Liste complémentaire';
    case 'FINALISER_INSCRIPTION': return "Finaliser l'inscription";
    case 'REJETER': return 'Rejeter';
  }
}

export function peutFaire(c: DossierAvecActions, action: ActionAdmission): boolean {
  return (c.actionsPossibles ?? []).includes(action);
}

/** Le geste suivant du parcours : la première action proposée qui n'est pas un rejet. */
export function actionSuivante(c: DossierAvecActions): { action: ActionAdmission; libelle: string } | null {
  const action = (c.actionsPossibles ?? []).find((a) => a !== 'REJETER') as ActionAdmission | undefined;
  return action ? { action, libelle: libelleAction(action, c.statut) } : null;
}

export function peutRejeter(c: DossierAvecActions): boolean {
  return peutFaire(c, 'REJETER');
}

/** Annuler la dernière décision : possible tant que l'accès étudiant n'est pas ouvert. */
export function peutAnnuler(c: DossierAvecActions): boolean {
  return c.statutPrecedent != null && c.statut !== 'INSCRIPTION_FINALISEE';
}

export function reculsPossibles(c: DossierAvecActions): string[] {
  return c.reculsPossibles ?? [];
}

/**
 * Statuts dont le dossier est encore relançable : le candidat peut agir sur ses pièces. Même liste que
 * `ApplicationController#STATUTS_RELANCABLES` (serveur) : sans elle, le compteur « dossiers incomplets » compterait
 * des dossiers que la relance ne visera pas.
 */
export const STATUTS_RELANCABLES: readonly string[] = ['SOUMIS', 'INCOMPLET', 'PRESELECTIONNE', 'PAIEMENT_EN_ATTENTE'];
