import { useCallback, useState } from "react";
import { useSnackbar } from "notistack";
import { apiDelete, apiGetJson, apiPost, apiPut } from "../../../utils/axios_utils";
import { OrderType } from "./typed";
import { ErrorResponse } from "../../../store/types";

export const useOrderTypesAPI = () => {
  const { enqueueSnackbar } = useSnackbar();
  const [loading, setLoading] = useState(false);
  const [orderTypes, setOrderTypes] = useState<OrderType[]>([]);

  const reload = useCallback(() => {
    setLoading(true);
    apiGetJson<OrderType[] | ErrorResponse>('/api/ordertype/list', {}).then(orderTypeList => {
      setLoading(false);
      if (typeof orderTypeList === 'object' && (orderTypeList as ErrorResponse).error) {
        enqueueSnackbar((orderTypeList as ErrorResponse).errorMessage, { variant: 'error' });
      } else {
        setOrderTypes(orderTypeList as OrderType[]);
      }
    });
  }, [enqueueSnackbar]);

  const getOrderType = (id: string): Promise<OrderType | undefined> => {
    return apiGetJson<OrderType | ErrorResponse>(`/api/ordertype/${id}`, {})
      .then((orderType) => {
        if (typeof orderType === 'object' && (orderType as ErrorResponse).error) {
          enqueueSnackbar((orderType as ErrorResponse).errorMessage, { variant: 'error' });
          return undefined;
        } else {
          return orderType as OrderType;
        }
      });
  }

  const saveOrderType = (orderType: OrderType): Promise<void> => {
    return apiPost('/api/ordertype', orderType)
      .then(() => {
        enqueueSnackbar(
          `Tipo ordine salvato`,
          { variant: 'success' }
        )
      })
      .catch(err => {
        enqueueSnackbar(
          err.response?.statusText || 'Errore nel salvataggio del tipo ordine',
          { variant: 'error' }
        );
      });
  }

  const updateOrderType = (id: string, orderType: OrderType): Promise<void> => {
    return apiPut(`/api/ordertype/${id}`, orderType)
      .then(() => {
        enqueueSnackbar(
          `Tipo ordine modificato`,
          { variant: 'success' }
        )
      })
      .catch(err => {
        enqueueSnackbar(
          err.response?.statusText || 'Errore nel salvataggio del tipo ordine',
          { variant: 'error' }
        );
      });
  }

  const deleteOrderType = (id: string): Promise<void> => {
    return apiDelete(`/api/ordertype/${id}`)
      .then(() => {
        reload();
        enqueueSnackbar('Tipo ordine eliminato', { variant: 'success' });
      })
      .catch(err => {
        enqueueSnackbar(
          err.response?.statusText ||
          "Errore nell'eliminazione del tipo ordine",
          { variant: 'error' }
        );
      });
  }

  const syncWithAequos = () => {
    return apiPut('/api/ordertype/aequos/sync')
      .then(() => {
        enqueueSnackbar('Sincronizzazione completata con successo', {
          variant: 'success',
        });
        reload();
      })
      .catch(err => {
        enqueueSnackbar(`Errore nella sincronizzazione: ${err}`, {
          variant: 'error',
        });
      });
  }

  return {
    orderTypes, loading, reload, getOrderType, saveOrderType, updateOrderType, deleteOrderType, syncWithAequos
  }
}