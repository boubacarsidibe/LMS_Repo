/**
 * Vocabulaire des statuts d'un dossier de candidature — UNE source pour tout le frontend.
 *
 * <p>Le 22/09/2026, le backend est passé de cinq statuts (SOUMIS, EN_COURS_DE_PRESELECTION, ADMIS1/2/3, REJETE) à
 * vingt (migration {@code V202609211230__workflow_recrutement_complet}, énumération {@code EnumStatutCandidature}).
 * Le frontend, lui, a continué de raisonner avec les anciens codes, recopiés dans une dizaine de composants :
 * <ul>
 *   <li>le formulaire « Nouveau test » ne listait plus aucun candidat (il attendait ADMIS1) ;</li>
 *   <li>la délibération, la convocation aux entretiens, les statistiques (« Rejetés 0 », taux d'admission « — »)
 *       ne voyaient aucun dossier ;</li>
 *   <li>la liste affichait « Soumis » d'un côté et le code brut « ENTRETIEN_EFFECTUE » de l'autre ;</li>
 *   <li>la frise du parcours se figeait avant l'entretien pour un dossier admis ;</li>
 *   <li>et le candidat lisait, sur sa page de suivi, un texte écrit en dur pour « le Bachelor in Strategic
 *       Management » et « la promotion 2024-2025 ».</li>
 * </ul>
 * Tout ce qui dépend d'un statut passe désormais par ce fichier : ajouter ou renommer un statut ne se fait qu'ici.
 */

export const STATUTS_CANDIDATURE = [
  'BROUILLON',
  'SOUMIS',
  'INCOMPLET',
  'DOSSIER_VALIDE',
  'DOSSIER_REJETE',
  'PRESELECTIONNE',
  'PAIEMENT_EN_ATTENTE',
  'PAIEMENT_CONFIRME',
  'CONVOQUE_TEST',
  'TEST_REUSSI',
  'TEST_ECHOUE',
  'ABSENT_TEST',
  'CONVOQUE_ENTRETIEN',
  'ENTRETIEN_EFFECTUE',
  'ABSENT_ENTRETIEN',
  'ADMIS',
  'LISTE_ATTENTE',
  'NON_ADMIS',
  'INSCRIPTION_FINALISEE',
  'DESISTEMENT',
] as const;

export type StatutCandidature = (typeof STATUTS_CANDIDATURE)[number];

export const LIBELLE_STATUT: Record<StatutCandidature, string> = {
  BROUILLON: 'Brouillon',
  SOUMIS: 'Soumis',
  INCOMPLET: 'Dossier incomplet',
  DOSSIER_VALIDE: 'Dossier validé',
  DOSSIER_REJETE: 'Dossier rejeté',
  PRESELECTIONNE: 'Présélectionné',
  PAIEMENT_EN_ATTENTE: 'Paiement en attente',
  PAIEMENT_CONFIRME: 'Paiement confirmé',
  CONVOQUE_TEST: 'Convoqué au test',
  TEST_REUSSI: 'Test réussi',
  TEST_ECHOUE: 'Test échoué',
  ABSENT_TEST: 'Absent au test',
  CONVOQUE_ENTRETIEN: "Convoqué à l'entretien",
  ENTRETIEN_EFFECTUE: 'Entretien effectué',
  ABSENT_ENTRETIEN: "Absent à l'entretien",
  ADMIS: 'Admis',
  LISTE_ATTENTE: 'Liste complémentaire',
  NON_ADMIS: 'Non admis',
  INSCRIPTION_FINALISEE: 'Inscrit',
  DESISTEMENT: 'Désistement',
};

const connu = (statut: string | null | undefined): statut is StatutCandidature =>
  !!statut && (STATUTS_CANDIDATURE as readonly string[]).includes(statut);

/** Libellé lisible ; un code inconnu (futur statut) est rendu tel quel plutôt que de faire planter l'écran. */
export function libelleStatut(statut: string | null | undefined): string {
  return connu(statut) ? LIBELLE_STATUT[statut] : (statut ?? '');
}

