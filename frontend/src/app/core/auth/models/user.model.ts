import { Role } from "./utils";

export interface User {
    id: number;
    username: string;
    email: string;
    password: string;
    roles: Role[];
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