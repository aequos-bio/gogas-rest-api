import { useSnackbar } from "notistack";
import { useCallback, useState } from "react";
import { GasMovement, GasMovementView } from "./types";
import { apiDelete, apiGetJson, apiPost, apiPut } from "../../../utils/axios_utils";
import { useAppSelector } from "../../../store/store";
import { ErrorResponse } from "../../../store/types";

export const useGasMovementsAPI = () => {
  const [loading, setLoading] = useState(false);
  const [gasMovements, setGasMovements] = useState<GasMovementView[]>([]);
  const accounting = useAppSelector(state => state.accounting);
  const { enqueueSnackbar } = useSnackbar();

  const reload = useCallback(() => {
    setLoading(true);
    apiGetJson<GasMovementView[] | ErrorResponse>('/api/accounting/gas/entry/list', {
      dateFrom: `01/01/${accounting.currentYear}`,
      dateTo: `31/12/${accounting.currentYear}`,
    }).then(response => {
      setLoading(false);
      if (typeof response === 'object' && (response as ErrorResponse).error) {
        enqueueSnackbar((response as ErrorResponse).errorMessage, {
          variant: 'error',
        });
      } else {
        setGasMovements(response as GasMovementView[]);
      }
    });
  }, [enqueueSnackbar, accounting]);

  const insertGasMovement = useCallback((id: string, movement: GasMovement) => {
    return apiPut(`/api/accounting/gas/entry/${id}`, movement)
      .then(() => {
        enqueueSnackbar('Movimento salvato', { variant: 'success' });
      })
      .catch(err => {
        enqueueSnackbar(
          err.response?.statusText ||
          'Errore nel salvataggio del movimento contabile',
          { variant: 'error' }
        );
      });
  }, [enqueueSnackbar]);

  const updateGasMovement = useCallback((movement: GasMovement) => {
    return apiPost('/api/accounting/gas/entry', movement)
      .then(() => {
        enqueueSnackbar('Movimento salvato', { variant: 'success' });

      })
      .catch(err => {
        enqueueSnackbar(
          err.response?.statusText ||
          'Errore nel salvataggio del movimento contabile',
          { variant: 'error' }
        );
      });
  }, [enqueueSnackbar]);

  const deleteGasMovement = useCallback((id: string) => {
    return apiDelete(`/api/accounting/gas/entry/${id}`)
      .then(() => {
        enqueueSnackbar('Movimento eliminato', { variant: 'success' });
        reload();
      })
      .catch(err => {
        enqueueSnackbar(`Impossibile eliminare il movimento: ${err}`, {
          variant: 'error',
        });
      });
  }, [enqueueSnackbar]);

  return { gasMovements, loading, reload, insertGasMovement, updateGasMovement, deleteGasMovement };
}