import { useCallback, useState } from 'react';
import { orderBy } from 'lodash';
import { useSnackbar } from 'notistack';
import { apiDelete, apiGetJson, apiPost, apiPut } from '../../../utils/axios_utils';
import { ErrorResponse } from '../../../store/types';
import { Supplier } from './types';

const errorText = (err: any, fallback: string) =>
  err?.response?.data
    ? err.response.data.debugMessage || err.response.data.message || err.response.data.error
    : `${fallback}: ${err}`;

/**
 * Data hook for the Suppliers (Produttori) page. Mirrors the useUsersAPI
 * convention: returns the list plus loading state, a reload, and CRUD
 * mutations, surfacing outcomes through notistack.
 *
 * Endpoints: GET /api/supplier/list, POST /api/supplier,
 * PUT /api/supplier/{id}, DELETE /api/supplier/{id}.
 */
export const useSuppliersAPI = () => {
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [loading, setLoading] = useState(false);
  const { enqueueSnackbar } = useSnackbar();

  const reload = useCallback(() => {
    setLoading(true);
    apiGetJson<Supplier[] | ErrorResponse>('/api/supplier/list', {}).then((res) => {
      setLoading(false);
      if (typeof res === 'object' && (res as ErrorResponse).error) {
        enqueueSnackbar((res as ErrorResponse).errorMessage, { variant: 'error' });
      } else {
        setSuppliers(orderBy(res as Supplier[], ['ragionesociale'], ['asc']));
      }
    });
  }, [enqueueSnackbar]);

  const createSupplier = useCallback(
    (supplier: Partial<Supplier>): Promise<boolean> => {
      return apiPost('/api/supplier', supplier)
        .then(() => {
          enqueueSnackbar('Produttore creato con successo', { variant: 'success' });
          reload();
          return true;
        })
        .catch((err) => {
          enqueueSnackbar(errorText(err, 'Errore durante la creazione del produttore'), {
            variant: 'error',
          });
          return false;
        });
    },
    [enqueueSnackbar, reload],
  );

  const updateSupplier = useCallback(
    (id: string, supplier: Partial<Supplier>): Promise<boolean> => {
      return apiPut(`/api/supplier/${id}`, supplier)
        .then(() => {
          enqueueSnackbar('Produttore modificato con successo', { variant: 'success' });
          reload();
          return true;
        })
        .catch((err) => {
          enqueueSnackbar(errorText(err, 'Errore durante la modifica del produttore'), {
            variant: 'error',
          });
          return false;
        });
    },
    [enqueueSnackbar, reload],
  );

  const deleteSupplier = useCallback(
    (id: string): Promise<void> => {
      return apiDelete(`/api/supplier/${id}`)
        .then(() => {
          enqueueSnackbar('Produttore eliminato con successo', { variant: 'success' });
          reload();
        })
        .catch((err) => {
          enqueueSnackbar(errorText(err, "Errore durante l'eliminazione del produttore"), {
            variant: 'error',
          });
        });
    },
    [enqueueSnackbar, reload],
  );

  return { suppliers, loading, reload, createSupplier, updateSupplier, deleteSupplier };
};