// ------------------------------------------------------------------ familles de statuts

/** Dossiers refusés ou abandonnés : plus rien à en attendre. */
export function estRefus(statut: string | null | undefined): boolean {
  return statut === 'DOSSIER_REJETE' || statut === 'NON_ADMIS' || statut === 'DESISTEMENT';
}

/** Admission prononcée (avec ou sans inscription finalisée). */
export function estAdmission(statut: string | null | undefined): boolean {
  return statut === 'ADMIS' || statut === 'INSCRIPTION_FINALISEE';
}

/** Décision finale prise : admis ou refusé. La liste complémentaire n'en est pas une, elle attend une place. */
export function estDecide(statut: string | null | undefined): boolean {
  return estAdmission(statut) || statut === 'DOSSIER_REJETE' || statut === 'NON_ADMIS';
}

/** Dossiers présélectionnés ou plus avancés dans l'évaluation, sans décision finale (compteur « présélectionnés »). */
export function estEnEvaluation(statut: string | null | undefined): boolean {
  return [
    'PRESELECTIONNE', 'PAIEMENT_EN_ATTENTE', 'PAIEMENT_CONFIRME', 'CONVOQUE_TEST', 'TEST_REUSSI', 'TEST_ECHOUE',
    'ABSENT_TEST', 'CONVOQUE_ENTRETIEN', 'ENTRETIEN_EFFECTUE', 'ABSENT_ENTRETIEN',
  ].includes(statut ?? '');
}

/** Refus prononcé par l'institut (dossier écarté, ou candidat non retenu) — hors désistement, qui est le choix du candidat. */
export function estRejete(statut: string | null | undefined): boolean {
  return statut === 'DOSSIER_REJETE' || statut === 'NON_ADMIS';
}

/** Dossier qui attend un geste du responsable pour être présélectionné (le chiffre « à traiter » des tableaux de bord). */
export function estATraiter(statut: string | null | undefined): boolean {
  return statut === 'SOUMIS' || statut === 'DOSSIER_VALIDE';
}

/** Dossiers que le responsable doit instruire : déposés, pas encore présélectionnés ni refusés. */
export function estAInstruire(statut: string | null | undefined): boolean {
  return statut === 'SOUMIS' || statut === 'INCOMPLET' || statut === 'DOSSIER_VALIDE';
}

export interface GroupeStatuts {
  cle: string;
  libelle: string;
  /** null = tous les statuts. */
  statuts: readonly StatutCandidature[] | null;
}

/** Onglets de la liste des candidatures, dans l'ordre où un dossier les traverse. */
export const GROUPES_STATUTS: readonly GroupeStatuts[] = [
  { cle: 'TOUS', libelle: 'Toutes', statuts: null },
  { cle: 'A_INSTRUIRE', libelle: 'À instruire', statuts: ['SOUMIS', 'INCOMPLET', 'DOSSIER_VALIDE'] },
  { cle: 'PRESELECTION', libelle: 'Présélection & paiement', statuts: ['PRESELECTIONNE', 'PAIEMENT_EN_ATTENTE', 'PAIEMENT_CONFIRME'] },
  { cle: 'TEST', libelle: 'Test', statuts: ['CONVOQUE_TEST', 'TEST_REUSSI', 'TEST_ECHOUE', 'ABSENT_TEST'] },
  { cle: 'ENTRETIEN', libelle: 'Entretien', statuts: ['CONVOQUE_ENTRETIEN', 'ENTRETIEN_EFFECTUE', 'ABSENT_ENTRETIEN'] },
  { cle: 'LISTE_ATTENTE', libelle: 'Liste complémentaire', statuts: ['LISTE_ATTENTE'] },
  { cle: 'ADMIS', libelle: 'Admises', statuts: ['ADMIS', 'INSCRIPTION_FINALISEE'] },
  { cle: 'REFUSES', libelle: 'Refusées', statuts: ['DOSSIER_REJETE', 'NON_ADMIS', 'DESISTEMENT'] },
];

