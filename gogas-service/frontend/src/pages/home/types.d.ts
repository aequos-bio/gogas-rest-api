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