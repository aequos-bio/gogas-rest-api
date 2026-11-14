/** Order header (OrderDetailsDTO) — the subset the detail pages use. */
export interface OrderDetails {
  id: string;
  idtipoordine: string;
  tipoordine: string;
  dataconsegna: string;
  datachiusura?: string;
  idaequos?: number | null;
  editable: boolean;
  external: boolean;
  totaleCalcolato: boolean;
  speseTrasporto?: number;
  contabilizzato: boolean;
  numerofattura?: string;
  datafattura?: string;
  totalefattura?: number;
  evaso: boolean;
  datapagamento?: string;
  invioOrdineConsentito: boolean;
  invioPesiRichiesto: boolean;
  invioPesiConsentito: boolean;
  pesiInviati?: string;
  hasAttachment: boolean;
  inviato: boolean;
  numeroOrdineEsterno?: string;
  sincronizzato?: string;
  /** Order opening date (DD/MM/YYYY). */
  dataapertura?: string;
  /** Grand total of the order. */
  totaleOrdine?: number;
  /** Number of distinct ordering users. */
  numeroUtenti?: number;
  /** Number of distinct ordered products. */
  numeroProdotti?: number;
  /** Comma-separated available workflow actions for the current user. */
  actions?: string;
}

/** Row of the by-product summary grid (OrderByProductDTO). */
export interface OrderByProduct {
  idProdotto: string;
  nomeProdotto: string;
  categoria?: string;
  umProdotto: string;
  pesoCollo: number;
  prezzoKg: number;
  qta: number;
  numeroOrdinanti: number;
  qtaOrdinata: number;
  colliOrdinati: number;
  annullato: boolean;
  // derived
  totale: number;
  totaleConsegnato: number;
  totaleColli: number;
  totaleDiff: number;
  colliRisultanti: number;
  rimanenze: number;
}

/** Row of the product-detail grid (OrderItemByProductDTO). */
export interface OrderItemByProduct {
  idOrdine: string;
  idUtente: string;
  utente: string;
  qta: number;
  um: string;
  qtaRitirata: number;
  annullato: boolean;
}

export interface SelectItem {
  id: string;
  description: string;
}

/** Row of the by-user summary grid (OrderByUserDTO). */
export interface OrderByUser {
  IdUtente: string;
  NomeUtente: string;
  ArticoliOrdinati: number;
  TotaleNetto: number;
  CostoTrasporto: number;
  TotaleOrdine: number;
  InRosso: boolean;
}

/** Row of the per-user detail grid (OrderItemByUserDTO). */
export interface OrderItemByUser {
  idOrdine: string;
  name: string;
  um: string;
  qtaRitirata: number;
  tot?: number;
  annullato: boolean;
}
