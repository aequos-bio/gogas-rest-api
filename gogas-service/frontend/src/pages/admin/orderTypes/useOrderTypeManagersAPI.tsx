import { useSnackbar } from "notistack";
import { useCallback, useState } from "react";
import { AvailableOrderTypeManager, OrderTypeManager } from "./typed";
import { apiDelete, apiGetJson, apiPut } from "../../../utils/axios_utils";
import { ErrorResponse } from "../../../store/types";
import { orderBy, remove } from "lodash";

export const useOrderTypeManagersAPI = (orderTypeId?: string) => {
  const { enqueueSnackbar } = useSnackbar();
  const [loadingManagers, setLoadingManagers] = useState(false);
  const [loadingAvailableManagers, setLoadingAvailableManagers] = useState(false);
  const [managers, setManagers] = useState<OrderTypeManager[]>([]);
  const [availableManagers, setAvailableManagers] = useState<AvailableOrderTypeManager[]>([]);

  const _reloadManagers = useCallback(() => {
    if (orderTypeId) {
      setLoadingManagers(true);
      apiGetJson<OrderTypeManager[] | ErrorResponse>(`/api/ordertype/${orderTypeId}/manager`, {}).then((managersList) => {
        setLoadingManagers(false);
        if (typeof managersList === 'object' && (managersList as ErrorResponse).error) {
          enqueueSnackbar((managersList as ErrorResponse).errorMessage, { variant: 'error' });
        } else {
          setManagers(orderBy((managersList as OrderTypeManager[]), 'description'));
        }
      });
    }
  }, [orderTypeId]);

  const _reloadAvailableManagers = useCallback(() => {
    if (orderTypeId) {
      setLoadingAvailableManagers(true);
      apiGetJson<AvailableOrderTypeManager[] | ErrorResponse>(`/api/ordertype/${orderTypeId}/manager/available`, {}).then((userList) => {
        setLoadingAvailableManagers(false);
        if (typeof userList === 'object' && (userList as ErrorResponse).error) {
          enqueueSnackbar((userList as ErrorResponse).errorMessage, { variant: 'error' });
        } else {
          setAvailableManagers(orderBy((userList as AvailableOrderTypeManager[]), 'description'));
        }
      });
    }
  }, [orderTypeId]);

  const reload = useCallback(() => {
    _reloadManagers();
    _reloadAvailableManagers();
  }, [_reloadManagers, _reloadAvailableManagers]);

  const addManager = useCallback((user: AvailableOrderTypeManager) => {
    apiPut(`/api/ordertype/${orderTypeId}/manager/${user.id}`)
      .then(() => {
        enqueueSnackbar('Referente aggiunto', { variant: 'success' });
        const _availableManagers = [...availableManagers];
        remove(_availableManagers, item => item.id === user.id)
        setAvailableManagers(_availableManagers);
        _reloadManagers();
      })
      .catch(error => {
        enqueueSnackbar(`Impossibile aggiungere il referente: ${error}`, { variant: 'error' });
      })
  }, [orderTypeId, availableManagers]);

  const addManagers = useCallback((userIds: string[]) => {
    const promises = userIds.map(userId => apiPut(`/api/ordertype/${orderTypeId}/manager/${userId}`));
    Promise.all(promises)
      .then(() => {
        enqueueSnackbar('Referenti aggiunti', { variant: 'success' });
        reload();
      })
      .catch(error => {
        enqueueSnackbar(`Impossibile aggiungere i referenti: ${error}`, { variant: 'error' });
      })
  }, [orderTypeId]);

  const removeManager = useCallback((manager: OrderTypeManager) => {
    apiDelete(`/api/ordertype/manager/${manager.id}`)
      .then(() => {
        enqueueSnackbar('Referente rimosso', { variant: 'success' });
        const _managers = [...managers];
        remove(_managers, item => item.id === manager.id)
        setManagers(_managers);
        _reloadAvailableManagers();
      })
      .catch(error => {
        enqueueSnackbar(`Impossibile rimuovere il referente: ${error}`, { variant: 'error' });
      })
  }, [orderTypeId, managers]);

  const removeManagers = useCallback((managerIds: string[]) => {
    const promises = managerIds.map(managerId => apiDelete(`/api/ordertype/manager/${managerId}`));
    Promise.all(promises)
      .then(() => {
        enqueueSnackbar('Referenti rimossi', { variant: 'success' });
        reload();
      })
      .catch(error => {
        enqueueSnackbar(`Impossibile rimuovere i referenti: ${error}`, { variant: 'error' });
      })
  }, [orderTypeId]);

  return { managers, availableManagers, loadingManagers, loadingAvailableManagers, reload, addManager, removeManager, addManagers, removeManagers }

}