export interface User {
    id: number;
    username: string;
    email: string;
    password?: string;
    roles: string[];
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
