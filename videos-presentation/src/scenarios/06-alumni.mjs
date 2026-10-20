// Vidéo 6 — L'alumni : profil, parcours professionnel, relevés, enquêtes de suivi, forum et documents partagés.
// Persona : Rokhaya Gueye (test.alumni@inpg.sn), diplômée du Master Pétrole & Gaz.
import { seConnecter } from './_communs.mjs';

const lien = (chemin) => `a[href="/espace-alumni/${chemin}"]`;

/** Un curseur (input range) se règle par programme : le geste à la souris n'a pas d'intérêt à l'écran. */
async function regler(a, selecteur, valeur) {
  const l = a.loc(selecteur);
  await a.pointer(l);
  await l.evaluate((el, v) => {
    el.value = v;
    el.dispatchEvent(new Event('input', { bubbles: true }));
    el.dispatchEvent(new Event('change', { bubbles: true }));
  }, String(valeur));
  await a.pause(500);
}

export default {
  id: '06-alumni',
  titre: "Espace alumni — rester en lien avec l'institut après le diplôme",
  depart: '/auth/login',
  ouverture: {
    pastille: 'Espace alumni',
    titre: "Rester en lien avec l'institut après le diplôme",
    sousTitre: 'Profil, parcours professionnel, relevés, enquêtes de suivi, forum et documents partagés',
    dire: "Voici l'espace des alumni, les anciens étudiants de l'institut. Il leur permet de garder leurs documents officiels, de raconter leur parcours, de répondre aux enquêtes qui améliorent les programmes, et d'échanger entre diplômés.",
  },
  etapes: [
    // ------------------------------------------------------------------ profil
    {
      chapitre: 'Profil et tableau de bord',
      dire: "L'étudiant dont le programme est terminé est marqué alumni par la direction pédagogique : son compte change de profil et conserve son identité, sa promotion et son matricule. Il se connecte comme avant.",
      faire: async (a) => {
        await seConnecter(a, 'test.alumni@inpg.sn', 'Test@2026');
      },
    },
    {
      dire: "Son profil résume l'ancienneté depuis le diplôme, la moyenne obtenue, le parcours renseigné et sa participation à la vie du réseau : contributions au forum, documents partagés ou consultés.",
      faire: async (a) => {
        await a.surligner('text=Moyenne au diplôme', { duree: 1700 });
        await a.defiler(350, { duree: 1500 });
        await a.pause(600);
        await a.defiler(-350, { duree: 800 });
      },
    },
    // ------------------------------------------------------------------ parcours
    {
      chapitre: 'Parcours professionnel',
      dire: "Le parcours professionnel retrace les postes occupés depuis la sortie de l'institut. L'alumni ajoute une étape en quelques champs : sa situation, l'intitulé du poste, l'entreprise et les dates.",
      faire: async (a) => {
        await a.cliquer(lien('mon-parcours'));
        await a.patienter();
        await a.cliquer('button:has-text("Ajouter une étape")');
        await a.pause(600);
        await a.taper('input[type=text] >> nth=-2', 'Ingénieure réservoir senior', { delai: 32 });
        await a.taper('input[type=text] >> nth=-1', 'Opérateur pétrolier national', { delai: 32 });
        await a.taper('input[type=date] >> nth=0', '2026-11-02');
      },
    },
    {
      dire: "Ces informations servent à mesurer l'insertion des diplômés, un indicateur suivi par la direction de la qualité.",
      faire: async (a) => {
        await a.cliquer('button:has-text("Enregistrer")');
        await a.patienter(1400);
      },
    },
    // ------------------------------------------------------------------ relevés
    {
      chapitre: 'Relevés de notes',
      dire: "Les relevés officiels délivrés par la direction pédagogique restent disponibles à vie. Leurs chiffres sont figés à la date d'émission : ils ne changent pas, même si une note est corrigée plus tard.",
      faire: async (a) => {
        await a.cliquer(lien('mes-releves'));
        await a.patienter();
        await a.surligner('text=Semestre 1', { duree: 1700 });
        await a.pause(800);
      },
    },
    // ------------------------------------------------------------------ enquêtes
    {
      chapitre: 'Enquêtes de suivi',
      dire: "Les enquêtes de suivi mesurent l'utilité des enseignements dans le poste actuel et l'effet de la formation sur la carrière. L'alumni répond en réglant deux curseurs, de un à cinq, et peut ajouter un commentaire.",
      faire: async (a) => {
        await a.cliquer(lien('enquetes'));
        await a.patienter();
        await a.cliquer('button:has-text("Répondre")');
        await a.pause(600);
        await regler(a, 'input[type=range] >> nth=0', 4);
        await regler(a, 'input[type=range] >> nth=1', 5);
        await a.taper('textarea', "Les cours de production m'ont directement servi ; j'aurais aimé plus de pratique sur les logiciels de simulation.", { delai: 22 });
      },
    },
    {
      dire: "Les réponses sont agrégées pour la direction pédagogique, qui s'en sert pour ajuster les programmes et alimenter le rapport de qualité.",
      faire: async (a) => {
        await a.cliquer('button:has-text("Envoyer")');
        await a.surligner('text=Merci, votre réponse a bien été enregistrée', { duree: 1500 });
        await a.cliquer('button:has-text("Fermer")');
      },
    },
    // ------------------------------------------------------------------ forum
    {
      chapitre: 'Forum des alumni',
      dire: "Le forum des alumni est un salon commun : offres de stage et d'emploi, retours d'expérience, événements. Il n'est ouvert qu'aux anciens étudiants.",
      faire: async (a) => {
        await a.cliquer(lien('forum'));
        await a.patienter();
        await a.cliquer('button:has-text("Nouveau sujet")');
        await a.pause(500);
        await a.taper('#titre', 'Recherche un mentor pour un projet de gaz naturel liquéfié', { delai: 28 });
        await a.taper('#message', "Je prépare un projet sur la valorisation du gaz. Un alumni du secteur accepterait-il de m'accorder une heure d'échange ?", { delai: 20 });
        await a.cliquer('button[type=submit]:has-text("Publier le sujet")');
        await a.patienter(1500);
      },
    },
    // ------------------------------------------------------------------ documents
    {
      chapitre: 'Documents partagés',
      dire: "Enfin, les alumni partagent des documents utiles aux autres : guides, veilles, modèles. Il suffit de choisir un fichier et de lui donner un titre.",
      faire: async (a) => {
        await a.cliquer(lien('documents'));
        await a.patienter();
        await a.pointer('button:has-text("Partager un document")');
        await a.televerser('input[type=file]', '/work/donnees/alumni/guide-insertion-professionnelle.pdf');
        await a.pause(600);
        await a.taper('#titre', 'Checklist de préparation aux entretiens techniques', { delai: 26 });
        await a.cliquer('button:has-text("Partager"):not(:has-text("un document"))');
        await a.patienter(1500);
      },
    },
  ],
  bilan: {
    titre: "Ce que l'alumni peut faire",
    puces: [
      'Garder ses relevés officiels, figés à leur date',
      'Raconter son parcours professionnel',
      'Répondre aux enquêtes de suivi',
      'Échanger avec les autres diplômés sur le forum',
      'Partager et consulter des documents utiles',
    ],
    dire: "Voilà l'espace des alumni : un lien durable avec l'institut, qui sert aussi à améliorer les programmes grâce à leurs retours.",
  },
};
