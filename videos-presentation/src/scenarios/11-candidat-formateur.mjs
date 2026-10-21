// Vidéo 10 — Le candidat formateur : répondre à une offre de recrutement, déposer son dossier, suivre son traitement.
// Une personne NOUVELLE s'inscrit en direct (Aminata Diallo), puis postule à l'offre de formateur en géosciences.
const DOC = '/work/donnees/documents';

export default {
  id: '11-candidat-formateur',
  titre: 'Candidat formateur — répondre à une offre de recrutement',
  depart: '/accueil',
  ouverture: {
    pastille: 'Candidat formateur',
    titre: 'Répondre à une offre de recrutement',
    sousTitre: 'Offres publiques, inscription, dépôt du dossier, pièces justificatives, suivi du traitement',
    dire: "Voici le parcours d'un professionnel qui souhaite enseigner à l'institut. Les offres de recrutement de formateurs sont publiques ; pour postuler, il crée un compte, dépose son dossier et suit son traitement en ligne.",
  },
  etapes: [
    // ------------------------------------------------------------------ offres
    {
      chapitre: 'Les offres de recrutement',
      dire: "La rubrique des offres de recrutement est ouverte à tous. Chaque offre précise le domaine, le lieu, le type de contrat, le nombre de postes, la date limite, les missions et le profil recherché.",
      faire: async (a) => {
        await a.aller('/offres-formateurs');
        await a.surligner('h2 >> nth=0', { duree: 1500 });
        await a.defiler(200, { duree: 1000 });
      },
    },
    // ------------------------------------------------------------------ inscription
    {
      chapitre: 'Création du compte',
      dire: "Pour postuler, le candidat doit être connecté. S'il n'a pas encore de compte, la plateforme l'envoie vers la connexion, d'où il peut s'inscrire en quelques secondes.",
      faire: async (a) => {
        await a.defiler(-200, { duree: 500 });
        await a.cliquer('button:has-text("Postuler") >> nth=1');
        await a.page.waitForURL(/\/auth\/login/);
        await a.patienter(600);
        await a.cliquer("text=S'inscrire >> nth=-1");
        await a.page.waitForURL(/\/auth\/register/);
        await a.patienter(500);
        await a.taper('#reg-prenom', 'Aminata', { delai: 34 });
        await a.taper('#reg-nom', 'Diallo', { delai: 34 });
        await a.taper('#reg-email', 'aminata.diallo@exemple.sn', { delai: 30 });
        await a.taper('#reg-password', 'Formateur@2026', { delai: 35 });
        await a.taper('#reg-confirm', 'Formateur@2026', { delai: 35 });
        await a.cliquer('button[type=submit]');
        await a.page.waitForURL((u) => !u.pathname.startsWith('/auth'), { timeout: 20000 });
        await a.patienter();
      },
    },
    // ------------------------------------------------------------------ dossier
    {
      chapitre: 'Dépôt du dossier',
      dire: "Une fois connecté, il retrouve l'offre et clique sur postuler. Le formulaire n'exige que l'identité et le téléphone. L'expérience, le diplôme principal et les autres rubriques sont facultatifs, mais un dossier plus complet est mieux instruit.",
      faire: async (a) => {
        await a.aller('/offres-formateurs');
        await a.cliquer('button:has-text("Postuler") >> nth=1');
        await a.page.waitForURL(/\/candidature-formateur/);
        await a.patienter();
        await a.taper('#nom', 'Diallo', { delai: 30 });
        await a.taper('#prenom', 'Aminata', { delai: 30 });
        await a.taper('#telephone', '+221 77 555 12 34', { delai: 28 });
        await a.taper('#anneesExperience', '8');
        await a.taper('#diplomePrincipal', 'Ingénieure géoscientifique — spécialité réservoirs', { delai: 26 });
      },
    },
    {
      dire: "Il peut compléter son dossier avec sa formation, ses expériences, ses langues, ses certifications et ses domaines d'expertise : autant d'éléments que le responsable de l'admission lira avant de décider.",
      faire: async (a) => {
        await a.cliquer('button:has-text("Compétences linguistiques")');
        await a.pause(1200);
        await a.cliquer('button:has-text("Domaines d\'expertise")');
        await a.pause(1200);
        await a.defiler(-0, { duree: 100 });
      },
    },
    {
      dire: "Le dépôt crée la candidature et l'accès devient celui d'un candidat formateur. Un message confirme la réception, et le suivi s'ouvre.",
      faire: async (a) => {
        await a.cliquer('button[type=submit]:has-text("Déposer ma candidature")');
        await a.patienter(1800);
      },
    },
    // ------------------------------------------------------------------ pièces
    {
      chapitre: 'Pièces et suivi',
      dire: "Deux pièces sont requises : le curriculum vitae et le diplôme. La lettre de motivation, l'attestation d'expérience et la pièce d'identité sont recommandées, sans bloquer le dépôt.",
      faire: async (a) => {
        await a.defiler(400, { duree: 1500 });
        await a.televerserViaBouton('li:has-text("Curriculum vitae") button[title="Ajouter"]', `${DOC}/cv.pdf`);
        await a.patienter(900);
        await a.televerserViaBouton('li:has-text("Diplôme") button[title="Ajouter"]', `${DOC}/diplome.pdf`);
        await a.patienter(900);
      },
    },
    {
      dire: "La frise du suivi montre l'avancement : dossier reçu, en cours de traitement, puis terminé. À l'issue, le responsable de l'admission retient ou non la candidature, avec un motif ; le candidat est informé de la décision.",
      faire: async (a) => {
        await a.defiler(-400, { duree: 1000 });
        await a.surligner('text=Dossier reçu', { duree: 2000 });
      },
    },
  ],
  bilan: {
    titre: 'Ce que le candidat formateur peut faire',
    puces: [
      'Consulter les offres de recrutement publiques',
      'Créer son compte puis postuler à une offre',
      'Déposer un dossier complet et ses pièces',
      'Suivre le traitement de sa candidature',
      'Être informé de la décision',
    ],
    dire: "Voilà le parcours du candidat formateur : une candidature entièrement en ligne, du premier clic sur une offre jusqu'à la décision de l'institut.",
  },
};