/**
 * Un filtre venu d'une URL est une clé de groupe (« A_INSTRUIRE »), un statut précis (« SOUMIS »), ou plusieurs de ceux-là
 * séparés par des virgules (« PRESELECTION,TEST,ENTRETIEN ») : c'est ce qui permet à une carte de chiffres de renvoyer vers
 * la liste exactement filtrée comme elle a compté.
 *
 * @return {@code null} = aucun filtre (tous les statuts) ; {@code undefined} = filtre inconnu
 */
export function statutsDuFiltre(cle: string): readonly StatutCandidature[] | null | undefined {
  if (cle.includes(',')) {
    const morceaux = cle.split(',').map((c) => c.trim()).filter(Boolean).map((c) => statutsDuFiltre(c));
    if (morceaux.length === 0 || morceaux.some((m) => m === undefined)) return undefined;
    if (morceaux.some((m) => m === null)) return null;
    return [...new Set(morceaux.flatMap((m) => m as readonly StatutCandidature[]))];
  }
  const groupe = GROUPES_STATUTS.find((g) => g.cle === cle);
  if (groupe) return groupe.statuts;
  return connu(cle) ? [cle] : undefined;
}

// ------------------------------------------------------------------ présentation

export type TonStatut = 'attente' | 'en-cours' | 'succes' | 'liste' | 'echec' | 'neutre';

export function tonStatut(statut: string | null | undefined): TonStatut {
  switch (statut) {
    case 'ADMIS':
    case 'INSCRIPTION_FINALISEE':
    case 'TEST_REUSSI':
    case 'ENTRETIEN_EFFECTUE':
    case 'DOSSIER_VALIDE':
    case 'PAIEMENT_CONFIRME':
      return 'succes';
    case 'DOSSIER_REJETE':
    case 'NON_ADMIS':
    case 'TEST_ECHOUE':
    case 'ABSENT_TEST':
    case 'ABSENT_ENTRETIEN':
    case 'DESISTEMENT':
      return 'echec';
    case 'LISTE_ATTENTE':
      return 'liste';
    case 'CONVOQUE_TEST':
    case 'CONVOQUE_ENTRETIEN':
    case 'PRESELECTIONNE':
      return 'en-cours';
    case 'SOUMIS':
    case 'INCOMPLET':
    case 'PAIEMENT_EN_ATTENTE':
      return 'attente';
    default:
      return 'neutre';
  }
}

/** Classes Tailwind (jetons de la charte) du badge d'un statut. */
export function classeStatut(statut: string | null | undefined): string {
  switch (tonStatut(statut)) {
    case 'succes': return 'bg-green-100 text-green-800';
    case 'echec': return 'bg-error-container/20 text-error';
    case 'liste': return 'bg-blue-100 text-blue-800';
    case 'en-cours': return 'bg-primary-fixed text-on-primary-fixed-variant';
    case 'attente': return 'bg-secondary-container/30 text-on-secondary-container';
    default: return 'bg-surface-container text-on-surface-variant';
  }
}

// ------------------------------------------------------------------ qui peut faire quoi (miroir du backend)

/**
 * Convocation à un test : le dossier est « admis au test » (CONVOQUE_TEST — l'ancien ADMIS1). Le formulaire de test
 * ne propose que ces candidats.
 */
export const STATUTS_CONVOCABLES_AU_TEST: readonly StatutCandidature[] = ['CONVOQUE_TEST'];

/** Convocation à un entretien : le dossier est « admis à l'entretien » (CONVOQUE_ENTRETIEN — l'ancien ADMIS2). */
export const STATUTS_CONVOCABLES_A_L_ENTRETIEN: readonly StatutCandidature[] = ['CONVOQUE_ENTRETIEN'];

/** À délibérer : l'entretien a eu lieu et la décision reste à prendre, ou le dossier attend une place. */
export const STATUTS_A_DELIBERER: readonly StatutCandidature[] = ['ENTRETIEN_EFFECTUE', 'LISTE_ATTENTE'];

