/** Friend/user as returned by GET /api/friend/list (UserDTO). */
export interface Friend {
  idUtente: string;
  username: string;
  nome: string;
  cognome: string;
  email?: string;
  ruolo: string;
  ruololabel?: string;
  attivo: boolean;
  telefono?: string;
  idReferente?: string;
  nomeReferente?: string;
  position?: number;
}

/** Payload for creating/updating a friend. */
export interface FriendInput {
  username: string;
  password?: string;
  nome: string;
  cognome: string;
  email?: string;
  ruolo: string;
  idReferente?: string;
  attivo: boolean;
}
