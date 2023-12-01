import { useCallback, useState } from "react";
import { useSnackbar } from "notistack";
import { orderBy } from "lodash";
import { apiDelete, apiGetJson } from "../../../utils/axios_utils";
import { ErrorResponse } from "../../../store/types";
import { UserTransaction } from './types';

export const useUserTransactionsAPI = (id: string) => {
  const { enqueueSnackbar } = useSnackbar();
  const [transactions, setTransactions] = useState<UserTransaction[]>([]);
  const [totals, setTotals] = useState({ accrediti: 0, addebiti: 0 });
  const [loading, setLoading] = useState(false);

  const reload = useCallback(() => {
    return apiGetJson<{ data: UserTransaction[] } | ErrorResponse>(
      `/api/useraccounting/userTransactions?userId=${id}`,
      {},
    ).then((response) => {
      setLoading(false);
      if (typeof response === 'object' && (response as ErrorResponse).error) {
        enqueueSnackbar((response as ErrorResponse).errorMessage, { variant: 'error' });
        setTransactions([]);
        setTotals({ accrediti: 0, addebiti: 0 });
      } else {
        const tt = orderBy((response as { data: UserTransaction[] }).data, 'date', 'desc');
        let saldo = 0;
        let accrediti = 0;
        let addebiti = 0;
        if (tt.length)
          for (let f = tt.length - 1; f >= 0; f--) {
            const m = tt[f].amount * (tt[f].sign === '-' ? -1 : 1);
            tt[f].saldo = saldo + m;
            saldo = tt[f].saldo;
            if (m < 0) {
              addebiti += -1 * m;
            } else {
              accrediti += m;
            }
          }
        setTransactions(tt);
        setTotals({ accrediti, addebiti });
      }
    });
  }, []);

  const deleteTransaction = useCallback((id: string) => {
    return apiDelete(`/api/accounting/user/entry/${id}`)
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