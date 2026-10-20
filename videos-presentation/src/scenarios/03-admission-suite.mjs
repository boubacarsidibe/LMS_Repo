// Vidéo 3 — Le responsable des admissions (2/2) : tests, entretiens, délibération, inscription, communications.
// Même compte et même jeu de données que la première partie. Chaque geste fait avancer un dossier précis.
import { seConnecter } from './_communs.mjs';

const lien = (chemin) => `a[href="/responsable-admission${chemin}"]`;
const dansJours = (n) => new Date(Date.now() + n * 86400000).toISOString().slice(0, 10);

export default {
  id: '03-admission-suite',
  titre: "Portail Admissions (2/2) — tests, entretiens, délibération et inscription",
  depart: '/auth/login',
  ouverture: {
    pastille: 'Responsable des admissions — 2 sur 2',
    titre: "Des tests à l'inscription : décider en connaissance de cause",
    sousTitre: 'Épreuves notées, entretiens, délibération classée par moyenne, inscription, communications',
    dire: "Voici la suite du portail des admissions. Les candidats présélectionnés passent des épreuves académiques puis un entretien ; le jury délibère sur un classement calculé, et le responsable finalise l'inscription des admis.",
  },
  etapes: [
    // ------------------------------------------------------------------ tests
    {
      chapitre: 'Tests académiques',
      dire: "Un test académique réunit une ou plusieurs épreuves, avec leur coefficient, leur date et leur lieu. Les notes se saisissent sur vingt, candidat par candidat. Ici, une candidate convoquée n'a pas encore sa note.",
      faire: async (a) => {
        await seConnecter(a, 'test.admission@inpg.sn', 'Test@2026');
        await a.cliquer(lien('/tests'));
        await a.patienter();
        await a.cliquer('a:has-text("Culture scientifique")');
        await a.patienter(900);
        await a.cliquer('a:has-text("Entrer les notes")');
        await a.page.waitForURL(/notes/);
        await a.patienter(900);
        await a.taper('tr:has-text("Fatoumata Sow") input[type=number]', '14', { delai: 90 });
      },
    },
    {
      dire: "Dès que toutes les épreuves d'un candidat sont notées, la plateforme calcule sa moyenne pondérée et la compare au seuil de réussite de la session. Le résultat se déduit : test réussi, échoué ou absent, sans geste supplémentaire.",
      faire: async (a) => {
        await a.cliquer('button:has-text("Enregistrer")');
        await a.patienter(1500);
        await a.aller('/responsable-admission/tests');
        await a.cliquer('a:has-text("Mathématiques appliquées")');
        await a.patienter(900);
        await a.cliquer('a:has-text("Entrer les notes")');
        await a.page.waitForURL(/notes/);
        await a.patienter(900);
        await a.taper('tr:has-text("Fatoumata Sow") input[type=number]', '15', { delai: 90 });
        await a.cliquer('button:has-text("Enregistrer")');
        await a.patienter(1500);
      },
    },
    {
      dire: "Dans la liste des candidatures, la candidate est maintenant « test réussi », sans que personne ait modifié son statut à la main.",
      faire: async (a) => {
        await a.cliquer(lien('/candidatures'));
        await a.patienter();
        await a.surligner('tr:has-text("Fatoumata Sow")', { duree: 2200 });
      },
    },
    // ------------------------------------------------------------------ entretiens
    {
      chapitre: 'Entretiens',
      dire: "Un candidat qui a réussi le test est admis à l'entretien d'un geste. Il est alors proposé à la convocation, avec les autres candidats déjà admis à cette étape.",
      faire: async (a) => {
        await a.cliquer('tr:has-text("Moussa Cissé")');
        await a.page.waitForURL(/candidatures\/\d+/);
        await a.patienter(900);
        await a.defiler(1500, { duree: 1400 });
        await a.cliquer("button:has-text(\"Admettre à l'entretien\")");
        await a.patienter(1700);
      },
    },
    {
      dire: "Le responsable convoque un groupe en choisissant les candidats, la date et l'heure, le lieu ou le lien de visioconférence. Chacun reçoit sa convocation en PDF et peut confirmer sa présence ou demander un report.",
      faire: async (a) => {
        await a.cliquer(lien('/entretiens'));
        await a.patienter();
        await a.cliquer('button:has-text("Convoquer un groupe")');
        await a.pause(700);
        await a.cliquer('label:has-text("Moussa Cissé")');
        await a.taper('input[type=datetime-local]', `${dansJours(6)}T10:00`);
        await a.taper('input[formcontrolname=lieuOuLien]', 'Immeuble Adja Koone — salle de jury', { delai: 28 });
      },
    },
    {
      dire: "Après l'entretien, le jury saisit une note sur vingt pour chaque candidat. Le dossier passe alors à « entretien effectué » et entre dans la délibération.",
      faire: async (a) => {
        await a.cliquer('button:has-text("Envoyer les convocations")');
        await a.patienter(1700);
        await a.cliquer('button:has-text("Abdoulaye Fall")');
        await a.page.waitForURL(/entretiens\/\d+/);
        await a.patienter(900);
        await a.cliquer('a:has-text("Saisir les notes")');
        await a.page.waitForURL(/notes/);
        await a.patienter(900);
        await a.taper('tr:has-text("Abdoulaye Fall") input[type=number]', '15', { delai: 90 });
        await a.cliquer('button:has-text("Enregistrer")');
        await a.patienter(1500);
      },
    },
    // ------------------------------------------------------------------ délibération
    {
      chapitre: 'Délibération',
      dire: "La délibération classe les candidats par moyenne : la note du test et celle de l'entretien, avec leurs coefficients. Le jury dispose ainsi d'un classement objectif, calculé, qu'il peut défendre.",
      faire: async (a) => {
        await a.cliquer(lien('/deliberation'));
        await a.patienter();
        await a.surligner('text=Rang', { duree: 1500 });
      },
    },
    {
      dire: "Pour chaque candidat, le jury décide : admis, mis en liste complémentaire s'il attend une place, ou non retenu. Chaque décision est horodatée, et reste corrigeable jusqu'à l'inscription.",
      faire: async (a) => {
        await a.cliquer('div.grid:has-text("Ndèye Gueye") button:has(span:text-is("check"))');
        await a.patienter(1500);
        await a.cliquer('div.grid:has-text("Pape Samb") button:has(span:text-is("hourglass_top"))');
        await a.patienter(1500);
      },
    },
    // ------------------------------------------------------------------ inscription
    {
      chapitre: "Inscription",
      dire: "Un candidat admis est inscrit par le responsable. Son compte candidat devient un compte étudiant, avec accès à l'espace étudiant ; la direction pédagogique lui affecte ensuite sa promotion, ce qui achève l'inscription et lui donne son matricule.",
      faire: async (a) => {
        await a.cliquer(lien('/candidatures'));
        await a.patienter();
        await a.taper('input[placeholder^="Nom, prénom"]', 'Rama Kane', { delai: 60 });
        await a.pause(900);
        await a.cliquer('tr:has-text("Rama Kane")');
        await a.page.waitForURL(/candidatures\/\d+/);
        await a.patienter(900);
        await a.defiler(1500, { duree: 1400 });
        await a.surligner("button:has-text(\"Finaliser l'inscription\")", { duree: 1700 });
        await a.cliquer("button:has-text(\"Finaliser l'inscription\")");
        await a.patienter(1800);
      },
    },
    // ------------------------------------------------------------------ communications
    {
      chapitre: 'Communications',
      dire: "Les courriels envoyés aux candidats suivent des modèles que le responsable personnalise, avec des variables comme le nom ou le numéro de dossier. Chaque modèle peut être activé ou désactivé.",
      faire: async (a) => {
        await a.cliquer(lien('/notifications'));
        await a.patienter();
        await a.defiler(520, { duree: 1900 });
        await a.pause(600);
      },
    },
  ],
  bilan: {
    titre: 'Ce que le responsable des admissions peut faire (2/2)',
    puces: [
      'Créer des épreuves et saisir les notes sur vingt',
      'Obtenir un résultat de test déduit automatiquement',
      'Convoquer les candidats aux entretiens et noter le jury',
      'Délibérer sur un classement calculé et horodaté',
      "Finaliser l'inscription des admis",
      'Personnaliser les courriels envoyés aux candidats',
    ],
    dire: "Voilà le parcours complet d'une admission : de la candidature à l'inscription, chaque étape est tracée, calculée quand elle peut l'être, et corrigeable tant que l'inscription n'est pas finalisée.",
  },
};
