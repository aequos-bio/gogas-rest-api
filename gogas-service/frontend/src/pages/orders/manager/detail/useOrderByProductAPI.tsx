import { useCallback, useState } from 'react';
import { useSnackbar } from 'notistack';
import {
  apiGetJson,
  apiPost,
  apiPut,
  apiPutJson,
} from '../../../../utils/axios_utils';
import { downloadFile } from '../../../../utils/download_utils';
import { apiPostMultipart } from '../../../../utils/upload_utils';
import { ErrorResponse } from '../../../../store/types';
import {
  OrderByProduct,
  OrderDetails,
  OrderItemByProduct,
  SelectItem,
} from './types';

const errorText = (err: any, fallback: string) =>
  err?.response?.data
    ? err.response.data.debugMessage || err.response.data.message || err.response.data.error
    : `${fallback}: ${err}`;

const isError = (res: any): res is ErrorResponse =>
  res && typeof res === 'object' && (res as ErrorResponse).error;

/**
 * Data access for the manager order-details "by product" view.
 *
 * Endpoints (all under /api/order/manage/{orderId}):
 *  - GET  ""                              -> order header (OrderDetailsDTO)
 *  - GET  "/product"                      -> by-product rows
 *  - GET  "/product/{productId}"          -> per-product user rows
 *  - PUT  "/product/{productId}/supplier" -> set ordered boxes (colli)
 *  - PUT  "/item/{itemId}"                -> set delivered qty (qta ritirata)
 *  - PUT  "/{type}/{rowId}/{action}"      -> cancel/restore product|item
 *  - POST "/item"                         -> add a user order
 *  - PUT  "/product/{productId}/price"    -> change product price
 *  - PUT  "/product/{productId}/distribute" -> distribute remaining qty
 *  - PUT  "/item/{itemId}/replace"        -> replace product on an item
 *  - GET  "/product/{productId}/availableUsers" -> users not ordering
 */
