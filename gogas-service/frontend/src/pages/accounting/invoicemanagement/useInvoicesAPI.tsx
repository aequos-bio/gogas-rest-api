import { useCallback, useState } from "react";
import { Invoice, Order } from "./types";
import { apiGet, apiGetJson, apiPost } from "../../../utils/axios_utils";
import { useSnackbar } from "notistack";
import { ErrorResponse } from "../../../store/types";
import { orderBy } from "lodash";
import moment from "moment-timezone";

export const useInvoicesAPI = (year: number) => {
  const { enqueueSnackbar } = useSnackbar();
  const [loading, setLoading] = useState(false);
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [ordersWithoutInvoice, setOrdersWithoutInvoice] = useState<Order[]>([]);

  const reload = useCallback(() => {
    setLoading(true);
    apiGetJson<Invoice[] | ErrorResponse>(
      `/api/accounting/gas/invoices/${year}`,
      {},
    ).then((response) => {
      setLoading(false);
      if (typeof response === 'object' && (response as ErrorResponse).error) {
        enqueueSnackbar((response as ErrorResponse).errorMessage, { variant: 'error' });
      } else {
        setInvoices(orderBy((response as Invoice[]), ['invoiceDate']));
      }
    });
  }, [enqueueSnackbar]);

  const syncWithOrders = useCallback(() => {
    return apiGet(`api/accounting/gas/syncAequosOrdersWithoutInvoice/${year}`)
      .then((response) => {
        reload();
        enqueueSnackbar(
          `Sincronizzazione completata con successo, ${response.data.data} ordini aggiunti`,
          {
            variant: 'success',
          },
        );
      })
      .catch((err) => {
        enqueueSnackbar(`Errore di sincronizzazione: ${err}`, {
          variant: 'error',
        });
      });
  }, [year, enqueueSnackbar]);

  const payInvoice = useCallback((invoice: Invoice, paymentDate: string) => {
    const promises: Promise<any>[] = [];
    invoice.orderIds.forEach(orderId => {
      const params = {
        idDataOrdine: orderId,
        numeroFattura: invoice.invoiceNumber,
        importoFattura: invoice.invoiceAmount,
        dataFattura: moment(invoice.invoiceDate).format('DD/MM/YYYY'),
        dataPagamento: paymentDate
          ? moment(paymentDate).format('DD/MM/YYYY')
          : null,
        pagato: paymentDate !== undefined && paymentDate !== null,
      };
      promises.push(
        apiPost(`/api/order/manage/${orderId}/invoice/data`, params)
      );
    });

    return Promise.all(promises)
      .then(() => {
        enqueueSnackbar(`Pagamento fattura salvato con successo`, {
          variant: 'success',
        });
      })
      .catch(err => {
        enqueueSnackbar(
          err.response?.statusText || 'Errore nel salvataggio del pagamento',
          { variant: 'error' }
        );
      });
  }, [year, enqueueSnackbar]);

  const saveInvoice = useCallback((number: string, date: string, amount: number, orderIds: string[], paymentDate: string) => {
    const promises: Promise<any>[] = [];
    orderIds.forEach((orderId) => {
      const params = {
        idDataOrdine: orderId,
        numeroFattura: number,
        importoFattura: amount,
        dataFattura: moment(date).format('DD/MM/YYYY'),
        dataPagamento: paymentDate
          ? moment(paymentDate).format('DD/MM/YYYY')
          : null,
        pagato: paymentDate !== undefined && paymentDate !== null,
      };
      promises.push(
        apiPost(`/api/order/manage/${orderId}/invoice/data`, params),
      );
    });

    return Promise.all(promises)
      .then(() => {
        enqueueSnackbar(`Fattura salvata con successo`, {
          variant: 'success',
        });
      })
      .catch((err) => {
        enqueueSnackbar(
          err.response?.statusText || 'Errore nel salvataggio della fattura',
          { variant: 'error' },
        );
      });
  }, [year, enqueueSnackbar]);

  const reloadOrdersWithoutInvoice = useCallback(() => {
    return apiGetJson<Order[] | ErrorResponse>(
      `api/accounting/gas/ordersWithoutInvoice/${year}`,
    ).then((response) => {
      if (typeof response === 'object' && (response as ErrorResponse).error) {
        enqueueSnackbar((response as ErrorResponse).errorMessage, { variant: 'error' });
      } else {
        setOrdersWithoutInvoice(
          orderBy(response as Order[],
            [(order) => moment(order.dataconsegna, 'DD/MM/YYYY').toISOString()],
            ['desc'])
        );
      }
    });

  }, [year, enqueueSnackbar]);

  return { loading, invoices, reload, syncWithOrders, payInvoice, saveInvoice, reloadOrdersWithoutInvoice, ordersWithoutInvoice }
}