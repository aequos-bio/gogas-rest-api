/** Select option returned by the backend SelectItemDTO ({ id, description }). */
export interface SelectItem {
  id: string;
  description: string;
}

/** Product as returned by GET /api/products/list/{typeId} (ProductDTO). */
export interface Product {
  id: string;
  descrizione: string;
  unitamisura: string;
  prezzounitario: number;
  unitamisuracollo?: string;
  pesocassa: number;
  cadenza?: string;
  codiceesterno?: string;
  note?: string;
  idproduttore: string;
  nomeproduttore?: string;
  idtipo: string;
  nometipo?: string;
  idcategoria: string;
  nomecategoria?: string;
  ordinabile: boolean;
  annullato: boolean;
  solocollointero: boolean;
  ordineaequos: boolean;
  multiplo?: number | null;
}

/** Payload for creating/updating a product. */
export interface ProductInput {
  descrizione: string;
  unitamisura: string;
  prezzounitario: number;
  unitamisuracollo?: string;
  pesocassa: number;
  cadenza?: string;
  codiceesterno?: string;
  note?: string;
  idproduttore: string;
  idtipo: string;
  idcategoria: string;
  ordinabile: boolean;
  annullato: boolean;
  solocollointero: boolean;
  multiplo?: number | null;
}

/** Response of GET /api/ordertype/{id}/synchro/info (OrderSynchroInfoDTO). */
export interface SynchroInfo {
  lastSynchro?: string;
  updatedProducts?: number;
  aequosOrderId?: number | null;
}

/** Filter state for the products search. */
export interface ProductFilter {
  orderType?: string;
  category?: string;
  available?: string; // '', 'true', 'false'
  cancelled?: string; // '', 'true', 'false'
}
