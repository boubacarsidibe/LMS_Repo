// Vidéo 8 — L'évaluateur d'accréditation : référentiel, instruction d'un dossier, avis et décision.
// Compte : test.evaluateur@inpg.sn. Le dossier traité en direct est celui de l'Institut Africain des Hydrocarbures
// (dix pièces déposées, jamais instruit) ; le Centre Régional de Formation Pétrolière a été accrédité d'avance.
import { seConnecter } from './_communs.mjs';

const lien = (chemin) => `a[href="/evaluateur-accreditation/${chemin}"]`;

export default {
  id: '09-evaluateur',
  titre: "Espace évaluateur — instruire une demande d'accréditation",
  depart: '/auth/login',
  ouverture: {
    pastille: "Évaluateur d'accréditation",
    titre: "Instruire une demande d'accréditation",
    sousTitre: 'Tableau de bord, référentiel, examen du dossier, grille d’évaluation, avis et décision',
    dire: "Voici l'espace de l'évaluateur d'accréditation, au sein de la direction de la régulation et du contrôle qualité. Il y examine les dossiers déposés par les écoles et rend une décision motivée.",
  },
  etapes: [
    // ------------------------------------------------------------------ tableau de bord
    {
      chapitre: 'Tableau de bord',
      dire: "À la connexion, le tableau de bord indique combien de dossiers sont actifs, combien attendent l'avis de l'évaluateur, et combien il a déjà instruits.",
      faire: async (a) => {
        await seConnecter(a, 'test.evaluateur@inpg.sn', 'Test@2026');
        await a.surligner('text=Dossiers actifs', { duree: 1700 });
        await a.defiler(300, { duree: 1400 });
      },
    },
    {
      dire: "Plus bas, la conformité par section du référentiel est calculée sur les dossiers déjà instruits : elle montre où les écoles réussissent, et où elles peinent.",
      faire: async (a) => {
        await a.surligner('text=Conformité par section', { duree: 2000 });
        await a.defiler(-300, { duree: 700 });
      },
    },
    // ------------------------------------------------------------------ référentiel
    {
      chapitre: "Référentiel d'accréditation",
      dire: "Le référentiel rassemble les conditions que doit remplir un programme, réparties en quatre sections : management de la qualité, suivi des compétences, structure et contenu des programmes, admission des étudiants.",
      faire: async (a) => {
        await a.cliquer(lien('criteres'));
        await a.patienter();
        await a.cliquer('button:has-text("Tout déplier")');
        await a.pause(900);
        await a.defiler(450, { duree: 1800 });
      },
    },
    {
      dire: "Chaque condition active apparaît dans la grille d'évaluation d'un dossier. L'évaluateur peut en ajouter, les désactiver ou les réactiver, sans jamais effacer l'historique des dossiers déjà jugés.",
      faire: async (a) => {
        await a.defiler(-450, { duree: 700 });
        await a.surligner('button:has-text("Ajouter une condition")', { duree: 1700 });
      },
    },
    // ------------------------------------------------------------------ examen du dossier
    {
      chapitre: 'Examen du dossier',
      dire: "La liste des demandes affiche les dossiers selon leur étape. L'évaluateur ouvre celui d'une école : le programme visé, les coordonnées de l'établissement, et la liste des pièces fournies.",
      faire: async (a) => {
        await a.cliquer(lien('demandes'));
        await a.patienter();
        await a.surligner('text=À traiter', { duree: 1500 });
        await a.cliquer('li:has-text("Techniques de production pétrolière")');
        await a.pause(1200);
        await a.defiler(520, { duree: 1900 });
      },
    },
    {
      dire: "Huit pièces sont requises et quatre recommandées pour la visite du comité. La plateforme indique si le dossier est complet, et chaque pièce se consulte d'un clic.",
      faire: async (a) => {
        await a.surligner('text=Requis : complet', { duree: 2000 });
      },
    },
    {
      dire: "Pour commencer l'instruction, l'évaluateur prend le dossier en charge. L'école en est informée par courriel, et le dossier passe en cours d'évaluation.",
      faire: async (a) => {
        await a.defiler(300, { duree: 1200 });
        await a.cliquer('button:has-text("Prendre en charge")');
        await a.patienter(1500);
      },
    },
    // ------------------------------------------------------------------ grille d'évaluation
    {
      chapitre: "Grille d'évaluation",
      dire: "La grille applique le référentiel au dossier : pour chaque condition, l'évaluateur juge « conforme », « non conforme » ou « sans objet », et peut noter une observation.",
      faire: async (a) => {
        await a.surligner("text=Grille d'évaluation", { duree: 1500 });
        await a.cliquer('button:text-is("Conforme") >> nth=0');
        await a.cliquer('button:text-is("Conforme") >> nth=1');
        await a.taper('input[placeholder="Observation (facultative)"] >> nth=2', 'Politique qualité signée et révisée en 2026.', { delai: 24 });
        await a.cliquer('button:text-is("Conforme") >> nth=2');
      },
    },
    {
      dire: "Un compteur indique l'avancement de l'évaluation, section par section. Les trente et une conditions sont jugées ici de la même manière.",
      faire: async (a) => {
        const conformes = a.page.locator('button:text-is("Conforme")');
        const n = await conformes.count();
        for (let i = 3; i < n; i++) {
          await conformes.nth(i).click();
          await a.pause(120);
        }
        await a.page.locator("text=Appliqué à").first().scrollIntoViewIfNeeded().catch(() => {});
        await a.pause(1400);
      },
    },
    // ------------------------------------------------------------------ avis et décision
    {
      chapitre: 'Avis et décision',
      dire: "Chaque évaluateur dépose son avis, favorable ou défavorable, avec une motivation. Plusieurs évaluateurs peuvent se prononcer, chacun indépendamment, et leurs avis restent visibles en interne.",
      faire: async (a) => {
        await a.cliquer('label:has-text("Favorable")');
        await a.taper('textarea[name=avisCommentaire]', "Dossier complet et cohérent. L'équipe pédagogique est qualifiée ; deux points restent à consolider dans l'année.", { delai: 20 });
        await a.cliquer('button:has-text("Envoyer mon avis")');
        await a.patienter(1500);
      },
    },
    {
      dire: "La décision est possible dès qu'un avis est déposé : accréditation pleine, accréditation sous réserve, ou refus. Sous réserve, un plan d'actions est obligatoire : l'école doit le mettre en œuvre pendant la période d'accréditation.",
      faire: async (a) => {
        await a.cliquer('button:has-text("Accréditer sous réserve")');
        await a.pause(700);
        await a.taper('#decision-plan-actions', "Recruter deux formateurs supplémentaires avant la rentrée et formaliser le suivi des diplômés dans les six mois.", { delai: 20 });
        await a.taper('#decision-synthese', 'Programme solide, accrédité sous réserve de la mise en œuvre du plan d’actions.', { delai: 22 });
      },
    },
    {
      dire: "Une fois la décision confirmée, l'école est informée, une attestation signée numériquement est générée, et le dossier rejoint la liste des programmes accrédités.",
      faire: async (a) => {
        await a.cliquer('button:has-text("Confirmer")');
        await a.patienter(2000);
        await a.surligner('p.font-label-md:has-text("Accréditée sous réserve")', { duree: 2000 });
      },
    },
  ],
  bilan: {
    titre: "Ce que l'évaluateur peut faire",
    puces: [
      'Suivre les dossiers par étape et par priorité',
      'Consulter le référentiel et le faire évoluer',
      'Examiner le dossier et chacune de ses pièces',
      "Juger chaque condition dans une grille d'évaluation",
      'Déposer son avis, puis décider : accréditer, sous réserve, ou refuser',
      "Générer une attestation signée, vérifiable par tous",
    ],
    dire: "Voilà l'espace de l'évaluateur : une instruction rigoureuse, conforme au référentiel, qui laisse une trace de chaque jugement et aboutit à une décision motivée.",
  },
};
