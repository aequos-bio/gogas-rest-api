export interface GasMovementView {
  id?: string;
  codicecausale?: string;
  codicecontabile?: string;
  data: string;
  descrizione: string;
  importo: number;
  nomecausale: string;
  segnocausale: '+' | '-';
  type: number;
}

export interface GasMovement {
  id?: string;
  data: string;
  codicecausale?: string;
  descrizione: string;
  importo: number;
}