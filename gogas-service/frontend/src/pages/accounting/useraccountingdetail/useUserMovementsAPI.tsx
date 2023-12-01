import { useCallback } from "react"
import { UserMovement, UserMovementView } from "./types"
import { ErrorResponse } from "../../../store/types";
import { useSnackbar } from "notistack";
import { apiGetJson, apiPost, apiPut } from "../../../utils/axios_utils";

export const useUserMovementsAPI = () => {
  const { enqueueSnackbar } = useSnackbar();

  const getUserMovement = useCallback((id: string) => {
    return apiGetJson<UserMovementView | ErrorResponse>(`/api/accounting/user/entry/${id}`, {})
      .then((response) => {
        if (typeof response === 'object' && (response as ErrorResponse).error) {
          enqueueSnackbar((response as ErrorResponse).errorMessage, { variant: 'error' });
          return undefined;
        } else {
          return response as UserMovementView;
        }
      })
      .catch((err) => {
        enqueueSnackbar(
          err.response?.statusText || 'Errore nel caricamento del movimento',
          { variant: 'error' },
        );
      });

  }, [enqueueSnackbar]);

  const insertUserMovement = useCallback((id: string, movement: UserMovement) => {
    return apiPut(`/api/accounting/user/entry/${id}`, movement)
      .then(() => {
        enqueueSnackbar('Movimento salvato', { variant: 'success' });
      })
      .catch((err: any) => {
        enqueueSnackbar(
          err.response?.statusText ||
          'Errore nel salvataggio del movimento contabile',
          { variant: 'error' },
        );
      })
  }, [enqueueSnackbar]);

  const updateUserMovement = useCallback((movement: UserMovement) => {
    return apiPost('/api/accounting/user/entry', movement)
      .then(() => {
        enqueueSnackbar('Movimento salvato', { variant: 'success' });
      })
      .catch((err: any) => {
        enqueueSnackbar(
          err.response?.statusText ||
          'Errore nel salvataggio del movimento contabile',
          { variant: 'error' },
        );
      })
  }, [enqueueSnackbar]);

  return { getUserMovement, insertUserMovement, updateUserMovement }
}