import { useCallback, useState } from "react";
import { useSnackbar } from "notistack";
import { orderBy } from "lodash";
import { apiDelete, apiGetJson } from "../../../utils/axios_utils";
import { ErrorResponse } from "../../../store/types";
import { UserTransaction } from './types';

export const useUserTransactionsAPI = (id: string, friend: boolean) => {
  const { enqueueSnackbar } = useSnackbar();
  const [transactions, setTransactions] = useState<UserTransaction[]>([]);
  const [totals, setTotals] = useState({ accrediti: 0, addebiti: 0 });
  const [loading, setLoading] = useState(false);

  const apiPath = friend ? 'friend' : 'user';

  const reload = useCallback(() => {
    return apiGetJson<{ movimenti: UserTransaction[] } | ErrorResponse>(
      `/api/accounting/${apiPath}/balance/${id}`,
      {},
    ).then((response) => {
      setLoading(false);
      if (typeof response === 'object' && (response as ErrorResponse).error) {
        enqueueSnackbar((response as ErrorResponse).errorMessage, { variant: 'error' });
        setTransactions([]);
        setTotals({ accrediti: 0, addebiti: 0 });
      } else {
        const tt = (response as { movimenti: UserTransaction[] }).movimenti; //orderBy((response as { movimenti: UserTransaction[] }).movimenti, 'data', 'desc');
        let saldo = 0;
        let accrediti = 0;
        let addebiti = 0;
        if (tt.length)
          for (let f = tt.length - 1; f >= 0; f--) {
            tt[f].saldo = saldo + tt[f].importo;
            saldo = tt[f].saldo;
            if (tt[f].importo < 0) {
              addebiti += -1 * tt[f].importo;
            } else {
              accrediti += tt[f].importo;
            }
          }
        setTransactions(tt);
        setTotals({ accrediti, addebiti });
      }
    });
  }, []);

  const deleteTransaction = useCallback((id: string) => {
    return apiDelete(`/api/accounting/${apiPath}/entry/${id}`)
      .then(() => {
        reload();
        enqueueSnackbar('Movimento eliminato', { variant: 'success' });
      })
      .catch((err) => {
        enqueueSnackbar(
          err.response?.statusText || "Errore nell'eliminazione del movimento",
          { variant: 'error' },
        );
      });
  }, []);

  return { transactions, totals, loading, reload, deleteTransaction }
}