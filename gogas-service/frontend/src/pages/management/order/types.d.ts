export interface OrderDetail {
  id: string;
  editable: boolean;
  external: boolean;
  hasAttachment: boolean;
  idtipoordine: string;
  tipoordine: string;
  dataconsegna: string;
  idaequos?: number;
  totaleCalcolato: boolean;
  speseTrasporto: number;
  contabilizzato: boolean;
  numerofattura?: string;
  datafattura?: string;
  totalefattura?: number;
  evaso: boolean;
  datapagamento?: string;
  invioPesiRichiesto: boolean;
  invioPesiConsentito: boolean;
  pesiInviati?: boolean;
  inviato: boolean;
  numeroOrdineEsterno?: string;
  sincronizzato?: string;
}

export interface OrderProduct {
  idProdotto: string;
  nomeProdotto: string;
  categoria: string;
  umProdotto: string;
  pesoCollo: number;
  prezzoKg: number;
  qta: number;
  numeroOrdinanti: number;
  qtaOrdinata: number;
  colliOrdinati: number;
  annullato: boolean;
  totale: number;
  totaleConsegnato: number;
  totaleColli: number;
  colliRisultanti: number;
  totaleDiff: number;
  rimanenze: number;
}