// Vidéo 9 — L'entreprise partenaire : consulter le catalogue, déposer une demande de formation, échanger avec l'équipe.
// Compte : test.entreprise@inpg.sn (Sahel Énergie SA, entreprise fictive).
import { seConnecter } from './_communs.mjs';

/** Retour à l'accueil de l'espace, d'où partent les accès rapides (cartes) : c'est ainsi qu'une entreprise y navigue. */
const accueil = async (a) => { await a.aller('/espace-entreprise'); };

export default {
  id: '09-entreprise',
  titre: "Espace entreprise — exprimer un besoin de formation et suivre la réponse",
  depart: '/auth/login',
  ouverture: {
    pastille: 'Espace entreprise',
    titre: 'Exprimer un besoin de formation et suivre la réponse',
    sousTitre: 'Catalogue, demande ou devis, messagerie avec l’équipe pédagogique, rendez-vous',
    dire: "Voici l'espace des entreprises partenaires de l'institut. Il leur permet d'exprimer un besoin de formation, de demander un devis pour une session sur mesure et d'échanger directement avec l'équipe pédagogique.",
  },
  etapes: [
    // ------------------------------------------------------------------ accès
    {
      chapitre: 'Accès et accueil',
      dire: "Une entreprise s'inscrit sur le site public avec le profil Entreprise, puis se connecte. Elle arrive sur une page d'accueil qui propose les gestes essentiels : consulter le catalogue, déposer une demande, écrire à l'équipe, prendre rendez-vous.",
      faire: async (a) => {
        await seConnecter(a, 'test.entreprise@inpg.sn', 'Test@2026');
        await a.surligner('text=Déposer une nouvelle demande', { duree: 1700 });
      },
    },
    // ------------------------------------------------------------------ catalogue
    {
      chapitre: 'Catalogue des formations',
      dire: "Le catalogue présente l'offre de formation de l'institut, avec un accès direct aux fiches. L'entreprise y repère les programmes qui correspondent à ses besoins, en sécurité, en techniques opérationnelles ou en géosciences.",
      faire: async (a) => {
        await a.aller('/espace-entreprise/catalogue');
        await a.defiler(420, { duree: 1700 });
        await a.pause(600);
      },
    },
    // ------------------------------------------------------------------ demande
    {
      chapitre: 'Déposer une demande',
      dire: "Pour exprimer un besoin, l'entreprise choisit la nature de sa demande : un besoin de formation décrit librement, ou un devis pour une session sur mesure, avec l'effectif et les dates souhaitées.",
      faire: async (a) => {
        await accueil(a);
        await a.cliquer('text=Déposer une nouvelle demande');
        await a.patienter();
        await a.surligner('button:has-text("Devis / session sur mesure")', { duree: 1300 });
        await a.cliquer('button:has-text("Besoin de formation")');
        await a.taper('#secteur', 'Production d’énergie', { delai: 28 });
        await a.taper('#adresse', 'Avenue Cheikh Anta Diop, Dakar', { delai: 24 });
        await a.taper('#telephone', '+221 77 000 03 01', { delai: 28 });
      },
    },
    {
      dire: "Elle précise la formation visée, ou aucune en particulier, et décrit son besoin. La demande est transmise à l'équipe pédagogique, et l'entreprise en suit le statut : soumise, en cours, répondue, puis clôturée.",
      faire: async (a) => {
        await a.choisir('#formation', { index: 1 });
        await a.taper('#description', "Mise à niveau en détection de gaz pour douze opérateurs de nos installations, avant l'arrêt programmé du deuxième trimestre.", { delai: 20 });
        await a.cliquer('button[type=submit]:has-text("Soumettre la demande")');
        await a.patienter(1500);
      },
    },
    {
      dire: "L'historique réunit toutes les demandes, avec leur date, leur statut et, lorsque l'équipe a répondu, sa réponse. L'entreprise peut aussi consulter la fiche technique de module préparée à son intention.",
      faire: async (a) => {
        await a.defiler(650, { duree: 1900 });
        await a.pause(800);
      },
    },
    // ------------------------------------------------------------------ messagerie
    {
      chapitre: "Échanges avec l'équipe",
      dire: "La messagerie permet de poser une question à l'équipe pédagogique sans quitter la plateforme : disponibilités des formateurs, précisions sur un devis, liste des participants.",
      faire: async (a) => {
        await accueil(a);
        await a.cliquer('text=Messagerie avec la pédagogie');
        await a.patienter();
        await a.defiler(300, { duree: 1200 });
        await a.taper('#message', "Bonjour, pouvez-vous nous confirmer la durée de la session de détection de gaz, et le nombre maximal de participants par groupe ?", { delai: 20 });
        await a.cliquer('button[type=submit]:has-text("Envoyer")');
        await a.patienter(1400);
      },
    },
    // ------------------------------------------------------------------ rendez-vous
    {
      chapitre: 'Rendez-vous',
      dire: "Enfin, l'entreprise peut prendre rendez-vous avec le responsable pédagogique ou avec l'admission, sur un créneau libre, en ligne ou en présentiel. La demande reste visible tant qu'elle n'a pas reçu de réponse.",
      faire: async (a) => {
        await accueil(a);
        await a.cliquer('text=Prendre rendez-vous');
        await a.patienter();
        await a.cliquer('button:has-text("Test Pedago")');
        await a.pause(900);
        await a.cliquer("button:has-text(\"jusqu'à\") >> nth=0");
        await a.pause(900);
        await a.taper('textarea', "Cadrer le calendrier et le budget de la session de formation du deuxième trimestre.", { delai: 22 });
        await a.cliquer('button:has-text("Envoyer la demande")');
        await a.patienter(1500);
      },
    },
  ],
  bilan: {
    titre: "Ce que l'entreprise peut faire",
    puces: [
      "Consulter l'offre de formation de l'institut",
      'Déposer un besoin de formation ou demander un devis',
      'Suivre le statut et la réponse de chaque demande',
      "Échanger avec l'équipe pédagogique",
      'Prendre rendez-vous sur un créneau libre',
    ],
    dire: "Voilà l'espace entreprise : un canal clair entre l'institut et ses partenaires, depuis le premier besoin exprimé jusqu'à la réponse de l'équipe.",
  },
};