/**
 * « Rejeter » n'est proposé que là où le serveur l'accepte (CandidatureService#estTransitionValide : DOSSIER_REJETE
 * depuis l'instruction et la présélection, NON_ADMIS après une évaluation). Un dossier convoqué à un test ou à un
 * entretien attend son résultat : on ne le refuse qu'une fois celui-ci connu.
 */
export function peutRejeter(statut: string | null | undefined): boolean {
  return [
    'SOUMIS', 'INCOMPLET', 'DOSSIER_VALIDE', 'PRESELECTIONNE',
    'TEST_REUSSI', 'TEST_ECHOUE', 'ABSENT_TEST', 'ENTRETIEN_EFFECTUE', 'ABSENT_ENTRETIEN', 'LISTE_ATTENTE',
  ].includes(statut ?? '');
}

export type CleAction = 'VALIDER_PRESELECTION' | 'ADMETTRE_TEST' | 'ADMETTRE_ENTRETIEN' | 'ADMETTRE_FINAL' | 'FINALISER_INSCRIPTION';

export interface ActionDossier {
  cle: CleAction;
  libelle: string;
}

/**
 * Le geste que le responsable fait faire au dossier, selon son statut. Les résultats des épreuves et de l'entretien
 * ne sont pas des gestes : ils se déduisent des notes saisies (ProgressionDossierService côté serveur).
 */
export function actionSuivante(statut: string | null | undefined): ActionDossier | null {
  switch (statut) {
    case 'SOUMIS':
    case 'DOSSIER_VALIDE':
      return { cle: 'VALIDER_PRESELECTION', libelle: 'Valider la présélection' };
    case 'PAIEMENT_CONFIRME':
      return { cle: 'ADMETTRE_TEST', libelle: 'Admettre au test' };
    case 'TEST_REUSSI':
      return { cle: 'ADMETTRE_ENTRETIEN', libelle: "Admettre à l'entretien" };
    case 'ENTRETIEN_EFFECTUE':
    case 'LISTE_ATTENTE':
      return { cle: 'ADMETTRE_FINAL', libelle: 'Admettre (final)' };
    case 'ADMIS':
      return { cle: 'FINALISER_INSCRIPTION', libelle: "Finaliser l'inscription" };
    default:
      return null;
  }
}

/**
 * Ce qu'attend un dossier qui n'offre aucun geste, dit en une phrase — pour ne jamais laisser le responsable devant un
 * dossier muet sans savoir pourquoi.
 */
export function attenteDuDossier(statut: string | null | undefined): string | null {
  switch (statut) {
    case 'INCOMPLET': return 'Le candidat doit remplacer les pièces signalées.';
    case 'PRESELECTIONNE': return 'En attente du règlement des frais de dossier par le candidat.';
    case 'PAIEMENT_EN_ATTENTE': return 'Règlement en cours : à confirmer dans Paiements.';
    case 'CONVOQUE_TEST': return 'En attente des notes du test — elles se saisissent dans Tests académiques.';
    case 'CONVOQUE_ENTRETIEN': return "En attente de l'entretien — la note se saisit dans Entretiens.";
    case 'TEST_ECHOUE': return 'Test non réussi : le dossier peut être rejeté.';
    default: return null;
  }
}

// ------------------------------------------------------------------ corrections (annuler, reculer)

/** Rang d'un statut dans le parcours linéaire, miroir de CandidatureService#rangParcours ; absent = hors parcours. */
const RANG_RECUL: Partial<Record<StatutCandidature, number>> = {
  SOUMIS: 1, INCOMPLET: 2, DOSSIER_VALIDE: 3, PRESELECTIONNE: 4, PAIEMENT_EN_ATTENTE: 5, PAIEMENT_CONFIRME: 6,
  CONVOQUE_TEST: 7, TEST_REUSSI: 8, TEST_ECHOUE: 8, ABSENT_TEST: 8,
  CONVOQUE_ENTRETIEN: 9, ENTRETIEN_EFFECTUE: 10, ABSENT_ENTRETIEN: 10,
};

