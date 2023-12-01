export interface UserMovementView {  // usato nel dialogo di modifica (view)
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

export interface UserMovement { // usato nel dialogo di modifica (insert/edit)
  id?: string;
  data: string;
  idutente: string;
  nomeutente: string;
  codicecausale: string;
  nomecausale: string;
  descrizione: string;
  importo: number;
}

export interface UserTransaction { // usato nella lista
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