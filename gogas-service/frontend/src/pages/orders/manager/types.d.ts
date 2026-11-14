export interface SelectItem {
  id: string;
  description: string;
}

/** Order row returned by POST /api/order/manage/list (OrderDTO). */
export interface ManagerOrder {
  id: string;
  idtipoordine: string;
  tipoordine: string;
  dataapertura: string;
  datachiusura: string;
  orachiusura: number;
  dataconsegna: string;
  codicestato: number;
  stato: string;
  inviato: boolean;
  evaso: boolean;
  totaleordine: number;
  totalefattura?: number;
  external: boolean;
  externallink?: string;
  /** Comma-separated available workflow actions computed by the backend. */
  actions?: string;
}

/** Filter payload for POST /api/order/manage/list (OrderSearchFilter). */
export interface OrderSearchFilter {
  orderType?: string;
  dueDateFrom?: string;
  dueDateTo?: string;
  deliveryDateFrom?: string;
  deliveryDateTo?: string;
  status?: number[];
  paid?: boolean;
}

/** Payload for creating/updating an order (OrderDTO input subset). */
export interface OrderInput {
  idtipoordine: string;
  dataapertura: string;
  datachiusura: string;
  orachiusura: number;
  dataconsegna: string;
  externallink?: string;
  updateProductList?: boolean;
}

/** Aequos available (not yet opened) order. */
export interface AequosAvailableOrder {
  idtipoordine: string;
  tipoordine: string;
  dataapertura: string;
  datachiusura: string;
  dataconsegna: string;
}

/** Extended order type select item (manager scope, extended=true). */
export interface OrderTypeSelectItem extends SelectItem {
  idordineaequos?: number | null;
  external?: boolean;
  externalLink?: string;
}
