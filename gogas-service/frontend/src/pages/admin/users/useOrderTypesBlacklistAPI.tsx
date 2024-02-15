import { useCallback, useState } from "react";
import { useSnackbar } from "notistack";
import { User } from './types';
import { apiDelete, apiGetJson, apiPost, apiPut } from "../../../utils/axios_utils";
import { BlacklistItem } from "./types";
import { ErrorResponse } from "../../../store/types";

export const useOrderTypesBlacklistAPI = () => {
  const { enqueueSnackbar } = useSnackbar();
  const [loading, setLoading] = useState(false);
  const [blacklist, setBlacklist] = useState<String[]>([]);

  const reload = useCallback(async (userId: string): Promise<string[]> => {
    setLoading(true);
    var orderTypeList = await apiGetJson<BlacklistItem[] | ErrorResponse>('/api/ordertype/blacklist/' + userId, {});
    setLoading(false);

    if (typeof orderTypeList === 'object' && (orderTypeList as ErrorResponse).error) {
      enqueueSnackbar((orderTypeList as ErrorResponse).errorMessage, { variant: 'error' });
      return [];
    } else {
      return (orderTypeList as BlacklistItem[]).map(order => order.id);
    }
  }, [enqueueSnackbar]);

  const updateBlacklist = useCallback((user: User, orderTypes: String[]): Promise<void> => {
    return apiPut('/api/ordertype/blacklist/' + user.idUtente, orderTypes)
      .then(() => {
        enqueueSnackbar(
          `Lista degli ordini bloccati per l'utente ${user.nome} ${user.cognome} salvata correttamente`,
          { variant: 'success' }
        )
      })
      .catch(err => {
        enqueueSnackbar(
          err.response?.statusText || 'Errore nel salvataggio della lista degli ordini bloccati',
          { variant: 'error' }
        );
      });
  }, []);

  return {
    blacklist, loading, reload, updateBlacklist
  }
}