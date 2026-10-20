// Vidéo 3 — Le responsable pédagogique : offre d'enseignement, scolarité, documents officiels, qualité.
// Compte : test.pedago@inpg.sn. Fil rouge : un étudiant (Elhadji Diouf) a terminé son programme ; le responsable lui délivre
// son certificat puis le fait passer côté alumni.
import { seConnecter, valeurOption } from './_communs.mjs';

const lien = (chemin) => `a[href="/pedagogie/${chemin}"]`;

export default {
  id: '03-pedagogie',
  titre: 'Espace pédagogique — offre de formation, scolarité, relevés et certificats',
  depart: '/auth/login',
  ouverture: {
    pastille: 'Responsable pédagogique',
    titre: "De l'offre de formation au certificat",
    sousTitre: 'Cours, promotions, étudiants, relevés de notes, certificats, qualité, entreprises et activités du personnel',
    dire: "Voici l'espace du responsable pédagogique, le chef d'orchestre de la formation. Il organise l'offre d'enseignement, suit la scolarité des étudiants, délivre les documents officiels et pilote la qualité.",
  },
  etapes: [
    // ------------------------------------------------------------------ vue d'ensemble
    {
      chapitre: "Vue d'ensemble",
      dire: "Après sa connexion, le responsable arrive sur la vue d'ensemble de l'offre de formation : le nombre de cours, de formations, de promotions et de ressources, avec des points d'attention.",
      faire: async (a) => {
        await seConnecter(a, 'test.pedago@inpg.sn', 'Test@2026');
        await a.surligner('text=Points d’attention', { duree: 1800 });
        await a.defiler(420, { duree: 1600 });
        await a.pause(500);
      },
    },
    {
      dire: "Ces alertes signalent ce qui ferait disparaître un écran chez un formateur ou un étudiant : un cours sans formateur, un cours hors programme, une promotion sans cours. Chacune renvoie vers l'endroit où la corriger.",
      faire: async (a) => {
        await a.surligner('text=Cours sans formateur', { duree: 1800, projecteur: true });
      },
    },
    // ------------------------------------------------------------------ offre d'enseignement
    {
      chapitre: "Offre d'enseignement",
      dire: "Les cours portent un code, un volume horaire, un coefficient et leurs formateurs. Le coefficient est une valeur de la maquette pédagogique : c'est lui qui pondère la moyenne générale de l'étudiant.",
      faire: async (a) => {
        await a.cliquer(lien('cours'));
        await a.patienter();
        await a.defiler(300, { duree: 1400 });
        await a.pause(500);
      },
    },
    {
      dire: "Les formations regroupent les cours de leur programme, et les promotions suivent la formation qui leur est rattachée. Un cours se rattache à une formation, jamais directement à une promotion.",
      faire: async (a) => {
        await a.cliquer(lien('formations'));
        await a.patienter();
        await a.cliquer('button:has-text("Actives")');
        await a.pause(1200);
        await a.cliquer(lien('promotions'));
        await a.patienter();
        await a.defiler(250, { duree: 1200 });
      },
    },
    // ------------------------------------------------------------------ étudiants
    {
      chapitre: 'Scolarité des étudiants',
      dire: "Le répertoire des étudiants indique, pour chacun, sa promotion, son matricule, sa moyenne sur cent avec l'équivalent sur vingt, et son statut de suivi : actif, à risque ou sans note.",
      faire: async (a) => {
        await a.cliquer(lien('etudiants'));
        await a.patienter();
        await a.defiler(350, { duree: 1600 });
        await a.pause(600);
      },
    },
    {
      dire: "Les étudiants se créent un par un, ou par import d'un fichier. Un candidat admis arrive ici sans promotion : c'est l'affectation d'une promotion qui achève son inscription et lui attribue son matricule.",
      faire: async (a) => {
        await a.defiler(-350, { duree: 800 });
        await a.surligner('button:has-text("Importer")', { duree: 1500 });
        await a.surligner('button:has-text("Nouveau compte")', { duree: 1500 });
      },
    },
    // ------------------------------------------------------------------ relevés
    {
      chapitre: 'Relevés de notes',
      dire: "Un relevé de notes se délivre pour une période, à un étudiant, à plusieurs, ou à toute une promotion. Le relevé délivré est figé : il ne changera plus, même si un formateur corrige une note ensuite.",
      faire: async (a) => {
        await a.cliquer(lien('releves'));
        await a.patienter();
        await a.cliquer('button:has-text("Délivrer des relevés")');
        await a.pause(700);
        await a.taper('input[type=text]', 'Semestre 2 — 2026', { delai: 35 });
        await a.choisir('select', await valeurOption(a, 'select', 'Master Pétrole'));
      },
    },
    {
      dire: "Le relevé courant, recalculé à l'instant, reste consultable avant de délivrer. Une fois délivré, l'étudiant et l'ancien étudiant le retrouvent dans leur espace, avec un PDF à télécharger.",
      faire: async (a) => {
        await a.cliquer('button:has-text("Délivrer")  >> nth=-1');
        await a.patienter(1800);
        await a.defiler(500, { duree: 1600 });
      },
    },
    // ------------------------------------------------------------------ certificats
    {
      chapitre: 'Certificats',
      dire: "Le certificat de formation n'est délivré qu'à un étudiant qui a réellement terminé son programme : tous les supports consultés et au moins une note dans chaque cours. La plateforme refuse sinon, et dit ce qui manque.",
      faire: async (a) => {
        await a.cliquer(lien('certificats'));
        await a.patienter();
        await a.cliquer('button:has-text("Délivrer un certificat")');
        await a.pause(700);
        const selEtudiant = 'select:has-text("Choisissez un étudiant")';
        await a.choisir(selEtudiant, await valeurOption(a, selEtudiant, 'Elhadji'));
        await a.choisir('select:has-text("Monsieur")', { label: 'Monsieur' });
        await a.taper('input[type=date] >> nth=0', '1997-08-21');
      },
    },
    {
      dire: "Le certificat reprend le modèle officiel de l'institut : le nom, la formation, la période et un code. La date de délivrance est celle du jour, posée par le serveur, et un code de vérification permet de contrôler le document.",
      faire: async (a) => {
        await a.taper('input[type=text] >> nth=0', 'Master Pétrole & Gaz', { delai: 30 });
        await a.taper('input[type=date] >> nth=1', '2026-01-12');
        await a.taper('input[type=date] >> nth=2', '2026-10-09');
        await a.cliquer('button:has-text("Délivrer") >> nth=-1');
        await a.patienter(2000);
      },
    },
    {
      dire: "Le certificat est signé numériquement, et porte un code de vérification imprimé avec son numéro. Toute personne peut le contrôler sur la page publique de l'institut, sans compte, en saisissant ce numéro.",
      faire: async (a) => {
        await a.surligner('text=Elhadji Diouf', { duree: 1800 });
      },
    },
    {
      dire: "Un étudiant dont le programme est achevé peut alors être marqué alumni : son compte change de profil, et il retrouve son relevé et son parcours dans l'espace des anciens.",
      faire: async (a) => {
        await a.cliquer(lien('etudiants'));
        await a.patienter();
        await a.surligner('tr:has-text("Elhadji Diouf") button:has-text("Marquer alumni")', { duree: 1800 });
      },
    },
    // ------------------------------------------------------------------ qualité
    {
      chapitre: 'Suivi de la qualité',
      dire: "Le suivi qualité rassemble les avis des étudiants sur les cours : note moyenne, nombre d'avis, cours évalués, et cours à surveiller. Les cours les moins bien notés apparaissent en premier, avec les commentaires.",
      faire: async (a) => {
        await a.cliquer(lien('qualite'));
        await a.patienter();
        await a.surligner('text=À surveiller', { duree: 1700 });
        await a.defiler(520, { duree: 1900 });
        await a.pause(700);
      },
    },
    // ------------------------------------------------------------------ entreprises
    {
      chapitre: 'Demandes des entreprises',
      dire: "Les entreprises partenaires déposent leurs besoins de formation ou demandent un devis. Le responsable lit la demande, y répond, et change son statut, de soumise à répondue puis clôturée.",
      faire: async (a) => {
        await a.cliquer(lien('entreprises'));
        await a.patienter();
        await a.cliquer('text=Teranga Pétrole');
        await a.pause(900);
        await a.choisir('select >> nth=0', { index: 1 });
        await a.taper('textarea >> nth=0', "Merci pour votre demande. Nous vous proposons deux sessions de trois jours en février ; un devis détaillé vous parvient sous quarante-huit heures.", { delai: 22 });
        await a.cliquer('button:has-text("Enregistrer la réponse")');
        await a.patienter(1400);
      },
    },
    // ------------------------------------------------------------------ activités
    {
      chapitre: 'Activités du personnel',
      dire: "Chaque mois, le personnel saisit ses activités par département : formations, projets, recherches. Le responsable en tire un rapport par département ou un rapport global de l'institut.",
      faire: async (a) => {
        await a.cliquer('button:has-text("Fermer"), button[aria-label="Fermer"], button:has-text("close")');
        await a.cliquer(lien('activites-personnel'));
        await a.patienter();
        await a.cliquer('button:has-text("Nouvelle activité")');
        await a.pause(600);
        await a.choisir('select >> nth=3', { index: 1 });
        await a.choisir('select >> nth=4', { index: 1 });
        await a.taper('input[type=text] >> nth=-1', 'Atelier de pédagogie active pour les formateurs vacataires', { delai: 25 });
      },
    },
  ],
  bilan: {
    titre: 'Ce que le responsable pédagogique peut faire',
    puces: [
      "Organiser l'offre : cours, formations, promotions",
      'Inscrire et suivre les étudiants',
      'Délivrer des relevés de notes figés',
      'Délivrer des certificats signés et vérifiables',
      'Piloter la qualité à partir des avis des étudiants',
      'Répondre aux entreprises et suivre les activités du personnel',
    ],
    dire: "Voilà l'espace pédagogique : une vue complète de la scolarité, de l'offre de formation jusqu'aux documents officiels, avec des garde-fous qui signalent ce qui casserait un écran ailleurs.",
  },
};
