// Réglages communs de la chaîne de production vidéo. Tout ce qui influe sur le rendu est ici.
export const CFG = {
  /** URL de l'application filmée (frontend ; il relaie /api vers le backend). */
  base: process.env.BASE_URL ?? 'http://localhost:4200',

  /**
   * Fenêtre filmée : 1280 × 720 pixels CSS à l'échelle 1,5 = 1920 × 1080 pixels réels. Le texte de l'interface
   * paraît donc 1,5 fois plus gros que sur un écran 1080p « à plat » : lisible en plein écran comme dans une
   * fenêtre de visioconférence ou sur un vidéoprojecteur.
   */
  fenetre: { width: 1280, height: 720 },
  echelle: 1.5,
  fps: 30,

  /** Voix off. « Multilingual » = les voix neuronales les plus naturelles du service. */
  voix: process.env.VOIX ?? 'fr-FR-VivienneMultilingualNeural',
  /** Léger ralentissement : le spectateur doit aussi pouvoir lire l'écran. */
  vitesse: process.env.VITESSE ?? '-6%',

  /** Qualité vidéo : CRF 20 = visuellement sans perte pour une interface, fichier léger. */
  crf: process.env.CRF ?? '20',
  preset: process.env.PRESET ?? 'medium',

  dossiers: {
    sortie: '/work/sortie',
    cache: '/work/cache',
    donnees: '/work/donnees',
  },

  /** Silences (ms) qui donnent du rythme. */
  pauses: {
    entreEtapes: 450,
    avantAction: 350,
    apresClic: 450,
    apresNavigation: 700,
  },

  /**
   * Prononciation : le texte AFFICHÉ (sous-titres) reste celui du scénario, seul le texte DIT est réécrit.
   * Sans cela, la synthèse lit « INPG » comme un mot (« inpigue »). Les sigles sont épelés à la française.
   */
  prononciations: [
    [/\bINPG\b/g, 'I N P G'],
    [/\bLMS\b/g, 'L M S'],
    [/\bQCM\b/g, 'Q C M'],
    [/\bPDF\b/g, 'P D F'],
    [/\bQR\b/g, 'Q R'],
    [/\bDRCQ\b/g, 'D R C Q'],
    [/\bHSE\b/g, 'H S E'],
    [/\bMPG\b/g, 'M P G'],
    [/\bSMTP\b/g, 'S M T P'],
    [/\bXOF\b/g, 'francs CFA'],
    [/\bFCFA\b/g, 'francs CFA'],
    [/\bERP\b/g, 'E R P'],
    [/\bCV\b/g, 'C V'],
    [/\bSIGB\b/g, 'S I G B'],
    [/\bTDR\b/g, 'T D R'],
    [/\bRH\b/g, 'R H'],
    [/\bIA\b/g, 'I A'],
    [/\bOTP\b/g, 'O T P'],
    [/\bURL\b/g, 'U R L'],
    [/\bSMS\b/g, 'S M S'],
    [/\bQMS-(\d+)/g, 'Q M S $1'],
  ],

  couleurs: {
    nuit: '#131741',
    bleu: '#30609a',
    accent: '#FF9F1C',
  },
};
