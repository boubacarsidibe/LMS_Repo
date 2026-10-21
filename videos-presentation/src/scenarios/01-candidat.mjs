// Vidéo 1 — Le candidat : de la découverte des formations au suivi de sa candidature.
import { seConnecter, seDeconnecter } from './_communs.mjs';

const DOC = '/work/donnees/documents';

export default {
  id: '01-candidat',
  titre: 'Espace candidat — du premier clic au suivi de la candidature',
  depart: '/accueil',
  ouverture: {
    pastille: 'Espace candidat',
    titre: 'Du premier clic au suivi de sa candidature',
    sousTitre: 'Découvrir les formations, créer son compte, déposer son dossier, régler les frais et suivre chaque étape',
    dire: "Bienvenue sur la plateforme de l'INPG, l'Institut National du Pétrole et du Gaz. Dans cette vidéo, nous suivons le parcours d'un candidat, de la découverte des formations jusqu'au suivi de sa candidature.",
  },
  etapes: [
    // ------------------------------------------------------------------ accueil
    {
      chapitre: "Page d'accueil",
      dire: "Tout commence sur la page d'accueil publique de l'institut. Le visiteur y découvre l'offre de formation et les chiffres clés, et peut, en un clic, postuler ou se connecter.",
      faire: async (a) => {
        await a.surligner('text=Découvrir les formations', { duree: 1800 });
        await a.defiler(520, { duree: 1600 });
        await a.pause(900);
        await a.defiler(-520, { duree: 900 });
      },
    },
    // ------------------------------------------------------------------ catalogue
    {
      chapitre: 'Catalogue des formations',
      dire: "Le catalogue réunit toutes les formations. Des filtres par niveau et par domaine, ainsi qu'une recherche par mots-clés, aident à trouver rapidement le bon programme.",
      faire: async (a) => {
        await a.cliquer('nav >> text=Formations');
        await a.patienter();
        await a.surligner('h3:has-text("Filtres")', { duree: 1600, marge: 14 });
        await a.cliquer('label:has-text("Certificat") >> nth=0');
        await a.pause(1200);
        await a.cliquer('label:has-text("Certificat") >> nth=0');
      },
    },
    {
      dire: "La fiche détaillée d'une formation présente son contenu, sa durée et les conditions d'accès.",
      faire: async (a) => {
        await a.cliquer('article >> nth=0');
        await a.patienter();
        await a.defiler(420, { duree: 1400 });
        await a.pause(800);
      },
    },
    // ------------------------------------------------------------------ sessions
    {
      chapitre: 'Sessions de candidature',
      dire: "Les sessions de candidature ouvertes sont affichées avec leur date de clôture. Pour postuler, il faut d'abord créer son compte.",
      faire: async (a) => {
        await a.cliquer('nav >> text=Sessions ouvertes');
        await a.patienter();
        await a.surligner('text=Postuler à cette session', { duree: 1800 });
      },
    },
    // ------------------------------------------------------------------ compte
    {
      chapitre: 'Création du compte',
      dire: "La création du compte est immédiate : un prénom, un nom, une adresse e-mail et un mot de passe suffisent.",
      faire: async (a) => {
        await a.cliquer("text=S'inscrire >> nth=0");
        await a.page.waitForURL(/\/auth\/register/);
        await a.patienter(500);
        await a.taper('#reg-prenom', 'Fatou');
        await a.taper('#reg-nom', 'Sène');
        await a.taper('#reg-email', 'fatou.sene@exemple.sn');
        await a.taper('#reg-password', 'Candidat@2026', { delai: 35 });
        await a.taper('#reg-confirm', 'Candidat@2026', { delai: 35 });
        await a.cliquer('button[type=submit]');
        await a.page.waitForURL(/\/recrutement\/sessions/, { timeout: 20000 });
        await a.patienter();
      },
    },
    // ------------------------------------------------------------------ dossier
    {
      chapitre: 'Dépôt du dossier',
      dire: "Le candidat choisit sa session et dépose son dossier. Le formulaire se déroule en trois étapes. La première concerne son profil : identité, coordonnées et situation professionnelle.",
      faire: async (a) => {
        await a.cliquer('text=Postuler à cette session');
        await a.page.waitForURL(/\/recrutement\/candidature/);
        await a.patienter();
        await a.taper('#cand-nom', 'Sène');
        await a.taper('#cand-prenom', 'Fatou');
        await a.taper('#cand-email', 'fatou.sene@exemple.sn');
        await a.taper('#cand-telephone', '77 123 45 67');
        await a.taper('#cand-date-naissance', '1996-05-14');
        await a.choisir('#cand-nationalite', { index: 1 });
        await a.choisir('#cand-genre', { index: 2 });
        await a.taper('#cand-adresse', 'Point E, Dakar');
      },
    },
    {
      dire: "La deuxième étape décrit la session visée et le parcours académique du candidat : niveau d'études, établissement et diplôme.",
      faire: async (a) => {
        await a.cliquer('button:has-text("Suivant")');
        await a.pause(600);
        await a.choisir('#cand-session', { index: 1 });
        await a.cliquer('label:has-text("Master 1 (M1)")');
        await a.taper('#cand-etablissement', 'Université Cheikh Anta Diop');
        await a.taper('#cand-diplome', 'Master en géosciences');
        await a.taper('#cand-annee-obtention', '2020');
      },
    },
    {
      dire: "Enfin, les pièces justificatives : pièce d'identité, curriculum vitae, relevé de notes avec la moyenne obtenue, et diplôme. Un candidat qui n'a pas terminé peut revenir plus tard : son brouillon est enregistré automatiquement.",
      faire: async (a) => {
        await a.cliquer('button:has-text("Suivant")');
        await a.pause(700);
        const fichiers = a.page.locator('input[type=file]');
        await a.televerser(fichiers.nth(0), `${DOC}/piece-identite.pdf`);
        await a.televerser(fichiers.nth(1), `${DOC}/cv.pdf`);
        await a.taper('#bulletin-moyenne-0-0', '14.5');
        await a.televerser(fichiers.nth(2), `${DOC}/releve-notes.pdf`);
        await a.televerser(fichiers.nth(3), `${DOC}/diplome.pdf`);
        await a.taper('#diplome-description-0', 'Master en géosciences, Université Cheikh Anta Diop, 2020');
      },
    },
    {
      dire: "Après avoir accepté les conditions, le candidat soumet son dossier.",
      faire: async (a) => {
        await a.cliquer('input[type=checkbox]');
        await a.pause(500);
        await a.cliquer('button:has-text("Soumettre la candidature")');
        await a.page.waitForURL(/\/recrutement\/suivi/, { timeout: 30000 });
        await a.patienter(1200);
      },
    },
    // ------------------------------------------------------------------ suivi
    {
      chapitre: 'Suivi de la candidature',
      dire: "Dès la soumission, le candidat accède à son espace de suivi : un numéro de dossier, la frise du parcours, et la liste des documents avec leur état de vérification.",
      faire: async (a) => {
        await a.surligner('text=Dossier >> nth=0', { duree: 1500 });
        await a.defiler(420, { duree: 1500 });
        await a.pause(1200);
        await a.defiler(-420, { duree: 900 });
      },
    },
    // ------------------------------------------------------------------ paiement
    {
      chapitre: 'Paiement des frais de dossier',
      dire: "Voyons la suite avec un autre candidat, dont le dossier a été validé par les admissions. Il est présélectionné, et l'on lui demande de régler ses frais de dossier.",
      faire: async (a) => {
        await seDeconnecter(a);
        await seConnecter(a, 'ousmane.ba@inpg-demo.sn', 'Demo@2026');
        await a.page.goto('http://localhost:4200/recrutement/suivi');
        await a.patienter();
        await a.surligner('text=Payer maintenant', { duree: 1800 });
      },
    },
    {
      dire: "Le candidat choisit son moyen de règlement, mobile money ou virement bancaire, puis déclare son paiement. L'équipe des admissions vérifie ensuite la transaction avant de poursuivre le dossier.",
      faire: async (a) => {
        await a.cliquer('text=Payer maintenant');
        await a.page.waitForURL(/paiement/);
        await a.patienter();
        await a.cliquer('label:has(input[name=operator]):has-text("Orange")');
        await a.pause(900);
        await a.cliquer('button:has-text("Déclarer ce règlement")');
        await a.patienter(1500);
      },
    },
    // ------------------------------------------------------------------ convocations
    {
      chapitre: 'Test, entretien et décision',
      dire: "Plus loin dans le parcours, le candidat est convoqué à l'entretien. Il retrouve la date, l'heure et le lieu, télécharge sa convocation et confirme sa présence ou demande un report.",
      faire: async (a) => {
        await seDeconnecter(a);
        await seConnecter(a, 'abdoulaye.fall@inpg-demo.sn', 'Demo@2026');
        await a.page.goto('http://localhost:4200/recrutement/suivi');
        await a.patienter();
        await a.defiler(300, { duree: 1400 });
        await a.pause(1200);
      },
    },
    {
      dire: "Et lorsque le jury l'admet, le candidat reçoit sa lettre d'admission et découvre les prochaines étapes : frais de scolarité, carte d'étudiant et date de la rentrée.",
      faire: async (a) => {
        await seDeconnecter(a);
        await seConnecter(a, 'rama.kane@inpg-demo.sn', 'Demo@2026');
        await a.page.goto('http://localhost:4200/recrutement/suivi');
        await a.patienter();
        await a.surligner('text=Lettre d\'Admission', { duree: 1800 });
        await a.defiler(520, { duree: 1500 });
        await a.pause(1200);
      },
    },
  ],
  bilan: {
    titre: 'Ce que le candidat peut faire',
    puces: [
      'Découvrir le catalogue et les sessions ouvertes',
      'Créer son compte en quelques secondes',
      'Déposer son dossier en trois étapes, avec ses pièces',
      'Suivre chaque étape du parcours en temps réel',
      'Déclarer le paiement des frais de dossier',
      "Recevoir convocations, décision et lettre d'admission",
    ],
    dire: "Voilà le parcours du candidat : une candidature entièrement en ligne, suivie à chaque étape, de la création du compte jusqu'à l'admission.",
  },
};
