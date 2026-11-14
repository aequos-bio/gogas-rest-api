import { useCallback, useState } from 'react';
import { useSnackbar } from 'notistack';
import { apiDelete, apiGetJson, apiPost, apiPostJson, apiPut } from '../../../utils/axios_utils';
import { ErrorResponse } from '../../../store/types';
import {
  AequosAvailableOrder,
  ManagerOrder,
  OrderInput,
  OrderSearchFilter,
  OrderTypeSelectItem,
  SelectItem,
} from './types';

const errorText = (err: any, fallback: string) =>
  err?.response?.data
    ? err.response.data.debugMessage || err.response.data.message || err.response.data.error
    : `${fallback}: ${err}`;

const isError = (res: any): res is ErrorResponse =>
  res && typeof res === 'object' && (res as ErrorResponse).error;

/**
 * Data hook for the manager (referente) orders list.
 *
 * Endpoints:
 *  - POST /api/order/manage/list                    (search, OrderSearchFilter)
 *  - GET  /api/ordertype/select/manager             (order types, extended)
 *  - GET  /api/home/order/status                    (status list)
 *  - POST /api/order/manage                         (create)
 *  - PUT  /api/order/manage/{id}                    (update)
 *  - DELETE /api/order/manage/{id}                  (delete)
 *  - POST /api/order/manage/{id}/action/{code}[?roundType]  (workflow action)
 *  - GET  /api/order/manage/aequos/available        (available Aequos orders)
 */
export const useOrderManagerAPI = () => {
  const [orders, setOrders] = useState<ManagerOrder[]>([]);
  const [orderTypes, setOrderTypes] = useState<OrderTypeSelectItem[]>([]);
  const [statuses, setStatuses] = useState<SelectItem[]>([]);
  const [loading, setLoading] = useState(false);
  const { enqueueSnackbar } = useSnackbar();

  const loadFilters = useCallback(() => {
    apiGetJson<OrderTypeSelectItem[] | ErrorResponse>('/api/ordertype/select/manager', {
      firstEmpty: false,
      extended: true,
    }).then((res) => {
      if (!isError(res)) setOrderTypes(res as OrderTypeSelectItem[]);
    });
    apiGetJson<SelectItem[] | ErrorResponse>('/api/home/order/status', {}).then((res) => {
      if (!isError(res)) setStatuses(res as SelectItem[]);
    });
  }, []);

  const search = useCallback(
    (filter: OrderSearchFilter) => {
      setLoading(true);
      apiPostJson<ManagerOrder[] | ErrorResponse>('/api/order/manage/list', filter)
        .then((res) => {
          setLoading(false);
          if (isError(res)) {
            enqueueSnackbar((res as ErrorResponse).errorMessage, { variant: 'error' });
          } else {
            setOrders(res as ManagerOrder[]);
          }
        })
        .catch((err) => {
          setLoading(false);
          enqueueSnackbar(errorText(err, 'Errore durante la ricerca degli ordini'), { variant: 'error' });
        });
    },
    [enqueueSnackbar],
  );

  const createOrder = useCallback(
    (order: OrderInput): Promise<boolean> =>
      apiPost('/api/order/manage', order)
        .then(() => {
          enqueueSnackbar(
            `Ordine creato con successo${order.updateProductList ? ' e listino sincronizzato' : ''}`,
            { variant: 'success' },
          );
          return true;
        })
        .catch((err) => {
          enqueueSnackbar(errorText(err, "Errore durante la creazione dell'ordine"), { variant: 'error' });
          return false;
        }),
    [enqueueSnackbar],
  );

  const updateOrder = useCallback(
    (id: string, order: OrderInput): Promise<boolean> =>
      apiPut(`/api/order/manage/${id}`, order)
        .then(() => {
          enqueueSnackbar('Ordine modificato con successo', { variant: 'success' });
          return true;
        })
        .catch((err) => {
          enqueueSnackbar(errorText(err, "Errore durante la modifica dell'ordine"), { variant: 'error' });
          return false;
        }),
    [enqueueSnackbar],
  );

  const deleteOrder = useCallback(
    (id: string): Promise<boolean> =>
      apiDelete(`/api/order/manage/${id}`)
        .then(() => {
          enqueueSnackbar('Ordine eliminato con successo', { variant: 'success' });
          return true;
        })
        .catch((err) => {
          enqueueSnackbar(errorText(err, "Errore durante l'eliminazione dell'ordine"), { variant: 'error' });
          return false;
        }),
    [enqueueSnackbar],
  );

  const changeStatus = useCallback(
    (id: string, action: string, roundType?: number): Promise<boolean> => {
      const url =
        roundType !== undefined
          ? `/api/order/manage/${id}/action/${action}?roundType=${roundType}`
          : `/api/order/manage/${id}/action/${action}`;
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
    [enqueueSnackbar],
  );

  const loadAequosAvailable = useCallback((): Promise<AequosAvailableOrder[]> => {
    return apiGetJson<AequosAvailableOrder[] | ErrorResponse>('/api/order/manage/aequos/available', {})
      .then((res) => (isError(res) ? [] : (res as AequosAvailableOrder[])))
      .catch(() => []);
  }, []);

  return {
    orders,
    orderTypes,
    statuses,
    loading,
    loadFilters,
    search,
    createOrder,
    updateOrder,
    deleteOrder,
    changeStatus,
    loadAequosAvailable,
  };
};