/** Étapes vers lesquelles le responsable peut ramener un dossier (liste déroulante « Reculer vers »). */
export const CIBLES_DE_RECUL: readonly { valeur: StatutCandidature; libelle: string }[] = [
  { valeur: 'SOUMIS', libelle: 'Soumis' },
  { valeur: 'PRESELECTIONNE', libelle: 'Présélectionné' },
  { valeur: 'CONVOQUE_TEST', libelle: 'Admis au test' },
  { valeur: 'CONVOQUE_ENTRETIEN', libelle: "Admis à l'entretien" },
];

/**
 * Peut-on ramener ce dossier vers `cible` ? Mêmes règles que CandidatureService#reculerStatut : jamais après
 * l'inscription, toujours possible depuis un refus ou la liste complémentaire (« dé-rejeter », « dé-lister »), sinon
 * seulement vers une étape strictement antérieure du parcours.
 */
export function peutReculerVers(statut: string | null | undefined, cible: string): boolean {
  if (!connu(statut) || statut === 'INSCRIPTION_FINALISEE') return false;
  if (statut === 'DOSSIER_REJETE' || statut === 'NON_ADMIS' || statut === 'LISTE_ATTENTE') return connu(cible) && RANG_RECUL[cible] !== undefined;
  const actuel = RANG_RECUL[statut];
  const vise = connu(cible) ? RANG_RECUL[cible] : undefined;
  return actuel !== undefined && vise !== undefined && vise < actuel;
}

/** « Annuler la dernière décision » : possible tant que l'accès étudiant n'a pas été ouvert. */
export function peutAnnuler(statut: string | null | undefined, statutPrecedent: string | null | undefined): boolean {
  return statutPrecedent != null && statut !== 'INSCRIPTION_FINALISEE';
}

// ------------------------------------------------------------------ frise du parcours

export interface TimelineItem {
  label: string;
  desc: string;
  /** L'étape n'est pas encore atteinte. */
  pending: boolean;
  /** L'étape est celle où se trouve le dossier. */
  active: boolean;
  /** L'étape s'est soldée par un échec. */
  erreur: boolean;
}

/** Position du dossier dans la frise : l'étape en cours (0 à 4), ou 5 quand toutes sont franchies. */
const RANG_FRISE: Record<StatutCandidature, number> = {
  BROUILLON: 0, INCOMPLET: 0,
  SOUMIS: 1, DOSSIER_VALIDE: 1, DOSSIER_REJETE: 1,
  PRESELECTIONNE: 2, PAIEMENT_EN_ATTENTE: 2,
  PAIEMENT_CONFIRME: 3, CONVOQUE_TEST: 3, TEST_ECHOUE: 3, ABSENT_TEST: 3,
  TEST_REUSSI: 4, CONVOQUE_ENTRETIEN: 4, ABSENT_ENTRETIEN: 4,
  ENTRETIEN_EFFECTUE: 5, ADMIS: 5, LISTE_ATTENTE: 5, INSCRIPTION_FINALISEE: 5, DESISTEMENT: 5,
  NON_ADMIS: 5, // ajusté plus bas d'après le statut précédent
};

/** Étape (0 à 4) où se solde un refus ; null si le statut n'est pas un échec de parcours. */
function etapeEnEchec(statut: StatutCandidature, statutPrecedent: string | null | undefined): number | null {
  switch (statut) {
    case 'DOSSIER_REJETE': return 1;
    case 'TEST_ECHOUE':
    case 'ABSENT_TEST': return 3;
    case 'ABSENT_ENTRETIEN': return 4;
    case 'NON_ADMIS':
      // Refusé après un test échoué ou manqué : l'échec est au test ; sinon il se joue à l'entretien/la décision.
      return statutPrecedent === 'TEST_ECHOUE' || statutPrecedent === 'ABSENT_TEST' ? 3 : 4;
    default: return null;
  }
}

