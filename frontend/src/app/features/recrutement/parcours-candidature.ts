/**
 * Parcours d'une candidature : ce que le candidat et l'admission voient d'un statut.
 *
 * <p>Ce fichier est la **seule** définition de la correspondance statut → phase d'affichage et de
 * la frise de progression. Elle existait en deux copies (suivi du candidat, détail côté admission)
 * dont le commentaire d'origine avertissait déjà que « toute divergence serait un bug muet » : à la
 * refonte du workflow (22/09), aucune des deux n'a suivi, et le candidat ne voyait plus rien.
 */

/** Une des huit « phases » d'affichage, chacune ayant son gabarit. */
export type PhaseSuivi = 'SOUMIS' | 'INCOMPLET' | 'PRESELECTION' | 'ADMIS1' | 'ADMIS2' | 'ADMIS3' | 'LISTE_ATTENTE' | 'REJETE';

/**
 * Le backend a vingt statuts depuis la refonte du workflow (22/09) ; les écrans n'en connaissaient
 * que cinq, anciens (`ADMIS1/2/3`, `EN_COURS_DE_PRESELECTION`, `REJETE`). La correspondance reprend
 * celle de la migration SQL `V202609211230` (ADMIS1 = convoqué au test, ADMIS2 = entretien, ADMIS3 = admis).
 * Les statuts `PAIEMENT_*` subsistent en base : le paiement en ligne n'est pas encore en service
 * (en attente de l'API Mobile Money), ils sont rangés dans la phase la plus proche.
 */
const PHASE_PAR_STATUT: Record<string, PhaseSuivi> = {
  SOUMIS: 'SOUMIS',
  DOSSIER_VALIDE: 'SOUMIS',
  INCOMPLET: 'INCOMPLET',
  PRESELECTIONNE: 'PRESELECTION',
  PAIEMENT_EN_ATTENTE: 'PRESELECTION',
  PAIEMENT_CONFIRME: 'ADMIS1',
  CONVOQUE_TEST: 'ADMIS1',
  TEST_REUSSI: 'ADMIS1',
  CONVOQUE_ENTRETIEN: 'ADMIS2',
  ENTRETIEN_EFFECTUE: 'ADMIS2',
  ADMIS: 'ADMIS3',
  INSCRIPTION_FINALISEE: 'ADMIS3',
  LISTE_ATTENTE: 'LISTE_ATTENTE',
  DOSSIER_REJETE: 'REJETE',
  TEST_ECHOUE: 'REJETE',
  ABSENT_TEST: 'REJETE',
  ABSENT_ENTRETIEN: 'REJETE',
  NON_ADMIS: 'REJETE',
  DESISTEMENT: 'REJETE',
};

/**
 * Phase d'un statut. `null` pour un brouillon (rien n'est encore déposé : écran d'accueil).
 * Un statut inconnu retombe sur « en cours d'évaluation » plutôt que sur un écran vide : un
 * nouveau statut ajouté au backend dégrade l'écran, il ne le supprime pas.
 */
export function phaseDeStatut(statut: string | null | undefined): PhaseSuivi | null {
  if (!statut || statut === 'BROUILLON') return null;
  return PHASE_PAR_STATUT[statut] ?? 'SOUMIS';
}

/**
 * Un candidat peut-il déposer un nouveau dossier alors que celui-ci existe ? Oui s'il n'est qu'un brouillon, refusé
 * à l'examen du dossier, ou non admis — c'est la règle du serveur (`CandidatController#soumettreCandidature` écarte
 * ces trois statuts du verrou). Le formulaire testait `'REJETE'`, un statut qui n'existe plus depuis la refonte du
 * workflow (22/09) : le test ne retenait jamais personne et un dossier refusé renvoyait vers le suivi.
 */
export function depotPossibleApres(statut: string | null | undefined): boolean {
  return statut === 'BROUILLON' || statut === 'DOSSIER_REJETE' || statut === 'NON_ADMIS';
}

export interface EtapeFrise {
  label: string;
  desc: string;
  pending: boolean;
  active: boolean;
  erreur: boolean;
}

/**
 * Frise de progression.
 *
 * <p>Candidat : dépôt → présélection → test académique → entretien. Le paiement en ligne n'est pas
 * encore en service (en attente de l'API Mobile Money) : le candidat n'a donc aucune étape de paiement.
 *
 * <p>Admission : la même frise **avec** l'étape « Paiement » entre la présélection et le test. Un règlement
 * fait hors plateforme est enregistré à la main par le responsable admission, qui doit voir où en est
 * ce règlement. `pointDeVue` change aussi la tournure des descriptions (« votre candidature » / « la
 * candidature »).
 *
 * <p>`documentsAFournir` : nombre de pièces exigées que le serveur dit manquantes ou à remplacer
 * (`documentsAttendus`). La description de l'étape « Dossier » était figée sur « Tous les documents requis
 * téléchargés », y compris pour un dossier à qui il en manquait : le candidat lisait cette phrase juste
 * au-dessus de la liste qui lui en réclamait. `null` = inconnu, la description reste neutre.
 */
