export interface Invoice {
  accountingCode: string;
  description: string;
  invoiceAmount: number;
  invoiceDate: string;
  invoiceKey: {
    accountingCode: string;
    invoiceNumber: string;
    invoiceDate: string;
  }
  invoiceNumber: string;
  orderIds: string[];
  paid: boolean;
  paymentDate?: string;
}

export interface Order {
  id: string;
  dataconsegna: string;
  tipoordine: string;
}