/**
 * Frise « dépôt → présélection → paiement → test → entretien », la même pour le candidat (sa page de suivi) et pour le
 * responsable (le dossier) : il doit voir exactement l'étape que voit le candidat.
 */
export function friseParcours(
  statut: string | null | undefined,
  options: { audience: 'candidat' | 'agent'; statutPrecedent?: string | null }
): TimelineItem[] {
  const vous = options.audience === 'candidat';
  const items: TimelineItem[] = [
    { label: 'Dossier', desc: 'Tous les documents requis téléchargés.', pending: true, active: false, erreur: false },
    { label: 'Présélection', desc: vous ? 'Le jury examine votre candidature.' : 'Le jury examine la candidature.', pending: true, active: false, erreur: false },
    { label: 'Paiement', desc: 'Frais de dossier réglés.', pending: true, active: false, erreur: false },
    { label: 'Test Académique', desc: '', pending: true, active: false, erreur: false },
    { label: 'Entretien', desc: vous ? 'Rencontre avec le jury.' : '', pending: true, active: false, erreur: false },
  ];

  if (!connu(statut)) {
    items[0].active = true;
    return items;
  }

  const echec = etapeEnEchec(statut, options.statutPrecedent);
  const rang = echec ?? RANG_FRISE[statut];

  items.forEach((item, i) => {
    if (i < rang) item.pending = false;                       // franchie
    else if (i === rang && rang < items.length) {             // étape courante (ou en échec)
      item.pending = false;
      if (echec !== null) item.erreur = true;
      else item.active = true;
    }
  });
  // Un dossier qui sort de la frise sans échec (admis, liste complémentaire, entretien passé…) a franchi toutes les étapes.
  if (rang >= items.length && echec === null) items.forEach((item) => (item.pending = false));
  return items;
}

/** « Étape 4 sur 5 » : affichée à partir du test, quand le dossier est suffisamment avancé pour que cela parle. */
export function etapeActuelle(statut: string | null | undefined, statutPrecedent?: string | null): string {
  if (!connu(statut) || estRefus(statut) && etapeEnEchec(statut, statutPrecedent) === null) return '';
  const rang = etapeEnEchec(statut, statutPrecedent) ?? RANG_FRISE[statut];
  if (rang < 3) return '';
  return `Étape ${Math.min(rang + 1, 5)} sur 5`;
}

// ------------------------------------------------------------------ ce que lit le candidat

export interface MessageCandidat {
  /** Texte du badge. */
  badge: string;
  titre: string;
  description: string;
}

/**
 * Titre, texte et badge de la page de suivi du candidat. Les formulations ne citent jamais un programme ni une promotion
 * « en dur » (l'ancien texte parlait du « Bachelor in Strategic Management » et de la « promotion 2024-2025 » à tous) :
 * c'est le libellé de la session du dossier qui est injecté.
 */
