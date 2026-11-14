import { useCallback, useState } from 'react';
import { useSnackbar } from 'notistack';
import { apiDelete, apiGetJson, apiPostJsonBody } from '../../../../utils/axios_utils';
import { ErrorResponse } from '../../../../store/types';
import { OrderByUser, OrderDetails, OrderItemByUser, SelectItem } from './types';

const errorText = (err: any, fallback: string) =>
  err?.response?.data
    ? err.response.data.debugMessage || err.response.data.message || err.response.data.error
    : `${fallback}: ${err}`;

const isError = (res: any): res is ErrorResponse =>
  res && typeof res === 'object' && (res as ErrorResponse).error;

/**
 * Data access for the manager order-details "by user" view.
 *
 * Endpoints (all under /api/order/manage/{orderId}):
 *  - GET    ""                           -> order header (OrderDetailsDTO)
 *  - GET    "/byUser/list"               -> by-user rows
 *  - GET    "/byUser/{userId}"           -> per-user item rows
 *  - POST   "/byUser/{userId}"           -> set net amount / add movement (body: amount)
 *  - DELETE "/byUser/{userId}"           -> delete a user movement
 *  - POST   "/shippingCost"              -> set shipping cost (returns updated totals)
 *  - GET    "/byUser/availableUsers"     -> users without a movement yet
 */
export const useOrderByUserAPI = (orderId: string) => {
  const base = `/api/order/manage/${orderId}`;
  const { enqueueSnackbar } = useSnackbar();

  const [details, setDetails] = useState<OrderDetails | undefined>(undefined);
  const [users, setUsers] = useState<OrderByUser[]>([]);
  const [loadingUsers, setLoadingUsers] = useState(false);

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

  const loadUsers = useCallback(() => {
    setLoadingUsers(true);
    apiGetJson<OrderByUser[] | ErrorResponse>(`${base}/byUser/list`, {})
      .then((res) => {
        setLoadingUsers(false);
        if (isError(res)) enqueueSnackbar((res as ErrorResponse).errorMessage, { variant: 'error' });
        else setUsers(res as OrderByUser[]);
      })
      .catch((err) => {
        setLoadingUsers(false);
        enqueueSnackbar(errorText(err, 'Errore durante il caricamento degli utenti'), { variant: 'error' });
      });
  }, [base, enqueueSnackbar]);

  const loadUserItems = useCallback(
    (userId: string): Promise<OrderItemByUser[]> =>
      apiGetJson<OrderItemByUser[] | ErrorResponse>(`${base}/byUser/${userId}`, {}).then((res) =>
        isError(res) ? [] : (res as OrderItemByUser[]),
      ),
    [base],
  );

  const updateNetAmount = useCallback(
    (userId: string, amount: number): Promise<boolean> =>
      apiPostJsonBody(`${base}/byUser/${userId}`, amount)
        .then(() => {
          enqueueSnackbar('Importo aggiornato', { variant: 'success' });
          return true;
        })
        .catch((err) => {
          enqueueSnackbar(errorText(err, 'Errore aggiornamento importo'), { variant: 'error' });
          return false;
        }),
    [base, enqueueSnackbar],
  );

  const addMovement = useCallback(
    (userId: string, amount: number): Promise<boolean> =>
      apiPostJsonBody(`${base}/byUser/${userId}`, amount)
        .then(() => {
          enqueueSnackbar('Movimento inserito correttamente', { variant: 'success' });
          return true;
        })
        .catch((err) => {
          enqueueSnackbar(errorText(err, "Errore durante l'inserimento del movimento"), { variant: 'error' });
          return false;
        }),
    [base, enqueueSnackbar],
  );

  const deleteMovement = useCallback(
    (userId: string): Promise<boolean> =>
      apiDelete(`${base}/byUser/${userId}`)
        .then(() => {
          enqueueSnackbar('Movimento eliminato correttamente', { variant: 'success' });
          return true;
        })
        .catch((err) => {
          enqueueSnackbar(errorText(err, "Errore durante l'eliminazione del movimento"), { variant: 'error' });
          return false;
        }),
    [base, enqueueSnackbar],
  );

  const updateShippingCost = useCallback(
    (cost: number): Promise<OrderByUser[] | undefined> =>
      apiPostJsonBody(`${base}/shippingCost`, cost)
        .then((response) => {
          const res = response.data as OrderByUser[] | ErrorResponse;
          if (isError(res)) {
            enqueueSnackbar((res as ErrorResponse).errorMessage, { variant: 'error' });
            return undefined;
          }
          return res as OrderByUser[];
        })
        .catch((err) => {
          enqueueSnackbar(errorText(err, 'Errore aggiornamento spese di trasporto'), { variant: 'error' });
          return undefined;
        }),
    [base, enqueueSnackbar],
  );

  const loadAvailableUsers = useCallback(
    (): Promise<SelectItem[]> =>
      apiGetJson<SelectItem[] | ErrorResponse>(`${base}/byUser/availableUsers`, {}).then((res) =>
        isError(res) ? [] : (res as SelectItem[]),
      ),
    [base],
  );

  return {
    details,
    users,
    loadingUsers,
    loadDetails,
    loadUsers,
    loadUserItems,
    updateNetAmount,
    addMovement,
    deleteMovement,
    updateShippingCost,
    loadAvailableUsers,
  };
};
