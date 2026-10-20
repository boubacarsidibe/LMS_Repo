// Vidéo 5 — L'étudiant : suivre sa formation, évaluer ses cours, consulter ses résultats et son certificat.
// Persona : Khadija Ndiaye, promotion Master Pétrole & Gaz 2026 (programme terminé, certificat délivré).
import { seConnecter } from './_communs.mjs';

const lien = (chemin) => `a[href="/espace-etudiant/${chemin}"]`;

export default {
  id: '05-etudiant',
  titre: 'Espace étudiant — suivre sa formation, de la première séance au certificat',
  depart: '/auth/login',
  ouverture: {
    pastille: 'Espace étudiant',
    titre: 'Suivre sa formation, de la première séance au certificat',
    sousTitre: 'Cours et supports, calendrier, forum, évaluations, résultats, relevés et certificat',
    dire: "Voici l'espace de l'étudiant. Une fois admis et inscrit, il y retrouve tout ce qui rythme sa formation : ses cours, son calendrier, ses échanges avec les formateurs, ses notes et, au bout du parcours, son certificat.",
  },
  etapes: [
    // ------------------------------------------------------------------ connexion et tableau de bord
    {
      chapitre: 'Connexion et tableau de bord',
      dire: "L'étudiant se connecte avec les identifiants reçus lors de son inscription. La plateforme le reconnaît et l'amène directement dans son espace.",
      faire: async (a) => {
        await seConnecter(a, 'khadija.ndiaye@inpg-demo.sn', 'Etudiant@2026');
      },
    },
    {
      dire: "Le tableau de bord résume l'essentiel : les prochaines séances, le nombre de cours suivis, la progression globale et les supports à reprendre là où l'on s'était arrêté.",
      faire: async (a) => {
        await a.surligner('text=Prochaines séances', { duree: 1800 });
        await a.defiler(380, { duree: 1600 });
        await a.pause(900);
        await a.defiler(-380, { duree: 900 });
      },
    },
    // ------------------------------------------------------------------ cours
    {
      chapitre: 'Cours et supports',
      dire: "Les cours du programme sont regroupés par semestre. Chaque carte indique le formateur, le volume horaire et l'avancement de l'étudiant dans les supports publiés.",
      faire: async (a) => {
        await a.cliquer(lien('mes-cours'));
        await a.patienter();
        await a.cliquer('button:has-text("Semestre 2")');
        await a.pause(1200);
        await a.cliquer('button:has-text("Tous")');
      },
    },
    {
      dire: "Dans un cours, l'étudiant consulte les supports déposés par le formateur : documents, présentations ou liens. Chaque ouverture est comptée dans sa progression.",
      faire: async (a) => {
        await a.cliquer(lien('mes-cours/4'));
        await a.patienter();
        await a.surligner('text=Supports de cours', { duree: 1800 });
        await a.defiler(300, { duree: 1400 });
      },
    },
    {
      dire: "Il peut aussi évaluer le cours : une note de un à cinq et un commentaire. Cet avis est transmis au formateur et alimente le suivi de la qualité des enseignements.",
      faire: async (a) => {
        await a.surligner('text=Mon évaluation', { duree: 1400 });
        await a.cliquer('button:has(span:text-is("star")) >> nth=3');
        await a.taper('textarea', "Cours très clair, les études de cas sur les réservoirs m'ont beaucoup aidée.", { delai: 30 });
        await a.cliquer('button:has-text("Envoyer")');
        await a.patienter(1200);
      },
    },
    // ------------------------------------------------------------------ calendrier et annonces
    {
      chapitre: 'Calendrier et annonces',
      dire: "Le calendrier académique affiche les séances de tous ses cours : cours, travaux dirigés, travaux pratiques et examens. L'étudiant sait toujours ce qui l'attend dans la semaine.",
      faire: async (a) => {
        await a.cliquer(lien('calendrier'));
        await a.patienter();
        await a.defiler(300, { duree: 1400 });
        await a.pause(800);
      },
    },
    {
      dire: "Les annonces de l'institut arrivent dans son espace, avec une pastille pour les messages non lus : mémos, informations pratiques, visites de terrain.",
      faire: async (a) => {
        await a.cliquer(lien('annonces'));
        await a.patienter();
        await a.surligner('text=Tout marquer comme lu', { duree: 1600 });
      },
    },
    // ------------------------------------------------------------------ forum
    {
      chapitre: 'Forum et échanges',
      dire: "Le forum prolonge le cours : l'étudiant pose une question à son formateur ou à ses camarades, et les réponses restent visibles pour toute la promotion. Un sujet résolu peut être clos.",
      faire: async (a) => {
        await a.cliquer(lien('forum'));
        await a.patienter();
        await a.cliquer(lien('forum/11'));
        await a.patienter();
        await a.defiler(300, { duree: 1400 });
      },
    },
    // ------------------------------------------------------------------ résultats
    {
      chapitre: 'Résultats, relevés et certificat',
      dire: "La page des résultats donne la moyenne générale, pondérée par les coefficients de chaque cours, puis le détail des notes. Les notes sont sur cent, avec leur équivalent sur vingt.",
      faire: async (a) => {
        await a.cliquer(lien('mes-resultats'));
        await a.patienter();
        await a.surligner('text=Moyenne générale', { duree: 1800 });
        await a.defiler(700, { duree: 2200 });
      },
    },
    {
      dire: "Plus bas, les relevés de notes officiels délivrés par la direction pédagogique. Un relevé délivré est figé : même si une note est corrigée plus tard, le document remis ne change pas.",
      faire: async (a) => {
        await a.defiler(1500, { duree: 2600 });
        await a.pause(900);
      },
    },
    {
      dire: "Enfin, le certificat de formation, délivré au terme du programme. Il se télécharge en PDF et porte un code de vérification.",
      faire: async (a) => {
        await a.defiler(900, { duree: 1800 });
        await a.surligner('text=Mes certificats', { duree: 1800 });
      },
    },
    // ------------------------------------------------------------------ rendez-vous
    {
      chapitre: 'Rendez-vous avec le personnel',
      dire: "Pour un point individuel, l'étudiant choisit un membre du personnel et consulte ses créneaux libres.",
      faire: async (a) => {
        await a.cliquer(lien('rendez-vous'));
        await a.patienter();
        await a.cliquer('button:has-text("Moussa Ndiaye")');
        await a.pause(900);
        await a.cliquer("button:has-text(\"jusqu'à\") >> nth=0");
      },
    },
    {
      dire: "Il précise la modalité, en ligne ou en présentiel, indique le motif de sa demande et l'envoie. Son interlocuteur accepte, refuse ou propose une autre date, et l'étudiant est prévenu.",
      faire: async (a) => {
        await a.taper('textarea', "Point sur mon mémoire de fin de programme : choix du sujet et calendrier de rédaction.", { delai: 28 });
        await a.cliquer('button:has-text("Envoyer la demande")');
        await a.patienter(1500);
        await a.defiler(900, { duree: 1600 });
      },
    },
  ],
  bilan: {
    titre: "Ce que l'étudiant peut faire",
    puces: [
      'Suivre ses cours et ses supports, avec sa progression',
      'Évaluer chaque cours pour améliorer la formation',
      'Consulter calendrier, annonces et forum de sa promotion',
      'Connaître ses notes sur cent et leur équivalent sur vingt',
      'Télécharger ses relevés officiels et son certificat',
      'Demander un rendez-vous au personnel',
    ],
    dire: "Voilà l'espace de l'étudiant : un seul endroit pour suivre sa formation, mesurer sa progression et conserver ses documents officiels, du premier cours jusqu'au certificat.",
  },
};
