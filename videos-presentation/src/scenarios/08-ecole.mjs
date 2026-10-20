// Vidéo 7 — L'école de formation : demander l'accréditation d'un programme, déposer son dossier, suivre la décision.
// Une école NOUVELLE s'inscrit en direct (Académie Sahélienne des Hydrocarbures). Le Centre Régional de Formation
// Pétrolière, accrédité d'avance par l'API réelle, sert à montrer l'aboutissement : attestation et liste publique.
import { readFileSync } from 'node:fs';
import { seConnecter, seDeconnecter } from './_communs.mjs';

const DOC = '/work/donnees/accreditation';

/** Références des documents signés, lues dans la base par preparer-donnees.sh (cache/references.json). */
const references = () => JSON.parse(readFileSync('/work/cache/references.json', 'utf8'));

export default {
  id: '08-ecole',
  titre: "Espace école de formation — obtenir l'accréditation d'un programme",
  depart: '/accueil',
  ouverture: {
    pastille: 'École de formation',
    titre: "Obtenir l'accréditation d'un programme",
    sousTitre: "Inscription, demande, dépôt des pièces, suivi de l'instruction, attestation",
    dire: "Voici le parcours d'une école de formation qui souhaite faire accréditer l'un de ses programmes par l'institut. De l'inscription au dépôt du dossier, puis jusqu'à l'attestation, tout se fait en ligne.",
  },
  etapes: [
    // ------------------------------------------------------------------ inscription
    {
      chapitre: "Inscription de l'école",
      dire: "L'école s'inscrit depuis le site public, en choisissant le profil École. Il suffit du nom de l'organisation, d'une adresse e-mail et d'un mot de passe.",
      faire: async (a) => {
        await a.cliquer("text=S'inscrire >> nth=0");
        await a.page.waitForURL(/\/auth\/register/);
        await a.patienter(500);
        await a.cliquer('button:has-text("École")');
        await a.taper('#reg-nom-organisation', 'Académie Sahélienne des Hydrocarbures', { delai: 32 });
        await a.taper('#reg-email', 'direction@ash-demo.sn', { delai: 32 });
        await a.taper('#reg-password', 'Ecole@2026', { delai: 35 });
        await a.taper('#reg-confirm', 'Ecole@2026', { delai: 35 });
        await a.cliquer('button[type=submit]');
        await a.page.waitForURL(/\/accreditation/, { timeout: 20000 });
        await a.patienter();
      },
    },
    // ------------------------------------------------------------------ demande
    {
      chapitre: "Demande d'accréditation",
      dire: "Elle arrive directement sur sa demande d'accréditation. L'école renseigne ses coordonnées, puis le programme visé : son intitulé, son niveau, le type de structure, l'effectif de la cohorte et sa date de début.",
      faire: async (a) => {
        await a.taper('#nomEcole', 'Académie Sahélienne des Hydrocarbures', { delai: 28 });
        await a.taper('#adresse', 'Route de Rufisque, Dakar', { delai: 28 });
        await a.taper('#telephone', '+221 77 400 12 34', { delai: 28 });
        await a.taper('#siteWeb', 'https://ash-demo.example.sn', { delai: 22 });
        await a.taper('#intituleProgramme', 'Licence professionnelle — Exploitation des installations gazières', { delai: 24 });
        await a.choisir('#niveauProgramme', { index: 3 });
        await a.choisir('#typeStructure', { index: 3 });
        await a.taper('#effectifCohorte', '35');
        await a.taper('#dateDebutCohorte', '2027-01-11');
      },
    },
    {
      dire: "En soumettant, l'école dépose sa demande. Un message l'informe que son dossier a bien été reçu, et que l'équipe de la direction de la qualité l'examinera prochainement.",
      faire: async (a) => {
        await a.cliquer('button[type=submit]:has-text("Soumettre la demande")');
        await a.patienter(1800);
      },
    },
    // ------------------------------------------------------------------ pièces
    {
      chapitre: 'Dépôt des pièces',
      dire: "Le dossier se complète par des pièces justificatives. Huit sont requises : le curriculum, la politique qualité, le programme de formation, les qualifications des formateurs, les équipements, l'engagement envers le référentiel, la preuve de reconnaissance et le rapport d'auto-évaluation.",
      faire: async (a) => {
        await a.surligner('text=Pièces du dossier', { duree: 1800 });
        await a.defiler(350, { duree: 1400 });
      },
    },
    {
      dire: "L'école ajoute chaque pièce depuis son ordinateur. La liste indique ce qui est fourni, ce qui manque, et permet de remplacer ou de supprimer un document tant que le dossier n'est pas en cours d'instruction.",
      faire: async (a) => {
        await a.televerserViaBouton('li:has-text("Curriculum") button[title="Ajouter"]', `${DOC}/curriculum.pdf`);
        await a.patienter(900);
        await a.televerserViaBouton('li:has-text("Politique qualité") button[title="Ajouter"]', `${DOC}/politique-qualite.pdf`);
        await a.patienter(900);
        await a.televerserViaBouton('li:has-text("Programme de formation") button[title="Ajouter"]', `${DOC}/programme-formation.pdf`);
        await a.patienter(900);
      },
    },
    {
      dire: "Tant qu'une pièce requise manque, la plateforme l'indique clairement. Les pièces recommandées, attendues à la visite du comité, ne bloquent pas le dépôt.",
      faire: async (a) => {
        await a.defiler(-350, { duree: 600 });
        await a.surligner('text=pièce(s) requise(s) manquante(s)', { duree: 1800 });
      },
    },
    // ------------------------------------------------------------------ issue : attestation
    {
      chapitre: 'Décision et attestation',
      dire: "Voyons maintenant l'aboutissement avec une autre école, dont le dossier a été instruit par l'évaluateur. Elle lit sa décision et retrouve la synthèse de l'évaluateur.",
      faire: async (a) => {
        await seDeconnecter(a);
        await seConnecter(a, 'direction@crfp-demo.sn', 'Demo@2026');
        await a.page.waitForURL(/\/accreditation/);
        await a.patienter();
        await a.surligner("text=Félicitations, ce programme est accrédité par l'INPG.", { duree: 2000 });
      },
    },
    {
      dire: "L'accréditation est valable trois ans. L'école télécharge son attestation en PDF : elle est signée numériquement et porte un code de vérification.",
      faire: async (a) => {
        await a.defiler(150, { duree: 900 });
        await a.surligner("button:has-text(\"Télécharger l'attestation d'accréditation\")", { duree: 2000 });
      },
    },
    {
      dire: "Toute personne peut vérifier une attestation sur la page publique de l'institut, sans compte, en saisissant ce code. Elle y voit l'école, le programme, la date d'expiration et la validité de la signature.",
      faire: async (a) => {
        await a.aller(`/verification-accreditation/${references().attestation}`);
        await a.pause(1200);
      },
    },
    {
      dire: "Enfin, les écoles accréditées figurent dans une liste publique, accessible depuis le site de l'institut : un repère pour les candidats comme pour les entreprises.",
      faire: async (a) => {
        await a.aller('/ecoles-accreditees');
        await a.pause(1500);
      },
    },
  ],
  bilan: {
    titre: "Ce que l'école de formation peut faire",
    puces: [
      "S'inscrire et déposer une demande d'accréditation",
      'Joindre les pièces requises et recommandées',
      "Suivre l'instruction, étape par étape",
      "Télécharger une attestation signée et vérifiable",
      'Figurer dans la liste publique des écoles accréditées',
    ],
    dire: "Voilà le parcours d'une école : un dossier suivi de bout en bout, une décision motivée et une attestation que n'importe qui peut vérifier.",
  },
};
