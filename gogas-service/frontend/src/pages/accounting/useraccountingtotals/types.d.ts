
interface User {
  email: string;
  enabled: boolean;
  firstName: string;
  friendReferral?: string;
  id: string;
  lastName: string;
  managedOrders: any[]
  password: string;
  phone?: string;
  position: number;
  role: string;
  roleEnum: string;
  username: string;
}

export interface UserAccountingTotal {
  idUtente: string;
  nome:  string;
  cognome: string;
  attivo: boolean;
  Saldo: number;
}