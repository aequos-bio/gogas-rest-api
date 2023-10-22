export interface OrderType {
  id?: string;
  descrizione: string;
  accountingCode?: string;
  billedByAequos: boolean;
  completamentocolli: boolean;
  excelAllProducts: boolean;
  excelAllUsers: boolean;
  external: boolean;
  externalLink?: string;
  idordineaequos?: number;
  preventivo: boolean;
  riepilogo: boolean;
  totalecalcolato: boolean;
  turni: boolean;
  utilizzata: boolean;
}

export interface Category {
  id: string;
  description: string;
}

export interface _GenericManager {
  id: string; // managerId
  description: string;
}
export interface OrderTypeManager extends _GenericManager { } // id = managerId
export interface AvailableOrderTypeManager extends _GenericManager { } // id = userId