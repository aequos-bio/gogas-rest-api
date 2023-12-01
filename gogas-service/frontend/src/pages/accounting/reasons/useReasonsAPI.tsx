import { useCallback, useState } from "react";
import { Reason } from "./types";
import { useSnackbar } from "notistack";
import { apiDelete, apiGetJson, apiPost } from "../../../utils/axios_utils";
import { ErrorResponse } from "../../../store/types";
import { orderBy } from "lodash";

export const useReasonsAPI = () => {
  const [reasons, setReasons] = useState<Reason[]>([]);
  const [loading, setLoading] = useState(false);
  const { enqueueSnackbar } = useSnackbar();

  const reload = useCallback(() => {
    setLoading(true);
    apiGetJson<Reason[] | ErrorResponse>('/api/accounting/reason/list', {})
      .then((rr) => {
        setLoading(false);
        if (typeof rr === 'object' && (rr as ErrorResponse).error) {
          enqueueSnackbar((rr as ErrorResponse).errorMessage, { variant: 'error' });
        } else {
          setReasons(orderBy(rr, ['reasonCode'], ['asc']));
        }
      })
      .catch((err) => {
        enqueueSnackbar(
          err.response?.statusText || 'Errore nel caricamento delle causali',
          { variant: 'error' },
        );
      });
  }, [enqueueSnackbar]);

  const getReason = useCallback((code: string): Promise<Reason | undefined> => {
    return apiGetJson<Reason | ErrorResponse>(`/api/accounting/reason/${code}`, {})
      .then((rr) => {
        if (typeof rr === 'object' && (rr as ErrorResponse).error) {
          enqueueSnackbar((rr as ErrorResponse).errorMessage, { variant: 'error' });
          return undefined;
        } else {
          return rr as Reason;
        }
      })
      .catch(err => {
        enqueueSnackbar(
          err.response?.statusText || 'Errore nel caricamento delle causali',
          { variant: 'error' }
        );
        return undefined;
      });

  }, [enqueueSnackbar]);

  const saveReason = useCallback((reason: Reason, mode: 'edit' | 'new' | false): Promise<void> => {
    return apiPost('/api/accounting/reason', reason)
      .then(() => {
        enqueueSnackbar(
          `Causale ${mode === 'new' ? 'salvata' : 'modificata'}`,
          { variant: 'success' }
        );
      })
      .catch(err => {
        enqueueSnackbar(
          err.response?.statusText || 'Errore nel salvataggio della causale',
          { variant: 'error' }
        );
      });

  }, [enqueueSnackbar]);

  const deleteReason = useCallback((code: string): Promise<void> => {
    return apiDelete(`/api/accounting/reason/${code}`)
      .then(() => {
        reload();
        enqueueSnackbar('Causale eliminata', { variant: 'success' });
      })
      .catch((err) => {
        enqueueSnackbar(
          err.response?.statusText || "Errore nell'eliminazione della causale",
          { variant: 'error' },
        );
      });

  }, [enqueueSnackbar]);

  return { reasons, loading, reload, getReason, saveReason, deleteReason }
} 