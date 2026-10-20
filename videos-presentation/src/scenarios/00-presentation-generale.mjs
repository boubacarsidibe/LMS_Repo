// Vidéo 0 — Présentation générale : la plateforme, ses profils et le fil rouge d'un étudiant, du candidat au diplômé.
import { readFileSync } from 'node:fs';

const references = () => JSON.parse(readFileSync('/work/cache/references.json', 'utf8'));

export default {
  id: '00-presentation-generale',
  titre: 'La plateforme LMS de l’INPG — vue d’ensemble',
  depart: '/accueil',
  ouverture: {
    pastille: 'Plateforme LMS',
    titre: 'Recruter, former, certifier, accréditer : une seule plateforme',
    sousTitre: "Vue d'ensemble des profils et du parcours, de la candidature au diplôme",
    dire: "Bienvenue. Cette vidéo présente la plateforme de l'Institut National du Pétrole et du Gaz : à qui elle s'adresse, ce qu'elle permet de faire, et comment un parcours la traverse, de la candidature au diplôme. Onze vidéos suivent : la première pour le candidat, puis une par profil.",
  },
  etapes: [
    {
      chapitre: 'Le site public',
      dire: "Tout commence par le site public de l'institut. Sans compte, on y découvre l'institut, le catalogue des formations et les offres de recrutement de formateurs, et on peut contacter l'équipe.",
      faire: async (a) => {
        await a.surligner('text=Découvrir les formations', { duree: 1700 });
        await a.defiler(520, { duree: 1700 });
        await a.pause(500);
        await a.defiler(-520, { duree: 800 });
      },
    },
    {
      chapitre: 'Dix profils',
      dire: "La plateforme répond à dix profils, chacun avec son espace : le candidat, le responsable des admissions, l'étudiant, le formateur, le responsable pédagogique, l'alumni, l'école de formation, l'évaluateur d'accréditation, l'entreprise, et le candidat formateur.",
      faire: async (a) => {
        await a.carte({
          pastille: 'Les profils',
          titre: 'Dix profils, un seul parcours',
          puces: [
            'Candidat — postuler et suivre son dossier',
            'Responsable des admissions — instruire et décider',
            'Étudiant — suivre sa formation',
            'Formateur — enseigner et suivre ses étudiants',
            'Responsable pédagogique — scolarité et documents officiels',
            'Alumni — rester en lien après le diplôme',
            'École de formation — demander une accréditation',
            "Évaluateur — instruire les demandes d'accréditation",
            'Entreprise — exprimer un besoin de formation',
            'Candidat formateur — répondre aux offres',
          ],
        }, { duree: 6500 });
        await a.retirerCarte();
      },
    },
    {
      chapitre: 'Le catalogue',
      dire: "Le catalogue présente les formations, avec des filtres par niveau et par domaine. Chaque fiche détaille les programmes inclus et la méthode pédagogique : le visiteur sait exactement ce qu'il peut attendre avant de postuler.",
      faire: async (a) => {
        await a.aller('/recrutement/catalogue/5');
        await a.defiler(520, { duree: 2000 });
        await a.pause(600);
      },
    },
    {
      chapitre: 'Offres de recrutement',
      dire: "Les offres de recrutement de formateurs sont publiques : chacune précise le domaine, le contrat, le nombre de postes et le profil recherché. Pour postuler, il faut un compte. La liste des écoles dont un programme est accrédité est, elle, réservée aux comptes connectés.",
      faire: async (a) => {
        await a.aller('/offres-formateurs');
        await a.pause(1800);
        await a.defiler(250, { duree: 1200 });
        await a.pause(800);
      },
    },
    {
      chapitre: 'Des documents vérifiables',
      dire: "Les certificats délivrés portent un code de vérification. Toute personne, sans compte, peut contrôler un certificat sur une page publique : le nom, la formation, la période, et la validité de la signature numérique. Un faux ne passe pas ce contrôle.",
      faire: async (a) => {
        await a.aller(`/verification-certificat/${references().certificat}`);
        await a.pause(2200);
      },
    },
    {
      chapitre: "Un parcours, du candidat au diplômé",
      dire: "Le parcours d'un étudiant traverse plusieurs espaces. Le candidat dépose son dossier. Le responsable des admissions l'instruit, confirme le paiement, fait passer les épreuves, puis le jury délibère. L'admis devient étudiant : la pédagogie l'inscrit, le formateur l'enseigne. Au terme de son programme, il reçoit son certificat, et devient alumni.",
      faire: async (a) => {
        await a.carte({
          pastille: 'Le parcours',
          titre: 'Du candidat au diplômé',
          puces: [
            'Candidature en ligne et pièces justificatives',
            'Instruction, paiement, tests et entretien',
            'Délibération et admission',
            "Inscription, promotion et matricule",
            'Cours, évaluations et notes sur cent',
            'Relevés figés et certificat signé',
            'Espace alumni et enquêtes de suivi',
          ],
        }, { duree: 7000 });
        await a.retirerCarte();
      },
    },
    {
      chapitre: "Accréditation et entreprises",
      dire: "En parallèle, deux autres parcours : une école de formation dépose un dossier d'accréditation, qu'un évaluateur instruit critère par critère jusqu'à une attestation signée ; et une entreprise exprime un besoin de formation et suit la réponse de l'équipe pédagogique.",
      faire: async (a) => {
        await a.carte({
          pastille: 'Deux parcours complémentaires',
          titre: 'Accréditer, former les entreprises',
          puces: [
            "École : demande, pièces, décision, attestation",
            'Évaluateur : grille de critères, avis, décision',
            'Entreprise : besoin ou devis, réponse, rendez-vous',
          ],
        }, { duree: 5500 });
        await a.retirerCarte();
      },
    },
  ],
  bilan: {
    titre: 'Une plateforme, de la candidature au diplôme',
    puces: [
      'Dix espaces adaptés à chaque profil',
      'Un parcours entièrement en ligne et tracé',
      'Des calculs automatiques là où ils sont possibles',
      'Des documents officiels signés et vérifiables',
      "Des données cloisonnées par rôle",
    ],
    dire: "Voilà la plateforme dans son ensemble. Les vidéos suivantes présentent, profil par profil, ce que chacun peut faire. Bon visionnage.",
  },
};
