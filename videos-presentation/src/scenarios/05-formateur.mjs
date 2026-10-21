// Vidéo 4 — Le formateur : piloter ses cours, suivre ses étudiants, animer sa promotion.
// Persona : Moussa Ndiaye (prof.demo@inpg.sn), qui enseigne les cours du Master Pétrole & Gaz.
import { seConnecter } from './_communs.mjs';

const lien = (chemin) => `a[href="/espace-etudiant/formateur/${chemin}"]`;
/** Date dans n jours, au format attendu par un champ de type « date ». */
const dansJours = (n) => {
  const d = new Date(Date.now() + n * 86400000);
  return d.toISOString().slice(0, 10);
};

export default {
  id: '05-formateur',
  titre: 'Espace formateur — piloter ses cours, suivre ses étudiants, animer sa promotion',
  depart: '/auth/login',
  ouverture: {
    pastille: 'Espace formateur',
    titre: 'Piloter ses cours, suivre ses étudiants, animer sa promotion',
    sousTitre: 'Tableau de bord, ressources, suivi, planning, forum, retours des étudiants et annonces',
    dire: "Voici l'espace du formateur. Il lui donne une vue claire de ses cours et de ses étudiants, et les moyens de publier des ressources, de planifier des séances et d'échanger avec sa promotion.",
  },
  etapes: [
    // ------------------------------------------------------------------ connexion et tableau de bord
    {
      chapitre: 'Connexion et tableau de bord',
      dire: "Le formateur se connecte, et la plateforme l'envoie directement dans son espace. Seuls les cours qui lui sont affectés par la direction pédagogique y apparaissent.",
      faire: async (a) => {
        await seConnecter(a, 'prof.demo@inpg.sn', 'Demo@2026');
      },
    },
    {
      dire: "Le tableau de bord réunit l'essentiel : le nombre d'étudiants suivis, ses cours actifs, la progression moyenne de ses étudiants, les prochaines séances, l'activité du forum et la note moyenne donnée par les étudiants.",
      faire: async (a) => {
        await a.surligner('text=Étudiants suivis', { duree: 1500 });
        await a.defiler(520, { duree: 1800 });
        await a.surligner('text=Analyse de la progression étudiante', { duree: 1700 });
        await a.defiler(300, { duree: 1400 });
        await a.pause(600);
        await a.defiler(-820, { duree: 1000 });
      },
    },
    // ------------------------------------------------------------------ cours et ressources
    {
      chapitre: 'Cours et ressources',
      dire: "La page des cours liste ceux que le formateur enseigne, avec le nombre d'étudiants concernés et les supports déjà publiés. Il peut filtrer par semestre ou par promotion.",
      faire: async (a) => {
        await a.cliquer(lien('mes-cours'));
        await a.patienter();
        await a.surligner('text=Cours publiés', { duree: 1600 });
        await a.defiler(520, { duree: 1800 });
      },
    },
    {
      dire: "Pour déposer une ressource, il choisit le cours concerné, puis le type de contenu : un document, un lien externe ou une vidéo. Une date de publication peut la programmer : elle reste invisible des étudiants jusque-là.",
      faire: async (a) => {
        await a.defiler(-520, { duree: 800 });
        await a.cliquer('a:has-text("Créer une ressource") >> nth=0');
        await a.patienter();
        await a.choisir('#cours-ressource', { index: 2 });
        await a.pause(800);
        await a.cliquer('button:has-text("Continuer")');
        await a.patienter(1200);
      },
    },
    // ------------------------------------------------------------------ suivi des étudiants
    {
      chapitre: 'Suivi des étudiants',
      dire: "Le suivi des étudiants montre, pour chacun, sa promotion, sa dernière activité sur la plateforme, sa progression dans les supports et sa moyenne. Les étudiants à accompagner ressortent d'un coup d'œil.",
      faire: async (a) => {
        await a.cliquer(lien('suivi-etudiants'));
        await a.patienter();
        await a.surligner('text=Étudiants à suivre', { duree: 1500 });
        await a.defiler(450, { duree: 1800 });
      },
    },
    {
      dire: "Il peut filtrer par promotion ou par statut, par exemple ne garder que les étudiants à risque, et les relancer par message, individuellement ou à toute la liste.",
      faire: async (a) => {
        await a.choisir('select >> nth=1', { index: 2 });
        await a.pause(1600);
        await a.cliquer('button:has-text("Réinitialiser")');
        await a.defiler(-450, { duree: 700 });
        await a.cliquer('button:has-text("Envoyer une relance collective")');
        await a.pause(1800);
      },
    },
    // ------------------------------------------------------------------ planning
    {
      chapitre: 'Planning et séances',
      dire: "Le calendrier pédagogique présente les séances du mois. Le formateur programme une séance en quelques champs : le cours, le titre, la date et l'heure, la durée, le type, et un lien de classe virtuelle si besoin.",
      faire: async (a) => {
        await a.cliquer('button:has-text("Fermer")');
        await a.cliquer(lien('planning-calendrier'));
        await a.patienter();
        await a.cliquer('button:has-text("Programmer une session")');
        await a.pause(700);
        await a.choisir('#seance-cours', { index: 1 });
        await a.taper('#seance-titre', 'Séance de révision avant les examens', { delai: 30 });
        await a.taper('#seance-date', dansJours(9));
        await a.taper('#seance-heure', '10:00');
        await a.taper('#seance-duree', '90');
      },
    },
    {
      dire: "Une fois enregistrée, la séance apparaît dans le calendrier de ses étudiants, qui la retrouvent dans leur propre espace.",
      faire: async (a) => {
        await a.taper('#seance-lien', 'https://visio.inpg-demo.sn/revision', { delai: 22 });
        await a.cliquer('button[type=submit]:has-text("Programmer")');
        await a.patienter(1500);
      },
    },
    // ------------------------------------------------------------------ forum
    {
      chapitre: 'Forum et échanges',
      dire: "Le forum est l'endroit où les étudiants posent leurs questions. Le formateur voit les sujets non lus, ouvre un fil et répond : sa réponse est visible de toute la promotion, et il peut mettre le fil en avant ou le clore.",
      faire: async (a) => {
        await a.cliquer(lien('forum'));
        await a.patienter();
        await a.cliquer("text=Quelle norme pour le plan d'urgence");
        await a.patienter(900);
        await a.defiler(350, { duree: 1200 });
        await a.taper('textarea', "Pour ce TP, citez la norme ISO 45001 et le plan d'urgence interne du site. Je dépose un modèle dans les supports du cours.", { delai: 24 });
        await a.cliquer('button:has-text("Publier la réponse")');
        await a.patienter(1200);
      },
    },
    // ------------------------------------------------------------------ retours
    {
      chapitre: 'Retours des étudiants',
      dire: "Les retours des étudiants alimentent le suivi de la qualité : note moyenne, taux de satisfaction et volume d'avis. Le formateur repère les cours à ajuster et peut exporter les données.",
      faire: async (a) => {
        await a.cliquer(lien('retours-evaluations'));
        await a.patienter();
        await a.surligner('text=Note moyenne', { duree: 1500 });
        await a.defiler(520, { duree: 1800 });
        await a.pause(500);
      },
    },
    // ------------------------------------------------------------------ annonces
    {
      chapitre: 'Annonces',
      dire: "Enfin, le formateur publie une annonce à destination d'une ou plusieurs promotions : un titre, un message, et la possibilité de l'épingler en tête de liste.",
      faire: async (a) => {
        await a.cliquer(lien('annonces'));
        await a.patienter();
        await a.cliquer('button:has-text("Publier une annonce")');
        await a.pause(600);
        await a.taper('#annonce-titre', 'Séance de révision avant les examens', { delai: 28 });
        await a.taper('#annonce-contenu', "Une séance de révision en ligne aura lieu la semaine prochaine. Préparez vos questions sur les cours de forage et de production.", { delai: 20 });
        await a.cliquer('label:has-text("Master Pétrole & Gaz")');
        await a.pause(700);
        await a.cliquer('button[type=submit]:has-text("Publier")');
        await a.patienter(1500);
      },
    },
  ],
  bilan: {
    titre: 'Ce que le formateur peut faire',
    puces: [
      'Voir ses cours, ses étudiants et leur progression',
      'Déposer des ressources, immédiates ou programmées',
      'Repérer et relancer les étudiants à accompagner',
      'Programmer ses séances et classes virtuelles',
      'Répondre aux questions sur le forum',
      'Suivre les retours des étudiants et publier des annonces',
    ],
    dire: "Voilà l'espace du formateur : tout ce qu'il faut pour enseigner, suivre ses étudiants et rester en lien avec sa promotion, depuis une seule interface.",
  },
};
