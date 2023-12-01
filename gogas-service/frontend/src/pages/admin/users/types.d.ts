export interface User {
  attivo: boolean;
  cognome: string;
  email: string;
  idReferente?: string;
  idUtente: string;
  nome: string;
  nomeReferente?: string;
  password?: string;
  position: number;
  ruolo: string;
  ruololabel: string;
  telefono?: string;
  username: string;
}

export interface Friend {
  id: string;
  description: string;
  attivo: boolean;
}

