import { useCallback, useState } from 'react';
import { useSnackbar } from 'notistack';
import { apiGetJson, apiPostJson } from '../../../utils/axios_utils';
import { ErrorResponse } from '../../../store/types';
import { OrderSearchFilter, SelectItem, UserOrder } from './types';

const isError = (res: any): res is ErrorResponse =>
  res && typeof res === 'object' && (res as ErrorResponse).error;

/**
 * Data hook for the user "orders list" page. Loads the filter options
 * (order types + order statuses) and runs the order search.
 *
 * Endpoints:
 *  - GET  /api/ordertype/select?firstEmpty=false   (user order types)
 *  - GET  /api/home/order/status                   (status list)
 *  - POST /api/order/user/list                     (search, OrderSearchFilter)
 *  - GET  /api/friend/select                       (to know if the user has friends)
 */
export const useUserOrdersAPI = () => {
  const [orders, setOrders] = useState<UserOrder[]>([]);
  const [orderTypes, setOrderTypes] = useState<SelectItem[]>([]);
  const [statuses, setStatuses] = useState<SelectItem[]>([]);
  const [hasFriends, setHasFriends] = useState(false);
  const [loading, setLoading] = useState(false);
  const { enqueueSnackbar } = useSnackbar();

  const loadFilters = useCallback(() => {
    apiGetJson<SelectItem[] | ErrorResponse>('/api/ordertype/select', { firstEmpty: false }).then((res) => {
      if (!isError(res)) setOrderTypes(res as SelectItem[]);
    });
    apiGetJson<SelectItem[] | ErrorResponse>('/api/home/order/status', {}).then((res) => {
      if (!isError(res)) setStatuses(res as SelectItem[]);
    });
    apiGetJson<SelectItem[] | ErrorResponse>('/api/friend/select', {}).then((res) => {
      if (!isError(res)) setHasFriends(((res as SelectItem[]) || []).length > 0);
    });
  }, []);

  const search = useCallback(
    (filter: OrderSearchFilter) => {
      setLoading(true);
      apiPostJson<UserOrder[] | ErrorResponse>('/api/order/user/list', filter)
        .then((res) => {
          setLoading(false);
          if (isError(res)) {
            enqueueSnackbar((res as ErrorResponse).errorMessage, { variant: 'error' });
          } else {
            setOrders(res as UserOrder[]);
          }
        })
        .catch((err) => {
          setLoading(false);
          enqueueSnackbar(`Errore durante la ricerca degli ordini: ${err}`, { variant: 'error' });
        });
    },
    [enqueueSnackbar],
  );

  return { orders, orderTypes, statuses, hasFriends, loading, loadFilters, search };
};
