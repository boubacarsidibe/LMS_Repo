// Vidéo 2 — Le responsable des admissions (1/2) : sessions, instruction des dossiers, paiements, statistiques.
// Compte : test.admission@inpg.sn. Dix-sept dossiers à tous les stades du parcours ; chaque geste de la vidéo fait
// réellement avancer un dossier précis.
import { seConnecter } from './_communs.mjs';

const lien = (chemin) => `a[href="/responsable-admission${chemin}"]`;
const dansJours = (n) => new Date(Date.now() + n * 86400000).toISOString().slice(0, 10);

export default {
  id: '02-admission',
  titre: "Portail Admissions (1/2) — sessions, instruction des dossiers et paiements",
  depart: '/auth/login',
  ouverture: {
    pastille: 'Responsable des admissions — 1 sur 2',
    titre: 'Ouvrir une session, instruire les dossiers, valider les paiements',
    sousTitre: "Vue d'ensemble, sessions, candidatures, pièces, présélection, rejet motivé, paiements et statistiques",
    dire: "Voici le portail du responsable des admissions. Cette première vidéo suit un dossier de la création de la session jusqu'à l'admission au test : ouverture des candidatures, vérification des pièces, présélection, rejet motivé et validation des paiements.",
  },
  etapes: [
    // ------------------------------------------------------------------ vue d'ensemble
    {
      chapitre: "Vue d'ensemble",
      dire: "À la connexion, le responsable voit l'essentiel : le nombre de candidatures, les sessions actives, les dossiers à traiter et le taux d'admission. Les dossiers les plus récents sont juste en dessous.",
      faire: async (a) => {
        await seConnecter(a, 'test.admission@inpg.sn', 'Test@2026');
        await a.surligner('text=À traiter', { duree: 1600 });
        await a.defiler(420, { duree: 1700 });
        await a.pause(500);
        await a.defiler(-420, { duree: 800 });
      },
    },
    // ------------------------------------------------------------------ sessions
    {
      chapitre: 'Sessions de candidature',
      dire: "Une session ouvre les candidatures pour une formation, entre deux dates, avec un quota. Le responsable suit pour chacune le nombre de candidatures reçues et son statut.",
      faire: async (a) => {
        await a.cliquer(lien('/sessions'));
        await a.patienter();
        await a.surligner('text=Prochaine clôture', { duree: 1600 });
      },
    },
    {
      dire: "Pour en créer une, il donne un intitulé et des dates, fixe éventuellement le tarif des frais de dossier, les critères d'admission et le seuil de réussite du test. Sans tarif propre, le tarif par défaut de l'établissement s'applique.",
      faire: async (a) => {
        await a.cliquer('text=Nouvelle session');
        await a.page.waitForURL(/sessions\/nouvelle/);
        await a.patienter();
        await a.taper('#session-libelle', 'Session Printemps 2027 — Master Pétrole & Gaz', { delai: 30 });
        await a.taper('#session-date-ouverture', dansJours(10));
        await a.taper('#session-date-fermeture', dansJours(100));
        await a.taper('#session-frais-dossier', '25000');
        await a.taper('#session-seuil-reussite', '12');
        await a.cliquer('button[type=submit]:has-text("Créer la session")');
        await a.patienter(1600);
      },
    },
    // ------------------------------------------------------------------ candidatures
    {
      chapitre: 'Instruction des dossiers',
      dire: "La liste des candidatures peut se filtrer par session, par étape du parcours ou par recherche libre. Ici, les dossiers à instruire : ceux qui viennent d'être déposés et attendent une première vérification.",
      faire: async (a) => {
        await a.cliquer(lien('/candidatures'));
        await a.patienter();
        await a.choisir('select >> nth=1', { index: 1 });
        await a.pause(1400);
      },
    },
    {
      dire: "En ouvrant un dossier, le responsable retrouve la frise du parcours du candidat et la liste de ses pièces justificatives, avec leur état de vérification.",
      faire: async (a) => {
        await a.cliquer('tr:has-text("Aminata Sarr")');
        await a.page.waitForURL(/candidatures\/\d+/);
        await a.patienter(1000);
        await a.surligner('text=Timeline du parcours', { duree: 1500 });
        await a.defiler(650, { duree: 1800 });
      },
    },
    {
      dire: "Pour chaque pièce, il la télécharge et la contrôle, puis la valide, ou demande son remplacement avec un motif : le candidat est alors prévenu et son dossier passe en « incomplet ».",
      faire: async (a) => {
        // Une pièce validée perd son bouton : on valide toujours la première restante (les index se décaleraient sinon).
        const valider = a.page.locator('button:text-is("Valider")');
        await a.cliquer('button:text-is("Valider") >> nth=0');
        await a.cliquer('button:text-is("Valider") >> nth=0');
        // La validation est asynchrone : le bouton disparaît après coup. Un clic tardif ne doit pas attendre trente secondes.
        for (let i = 0; i < 6 && (await valider.count()) > 0; i++) {
          await valider.first().click({ timeout: 1500 }).catch(() => {});
          await a.page.waitForTimeout(350);
        }
        await a.pause(900);
      },
    },
    {
      dire: "Quand toutes les pièces sont conformes, le responsable valide la présélection. Le candidat est informé, et il est invité à régler ses frais de dossier.",
      faire: async (a) => {
        await a.defiler(900, { duree: 1300 });
        await a.cliquer('button:has-text("Valider la présélection")');
        await a.patienter(1800);
      },
    },
    {
      dire: "À l'inverse, un dossier qui ne remplit pas les conditions peut être rejeté. Le motif est obligatoire : il est transmis au candidat et conservé dans le journal du dossier.",
      faire: async (a) => {
        await a.aller('/responsable-admission/candidatures');
        await a.cliquer('tr:has-text("Ibrahima Ndiaye")');
        await a.page.waitForURL(/candidatures\/\d+/);
        await a.patienter(900);
        await a.defiler(1500, { duree: 1500 });
        await a.cliquer('button:has-text("Rejeter")');
        await a.pause(900);
        await a.taper('textarea', "Le diplôme joint ne correspond pas au niveau d'études exigé pour cette session.", { delai: 24 });
      },
    },
    {
      dire: "La décision est enregistrée avec la date et le nom de celui qui l'a prise : rien ne se perd, et une décision peut toujours être corrigée tant que l'inscription n'est pas finalisée.",
      faire: async (a) => {
        await a.cliquer('button:has-text("Confirmer le rejet"), button:has-text("Rejeter le dossier"), button:has-text("Confirmer")');
        await a.patienter(1800);
      },
    },
    // ------------------------------------------------------------------ paiements
    {
      chapitre: 'Paiements des frais de dossier',
      dire: "La page des paiements affiche l'encaissement total, les règlements réussis, en attente et les impayés. Les candidats qui ont déclaré un règlement attendent la confirmation de l'équipe.",
      faire: async (a) => {
        await a.aller('/responsable-admission/paiements');
        await a.surligner('text=Total encaissé', { duree: 1700 });
        await a.defiler(300, { duree: 1300 });
      },
    },
    {
      dire: "Le responsable vérifie la transaction, puis confirme le paiement. Le dossier passe à « paiement confirmé », le candidat reçoit son reçu en PDF, et il peut être admis au test.",
      faire: async (a) => {
        await a.cliquer('tr:has-text("Mariama Diallo") button:has-text("Valider")');
        await a.patienter(1800);
      },
    },
    // ------------------------------------------------------------------ admission au test
    {
      chapitre: 'Admission au test',
      dire: "Un dossier dont le paiement est confirmé peut être admis au test académique. Un seul geste, et le candidat est convoqué : il retrouve la date de l'épreuve dans son espace.",
      faire: async (a) => {
        await a.aller('/responsable-admission/candidatures');
        await a.cliquer('tr:has-text("Cheikh Mbaye")');
        await a.page.waitForURL(/candidatures\/\d+/);
        await a.patienter(900);
        await a.defiler(1500, { duree: 1500 });
        await a.surligner('button:has-text("Admettre au test")', { duree: 1600 });
        await a.cliquer('button:has-text("Admettre au test")');
        await a.patienter(1800);
      },
    },
    // ------------------------------------------------------------------ statistiques
    {
      chapitre: 'Statistiques et rapports',
      dire: "Les statistiques s'appuient sur les vrais dossiers : répartition par étape du parcours et taux d'admission parmi les dossiers déjà tranchés. Des rapports PDF et Excel se génèrent d'un clic, pour la direction.",
      faire: async (a) => {
        await a.aller('/responsable-admission/statistiques');
        await a.pause(1500);
        await a.defiler(520, { duree: 2000 });
        await a.pause(600);
      },
    },
  ],
  bilan: {
    titre: 'Ce que le responsable des admissions peut faire (1/2)',
    puces: [
      'Ouvrir une session, avec ses critères et ses frais',
      'Instruire un dossier pièce par pièce',
      'Présélectionner, ou rejeter avec un motif',
      'Confirmer les paiements et délivrer les reçus',
      'Admettre un candidat au test académique',
      'Suivre des statistiques fiables et produire des rapports',
    ],
    dire: "Voilà pour l'instruction des dossiers. Dans la vidéo suivante, nous suivons les candidats à travers les tests, les entretiens et la délibération, jusqu'à l'inscription.",
  },
};
