export interface ManagedOrder {
  id: string;
  external: false;
  actions: string;
  updateProductList: boolean;
  idtipoordine: string;
  tipoordine: string;
  dataapertura: string;
  datachiusura: string;
  orachiusura: number;
  dataconsegna: string;
  codicestato: number | 1 | 2;
  stato: string;
  inviato: boolean;
  evaso: boolean;
  totaleordine: number;
  numarticoli: number;
  externallink?: string;
  amici: boolean;
  contabilizzato: boolean;
  contabilizzabile: boolean;
}

export interface ManagedOrderQueryFilter {
  filterscount: number;
  groupscount: number;
  sortorder: string;
  pagenum: number;
  pagesize: number;
  recordstartindex: number;
  recordendindex: number;
  orderType: string;
  paid: string;
  dueDateFrom: string;
  dueDateTo: string;
  deliveryDateFrom: string;
  deliveryDateTo: string;
  status: string[];
}