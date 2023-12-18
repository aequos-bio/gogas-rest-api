export interface UserOpenOrder {
  id: string;
  idtipoordine: string;
  datachiusura: string;
  dataconsegna: string;
  external: boolean;
  externallink?: string;
  orachiusura: number;
  showAdvance?: boolean;
  tipoordine: string;
  userOrders: UserSubOrder[];
}

export interface UserSubOrder {
  userId: string;
  firstname: string;
  lastname: string;
  itemsCount: number;
  totalAmount: number;
}

export interface UserSelect {
  id: string;
  description: string;
}

export interface UserDeliveryOrder {
  id: string;
  idtipoordine: string;
  datachiusura: string;
  dataconsegna: string;
  external: boolean;
  externallink?: string;
  orachiusura: number;
  showAdvance?: boolean;
  tipoordine: string;
  numarticoli: number;
  totaleordine: number;
  amici: boolean;
}