export function friseCandidature(
  statut: string | null | undefined,
  pointDeVue: 'candidat' | 'admission',
  documentsAFournir: number | null = null,
): EtapeFrise[] {
  const base = friseSansPaiement(statut, pointDeVue === 'candidat', documentsAFournir);
  if (pointDeVue === 'candidat') return base;

  const phase = phaseDeStatut(statut);
  const apresPaiement = phase === 'ADMIS1' || phase === 'ADMIS2' || phase === 'ADMIS3' || phase === 'LISTE_ATTENTE'
    || statut === 'TEST_ECHOUE' || statut === 'ABSENT_TEST' || statut === 'ABSENT_ENTRETIEN' || statut === 'NON_ADMIS';
  const paiement: EtapeFrise = {
    label: 'Paiement', desc: 'Frais de dossier réglés.',
    pending: !(phase === 'PRESELECTION' || apresPaiement),   // à venir tant que le dossier n'est pas présélectionné
    active: phase === 'PRESELECTION',                        // règlement attendu
    erreur: false,
  };
  // Présélectionné : côté candidat le test est la prochaine étape ; côté admission c'est le règlement,
  // le test n'est alors qu'à venir.
  const test: EtapeFrise = phase === 'PRESELECTION' ? { ...base[2], pending: true, active: false } : base[2];
  return [base[0], base[1], paiement, test, base[3]];
}

/** Ce que l'étape « Dossier » affirme : jamais plus que ce que le serveur sait du contenu du dossier. */
function descriptionDossier(candidat: boolean, documentsAFournir: number | null): string {
  if (documentsAFournir === null) return 'Dossier déposé.';
  if (documentsAFournir === 0) return 'Tous les documents requis téléchargés.';
  const documents = `${documentsAFournir} document${documentsAFournir > 1 ? 's' : ''}`;
  return candidat ? `${documents} à fournir ou à remplacer.` : `${documents} à fournir ou à remplacer par le candidat.`;
}

function friseSansPaiement(statut: string | null | undefined, candidat: boolean, documentsAFournir: number | null): EtapeFrise[] {
  const phase = phaseDeStatut(statut);
  const items: EtapeFrise[] = [
    { label: 'Dossier', desc: descriptionDossier(candidat, documentsAFournir), pending: true, active: false, erreur: false },
    { label: 'Présélection', desc: candidat ? 'Le jury examine votre candidature.' : 'Le jury examine la candidature.', pending: true, active: false, erreur: false },
    { label: 'Test Académique', desc: '', pending: true, active: false, erreur: false },
    { label: 'Entretien', desc: candidat ? 'Rencontre avec le jury.' : '', pending: true, active: false, erreur: false },
  ];
  /** Étape franchie. */
  const fait = (n: number) => { items[n].pending = false; };
  /** Étape en cours. */
  const encours = (n: number) => { items[n].pending = false; items[n].active = true; };
  /**
   * Étape en échec : « franchie, pas en attente » — sinon le gabarit affichait à la fois le numéro
   * d'étape et la croix, et l'étape était grisée au lieu de rouge.
   */
  const echec = (n: number) => { items[n].pending = false; items[n].erreur = true; };

  if (phase === null) { items[0].active = true; return items; }          // brouillon
  if (phase === 'INCOMPLET') { items[0].active = true; return items; }   // un complément est attendu
  fait(0);                                                                // dossier déposé
  if (phase === 'SOUMIS') { encours(1); return items; }                  // dossier en examen
  fait(1);                                                                // présélection franchie

  if (phase === 'PRESELECTION') { encours(2); return items; }            // convocation au test à venir
  if (phase === 'REJETE') {
    // Où le parcours s'est arrêté : l'étape fautive dépend du statut réel, pas de la seule phase.
    switch (statut) {
      case 'DOSSIER_REJETE': echec(1); break;
      case 'TEST_ECHOUE': case 'ABSENT_TEST': echec(2); break;
      case 'ABSENT_ENTRETIEN': case 'NON_ADMIS': fait(2); echec(3); break;
      default: echec(2);                                                  // désistement
    }
    return items;
  }
  if (phase === 'ADMIS1') {                                               // test académique
    if (statut === 'TEST_REUSSI') { fait(2); encours(3); } else { encours(2); }
    return items;
  }
  fait(2);                                                                // test franchi
  if (phase === 'ADMIS2') { encours(3); return items; }                  // entretien
  fait(3);                                                                // admis, ou liste complémentaire
  return items;
}