export const useOrderByProductAPI = (orderId: string) => {
  const base = `/api/order/manage/${orderId}`;
  const { enqueueSnackbar } = useSnackbar();

  const [details, setDetails] = useState<OrderDetails | undefined>(undefined);
  const [products, setProducts] = useState<OrderByProduct[]>([]);
  const [loadingProducts, setLoadingProducts] = useState(false);

  const loadDetails = useCallback((): Promise<OrderDetails | undefined> => {
    return apiGetJson<OrderDetails | ErrorResponse>(base, {}).then((res) => {
      if (isError(res)) {
        enqueueSnackbar((res as ErrorResponse).errorMessage, { variant: 'error' });
        return undefined;
      }
      setDetails(res as OrderDetails);
      return res as OrderDetails;
    });
  }, [base, enqueueSnackbar]);

  const loadProducts = useCallback(() => {
    setLoadingProducts(true);
    apiGetJson<OrderByProduct[] | ErrorResponse>(`${base}/product`, {})
      .then((res) => {
        setLoadingProducts(false);
        if (isError(res)) enqueueSnackbar((res as ErrorResponse).errorMessage, { variant: 'error' });
        else setProducts(res as OrderByProduct[]);
      })
      .catch((err) => {
        setLoadingProducts(false);
        enqueueSnackbar(errorText(err, 'Errore durante il caricamento dei prodotti'), { variant: 'error' });
      });
  }, [base, enqueueSnackbar]);

  const loadProductItems = useCallback(
    (productId: string): Promise<OrderItemByProduct[]> =>
      apiGetJson<OrderItemByProduct[] | ErrorResponse>(`${base}/product/${productId}`, {}).then((res) =>
        isError(res) ? [] : (res as OrderItemByProduct[]),
      ),
    [base],
  );

  const updateOrderedBoxes = useCallback(
    (productId: string, boxes: number): Promise<boolean> =>
      apiPutJson(`${base}/product/${productId}/supplier`, boxes)
        .then(() => {
          enqueueSnackbar('Colli aggiornati', { variant: 'success' });
          return true;
        })
        .catch((err) => {
          enqueueSnackbar(errorText(err, 'Errore aggiornamento colli'), { variant: 'error' });
          return false;
        }),
    [base, enqueueSnackbar],
  );

  const updateDeliveredQty = useCallback(
    (itemId: string, qty: number): Promise<boolean> =>
      apiPutJson(`${base}/item/${itemId}`, qty)
        .then(() => {
          enqueueSnackbar('Quantità aggiornata', { variant: 'success' });
          return true;
        })
        .catch((err) => {
          enqueueSnackbar(errorText(err, 'Errore aggiornamento quantità'), { variant: 'error' });
          return false;
        }),
    [base, enqueueSnackbar],
  );

  const itemAction = useCallback(
    (type: 'product' | 'item', rowId: string, action: 'cancel' | 'restore'): Promise<boolean> =>
      apiPut(`${base}/${type}/${rowId}/${action}`)
        .then(() => {
          enqueueSnackbar('Operazione effettuata con successo', { variant: 'success' });
          return true;
        })
        .catch((err) => {
          enqueueSnackbar(errorText(err, "Errore durante l'operazione"), { variant: 'error' });
          return false;
        }),
    [base, enqueueSnackbar],
  );

  const addUserOrder = useCallback(
    (productId: string, userId: string, um: string, qta: number): Promise<boolean> =>
      apiPost(`${base}/item`, { id: productId, idUtente: userId, um, qta })
        .then(() => {
          enqueueSnackbar('Ordine inserito correttamente', { variant: 'success' });
          return true;
        })
        .catch((err) => {
          enqueueSnackbar(errorText(err, "Errore durante l'inserimento dell'ordine"), { variant: 'error' });
          return false;
        }),
    [base, enqueueSnackbar],
  );

  const updatePrice = useCallback(
    (productId: string, price: number): Promise<boolean> =>
      apiPutJson(`${base}/product/${productId}/price`, price)
        .then(() => {
          enqueueSnackbar('Prezzo aggiornato correttamente', { variant: 'success' });
          return true;
        })
        .catch((err) => {
          enqueueSnackbar(errorText(err, "Errore durante l'aggiornamento del prezzo"), { variant: 'error' });
          return false;
        }),
    [base, enqueueSnackbar],
  );

  const distributeRemaining = useCallback(
    (productId: string): Promise<boolean> =>
      apiPut(`${base}/product/${productId}/distribute`)
        .then(() => {
          enqueueSnackbar('Distribuzione effettuata con successo', { variant: 'success' });
          return true;
        })
        .catch((err) => {
          enqueueSnackbar(errorText(err, 'Errore durante la distribuzione'), { variant: 'error' });
          return false;
        }),
    [base, enqueueSnackbar],
  );

  const replaceProduct = useCallback(
    (itemId: string, productId: string): Promise<boolean> =>
      apiPutJson(`${base}/item/${itemId}/replace`, productId)
        .then(() => {
          enqueueSnackbar('Sostituzione effettuata con successo', { variant: 'success' });
          return true;
        })
        .catch((err) => {
          enqueueSnackbar(errorText(err, 'Errore durante la sostituzione'), { variant: 'error' });
          return false;
        }),
    [base, enqueueSnackbar],
  );

  const loadAvailableUsers = useCallback(
    (productId: string): Promise<SelectItem[]> =>
      apiGetJson<SelectItem[] | ErrorResponse>(`${base}/product/${productId}/availableUsers`, {}).then((res) =>
        isError(res) ? [] : (res as SelectItem[]),
      ),
    [base],
  );

  /** ---------- toolbar downloads ---------- */

  const exportSmistamentoFile = useCallback(
    (): Promise<void> =>
      downloadFile(`/api/delivery/${orderId}/file`, 'smistamento.xlsx').catch((err) => {
        enqueueSnackbar(errorText(err, "Errore durante l'esportazione per smistamento"), { variant: 'error' });
      }),
    [orderId, enqueueSnackbar],
  );

  const exportExcel = useCallback(
    (): Promise<void> =>
      downloadFile(`${base}/export`, 'ordine.xlsx').catch((err) => {
        enqueueSnackbar(errorText(err, "Errore durante l'export in excel"), { variant: 'error' });
      }),
    [base, enqueueSnackbar],
  );

  const printUserSheets = useCallback(
    (type: 'pdf' | 'excel'): Promise<void> => {
      const url = type === 'excel' ? `${base}/byUser/sheets?type=excel` : `${base}/byUser/sheets`;
      const fallback = type === 'excel' ? 'schede-utenti.xlsx' : 'schede-utenti.pdf';
      return downloadFile(url, fallback).catch((err) => {
        enqueueSnackbar(errorText(err, 'Errore durante la stampa delle schede'), { variant: 'error' });
      });
    },
    [base, enqueueSnackbar],
  );

  /** ---------- uploads ---------- */

  const uploadDeliveryFile = useCallback(
    (file: File): Promise<boolean> => {
      const formData = new FormData();
      formData.append('file', file);
      return apiPostMultipart(`/api/delivery/${orderId}/file`, formData)
        .then(() => {
          enqueueSnackbar('Quantità caricate con successo', { variant: 'success' });
          return true;
        })
        .catch((err) => {
          enqueueSnackbar(errorText(err, 'Errore durante il caricamento delle quantità'), { variant: 'error' });
          return false;
        });
    },
    [orderId, enqueueSnackbar],
  );

  const uploadInvoiceAttachment = useCallback(
    (file: File): Promise<boolean> => {
      const formData = new FormData();
      formData.append('file', file);
      return apiPostMultipart(`${base}/invoice/attachment`, formData)
        .then(() => {
          enqueueSnackbar('Allegato caricato con successo', { variant: 'success' });
          return true;
        })
        .catch((err) => {
          enqueueSnackbar(errorText(err, "Errore durante il caricamento dell'allegato"), { variant: 'error' });
          return false;
        });
    },
    [base, enqueueSnackbar],
  );

  const downloadInvoiceAttachment = useCallback(
    (): Promise<void> =>
      downloadFile(`${base}/invoice/attachment`, 'fattura').catch((err) => {
        enqueueSnackbar(errorText(err, "Errore durante il download dell'allegato"), { variant: 'error' });
      }),
    [base, enqueueSnackbar],
  );

  /** ---------- Aequos ---------- */

  const aequosAction = useCallback(
    (action: 'send' | 'synch' | 'weights'): Promise<boolean> => {
      const path =
        action === 'send'
          ? `${base}/aequos/order/send`
          : action === 'synch'
            ? `${base}/aequos/order/synch`
            : `${base}/aequos/order/weights`;
      const successMsg =
        action === 'send'
          ? 'Invio ad Aequos completato con successo'
          : action === 'synch'
            ? 'Sincronizzazione completata con successo'
            : 'Invio pesi completato con successo';
      return apiPost(path)
        .then(() => {
          enqueueSnackbar(successMsg, { variant: 'success' });
          return true;
        })
        .catch((err) => {
          enqueueSnackbar(errorText(err, "Errore durante l'operazione con Aequos"), { variant: 'error' });
          return false;
        });
    },
    [base, enqueueSnackbar],
  );

  /** ---------- order status workflow ---------- */

  const changeStatus = useCallback(
    (action: string, roundType?: number): Promise<boolean> => {
      const url =
        roundType !== undefined
          ? `${base}/action/${action}?roundType=${roundType}`
          : `${base}/action/${action}`;
      return apiPost(url)
        .then(() => {
          enqueueSnackbar('Operazione eseguita con successo', { variant: 'success' });
          return true;
        })
        .catch((err) => {
          enqueueSnackbar(errorText(err, "Errore durante l'operazione sull'ordine"), { variant: 'error' });
          return false;
        });
    },
    [base, enqueueSnackbar],
  );

  /** ---------- invoice data ---------- */

  const saveInvoiceData = useCallback(
    (data: {
      numeroFattura?: string;
      importoFattura?: number;
      dataFattura?: string;
      pagato: boolean;
      dataPagamento?: string;
    }): Promise<boolean> =>
      apiPost(`${base}/invoice/data`, data)
        .then(() => {
          enqueueSnackbar('Dati contabili aggiornati con successo', { variant: 'success' });
          return true;
        })
        .catch((err) => {
          enqueueSnackbar(errorText(err, "Errore durante l'aggiornamento dei dati contabili"), { variant: 'error' });
          return false;
        }),
    [base, enqueueSnackbar],
  );

  return {
    details,
    products,
    loadingProducts,
    loadDetails,
    loadProducts,
    loadProductItems,
    updateOrderedBoxes,
    updateDeliveredQty,
    itemAction,
    addUserOrder,
    updatePrice,
    distributeRemaining,
    replaceProduct,
    loadAvailableUsers,
    exportSmistamentoFile,
    exportExcel,
    printUserSheets,
    uploadDeliveryFile,
    uploadInvoiceAttachment,
    downloadInvoiceAttachment,
    aequosAction,
    saveInvoiceData,
    changeStatus,
  };
};
