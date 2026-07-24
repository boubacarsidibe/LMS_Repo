import { Role } from "./utils";

export interface User {
    id: number;
    username: string;
    email: string;
    password: string;
    roles: Role[];
}
