/** Select option returned by the backend SelectItemDTO ({ id, description }). */
export interface SelectItem {
  id: string;
  description: string;
}

/**
 * Order row as returned by POST /api/order/user/list (OrderDTO).
 * Property names mirror the backend JSON (Italian).
 */
export interface UserOrder {
  id: string;
  idtipoordine: string;
  tipoordine: string;
  dataapertura: string;
  datachiusura: string;
  orachiusura: number;
  dataconsegna: string;
  codicestato: number;
  stato: string;
  numarticoli: number;
  totaleordine: number;
  amici: boolean;
  contabilizzabile: boolean;
  contabilizzato: boolean;
}

/** Filter payload for POST /api/order/user/list (OrderSearchFilter). */
export interface OrderSearchFilter {
  orderType?: string;
  dueDateFrom?: string;
  dueDateTo?: string;
  deliveryDateFrom?: string;
  deliveryDateTo?: string;
  status?: number[];
}
