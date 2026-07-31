export interface MessageForum {
  auteur: string;
  initiales: string;
  contenu: string;
  date: string;
}

export interface SujetForum {
  id: number;
  auteur: string;
  initiales: string;
  couleurAvatar: string;
  couleurTexte: string;
  cours: string;
  module: string;
  titre: string;
  apercu: string;
  date: string;
  statut: 'nouveau' | 'urgent' | 'resolu';
  nbReponses: number;
  messages: MessageForum[];
}

export const SUJETS_FORUM_MOCK: SujetForum[] = [
  {
    id: 1,
    auteur: 'Alex Mercer',
    initiales: 'AM',
    couleurAvatar: 'bg-secondary-container',
    couleurTexte: 'text-on-secondary-container',
    cours: 'Macroéconomie Avancée',
    module: 'Module 3',
    titre: 'Difficulté variable de simulation',
    apercu: "J'ai une difficulté avec la variable de simulation dans le module 3, les résultats ne semblent pas converger...",
    date: 'il y a 14m',
    statut: 'nouveau',
    nbReponses: 3,
    messages: [
      {
        auteur: 'Alex Mercer',
        initiales: 'AM',
        date: 'il y a 14m',
        contenu: "Bonjour Professeur Sterling,\n\nJe rencontre une difficulté bloquante sur le TP de simulation du Module 3. Lorsque j'introduis la variable exogène de choc d'offre, le modèle de simulation refuse de converger, même après 500 itérations.\n\nEst-ce un problème lié au paramétrage de l'alpha ou bien une erreur dans l'implémentation de la fonction de coût ?\n\nMerci d'avance pour votre aide !"
      }
    ]
  },
  {
    id: 2,
    auteur: 'Lydia Chen',
    initiales: 'LC',
    couleurAvatar: 'bg-error-container/20',
    couleurTexte: 'text-error',
    cours: 'Finance Durable',
    module: 'Module 2',
    titre: 'Support PDF inaccessible',
    apercu: "Le support PDF du cours de Finance n'est pas téléchargeable, j'obtiens une erreur 404 lors du clic...",
    date: 'il y a 2h',
    statut: 'urgent',
    nbReponses: 1,
    messages: [
      {
        auteur: 'Lydia Chen',
        initiales: 'LC',
        date: 'il y a 2h',
        contenu: "Bonjour,\n\nLe support PDF du cours de Finance n'est pas téléchargeable, j'obtiens une erreur 404 lors du clic sur le lien de téléchargement.\n\nPourriez-vous vérifier ?"
      }
    ]
  },
  {
    id: 3,
    auteur: 'Jean Dupont',
    initiales: 'JD',
    couleurAvatar: 'bg-surface-container-highest',
    couleurTexte: 'text-on-surface-variant',
    cours: 'Analyse de Données',
    module: 'Projet final',
    titre: "Projet de fin d'année",
    apercu: "Bonjour Monsieur, pouvons-nous utiliser Python pour la partie visualisation du projet final ?",
    date: 'Hier',
    statut: 'resolu',
    nbReponses: 5,
    messages: [
      {
        auteur: 'Jean Dupont',
        initiales: 'JD',
        date: 'Hier',
        contenu: "Bonjour Monsieur,\n\nPouvons-nous utiliser Python pour la partie visualisation du projet final ?"
      }
    ]
  }
];