export function messageCandidat(statut: string | null | undefined, session?: string | null): MessageCandidat {
  const cette = session ? `la session « ${session} »` : 'cette session';
  switch (statut) {
    case 'BROUILLON':
      return { badge: 'Brouillon', titre: "Votre dossier n'est pas encore déposé.", description: "Complétez votre dossier puis déposez-le : il sera alors examiné par le jury d'admission." };
    case 'INCOMPLET':
      return { badge: 'Dossier à compléter', titre: 'Des pièces de votre dossier sont à remplacer.', description: "Remplacez les documents signalés ci-dessous pour que votre dossier puisse être examiné." };
    case 'DOSSIER_VALIDE':
      return { badge: 'Dossier validé', titre: 'Votre dossier est complet.', description: "Vos pièces ont été jugées conformes. Votre dossier passe à l'étape de présélection." };
    case 'DOSSIER_REJETE':
      return { badge: 'Dossier rejeté', titre: "Votre dossier n'a pas été retenu.", description: `Nous regrettons de vous informer que votre dossier n'a pas pu être retenu pour ${cette}. Le motif figure ci-dessous.` };
    case 'PRESELECTIONNE':
      return { badge: 'Présélectionné(e)', titre: 'Vous êtes présélectionné(e) !', description: 'Votre dossier a retenu l\'attention du jury. Réglez maintenant les frais de dossier pour poursuivre votre candidature.' };
    case 'PAIEMENT_EN_ATTENTE':
      return { badge: 'Paiement en attente', titre: 'Votre règlement est en cours de traitement.', description: 'Dès que votre paiement sera confirmé, vous serez convoqué(e) au test académique.' };
    case 'PAIEMENT_CONFIRME':
      return { badge: 'Paiement confirmé', titre: 'Vos frais de dossier sont réglés.', description: 'Merci ! Vous serez prochainement convoqué(e) au test académique.' };
    case 'CONVOQUE_TEST':
      return { badge: 'Convoqué(e) au test', titre: 'Vous êtes convoqué(e) au test académique.', description: 'Retrouvez ci-dessous la date et le lieu du test. Vos résultats vous seront communiqués après correction.' };
    case 'TEST_REUSSI':
      return { badge: 'Test réussi', titre: 'Résultats du test académique', description: "Félicitations ! Vous avez réussi le test académique. La prochaine étape est l'entretien avec le jury." };
    case 'TEST_ECHOUE':
      return { badge: 'Test non concluant', titre: 'Résultats du test académique', description: `Malgré tout l'intérêt que nous portons à votre candidature, vos résultats au test ne nous permettent pas de poursuivre le processus d'admission pour ${cette}.` };
    case 'ABSENT_TEST':
      return { badge: 'Absent(e) au test', titre: "Vous n'avez pas passé le test académique.", description: `Votre absence au test ne nous permet pas de poursuivre le processus d'admission pour ${cette}.` };
    case 'CONVOQUE_ENTRETIEN':
      return { badge: "Convoqué(e) à l'entretien", titre: 'Vous y êtes presque !', description: "Vous êtes convoqué(e) à l'entretien avec le jury. Confirmez votre présence ci-dessous." };
    case 'ENTRETIEN_EFFECTUE':
      return { badge: 'Entretien effectué', titre: 'Votre entretien a eu lieu.', description: 'Le jury délibère. Vous serez informé(e) de sa décision par e-mail et sur cette page.' };
    case 'ABSENT_ENTRETIEN':
      return { badge: "Absent(e) à l'entretien", titre: "Vous n'avez pas passé l'entretien.", description: "Contactez l'équipe des admissions si votre absence était justifiée." };
    case 'LISTE_ATTENTE':
      return { badge: 'Liste complémentaire', titre: 'Vous êtes sur liste complémentaire', description: "Votre dossier a été jugé valable par le jury, mais aucune place n'est disponible pour l'instant. Nous reviendrons vers vous si une place se libère." };
    case 'ADMIS':
      return { badge: 'Admis(e)', titre: "Félicitations ! Bienvenue à l'INPG", description: `Votre candidature a été officiellement acceptée pour ${cette}. Retrouvez ci-dessous les prochaines étapes.` };
    case 'INSCRIPTION_FINALISEE':
      return { badge: 'Inscrit(e)', titre: 'Votre inscription est finalisée', description: "Votre accès à l'espace étudiant est ouvert : connectez-vous pour retrouver vos cours et votre calendrier." };
    case 'NON_ADMIS':
      return { badge: 'Non admis(e)', titre: 'Décision du jury', description: `Malgré la qualité de votre candidature, le jury n'a pas pu la retenir pour ${cette}. Nous vous remercions de votre intérêt pour l'INPG.` };
    case 'DESISTEMENT':
      return { badge: 'Désistement', titre: 'Votre désistement a été enregistré.', description: "Votre candidature est clôturée. Vous pouvez déposer un nouveau dossier lors d'une prochaine session." };
    case 'SOUMIS':
    default:
      return { badge: 'Candidature en cours', titre: "Votre dossier est en cours d'évaluation.", description: "Félicitations pour avoir complété la première étape. Notre jury d'admission examine actuellement vos documents avec la plus grande attention." };
  }
}
