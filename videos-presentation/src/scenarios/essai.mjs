// Scénario d'essai technique : valide la chaîne (voix, sous-titres, curseur, chapitres) en ~1 minute.
import { seConnecter } from './_communs.mjs';

export default {
  id: 'essai',
  titre: 'Essai technique',
  depart: '/auth/login',
  ouverture: {
    pastille: 'Essai technique',
    titre: 'Responsable des admissions',
    sousTitre: 'Vérification de la chaîne de production vidéo',
    dire: "Bienvenue sur la plateforme de l'INPG, l'Institut National du Pétrole et du Gaz. Dans cet essai, nous découvrons l'espace du responsable des admissions.",
  },
  etapes: [
    {
      chapitre: 'Connexion',
      dire: "Pour accéder à son espace, le responsable des admissions saisit son adresse e-mail, puis son mot de passe.",
      faire: (a) => seConnecter(a, 'test.admission@inpg.sn', 'Test@2026'),
    },
    {
      chapitre: "Vue d'ensemble",
      dire: "Voici le tableau de bord. En un coup d'œil, on voit le nombre de candidatures, les sessions actives et le taux d'admission.",
      faire: async (a) => {
        await a.surligner('text=Candidatures >> nth=1', { duree: 1500 });
      },
    },
    {
      chapitre: 'Menu',
      dire: "Le menu de gauche donne accès aux sessions, aux candidatures, aux tests, aux entretiens et à la délibération.",
      faire: async (a) => {
        await a.cliquer('nav >> text=Candidatures');
        await a.patienter();
      },
    },
  ],
  bilan: {
    titre: 'Ce que vous avez vu',
    puces: ['Se connecter à son espace', 'Suivre les chiffres clés', 'Parcourir les candidatures'],
    dire: "Voilà pour cet essai. Merci de votre attention.",
  },
};
