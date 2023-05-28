export interface BalanceRow {
  data: string;
  descrizione: string;
  importo: number;
  contoDare?: string;
  contoAvere?: string;
  type: number;
}