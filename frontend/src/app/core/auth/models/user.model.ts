/**
 * Intention déclarée à l'inscription (EnumTypeInscription côté serveur). Elle
 * n'accorde aucun droit : les rôles métier ne sont acquis qu'à la première
 * soumission réelle.
 */
export type TypeInscription = 'CANDIDAT' | 'ECOLE' | 'ENTREPRISE' | 'FORMATEUR';

/** Comptes qui ne relèvent pas du parcours candidat/étudiant : chacun a son propre espace. */
export type ProfilCompte = 'ecole' | 'entreprise' | 'formateur';

export interface User {
    id: number;
    username: string;
    email: string;
    password?: string;
    roles: string[];
    typeInscription?: TypeInscription;
}

export interface candidat {
    dateDeNaissance: Date;
    nationalite: string;
    user: User;
}

export interface etudiant {
    dateDeNaissance: Date;
    nationalite: string;
    user: User;
    matricule: string;
}
