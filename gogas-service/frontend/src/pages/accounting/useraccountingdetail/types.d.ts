export interface UserMovementView {
  codicecausale: string;
  data: string;
  descrizione: string;
  friendReferralId?: string;
  id: string;
  idutente: string;
  importo: number;
  nomecausale: string;
  nomeutente: string;
  segno: '+' | '-';
}

export interface UserMovement {
  id?: string;
  data: string;
  idutente: string;
  nomeutente: string;
  codicecausale: string;
  nomecausale: string;
  descrizione: string;
  importo: number;
}

export interface UserTransaction {
  amount: number;
  date: string; // 2022-06-18
  description: string;
  friend?: string;
  id: string;
  reason: string;
  recorded: boolean;
  sign: '+' | '-';
  type: string;
  userId: string;
  saldo: number;
}