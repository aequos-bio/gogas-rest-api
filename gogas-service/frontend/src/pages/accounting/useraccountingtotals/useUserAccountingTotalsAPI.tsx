import { useCallback, useState } from "react";
import { useSnackbar } from "notistack";
import { UserAccountingTotal } from "./types";
import { DataResponse, ErrorResponse } from "../../../store/types";
import { apiGetJson } from "../../../utils/axios_utils";
import { orderBy } from "lodash";

export const useUserAccountingTotalsAPI = (manageFriends: boolean) => {
  const { enqueueSnackbar } = useSnackbar();
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [userTotals, setUserTotals] = useState<UserAccountingTotal[]>([]);

  const reload = useCallback(() => {
    var apiPath = manageFriends ? 'friend' : 'user';

    setLoading(true);
    apiGetJson<UserAccountingTotal[]>('/api/accounting/' + apiPath + '/balance', {}).then((response) => {
      setLoading(false);
      if (response) {
        let tot = 0;
        response.forEach((t) => {
          tot += t.Saldo;
        });

        setUserTotals(
          orderBy(
            response,
            [
              (total: UserAccountingTotal) => total.attivo,
              (total: UserAccountingTotal) => total.nome,
              (total: UserAccountingTotal) => total.cognome
            ],
            ['desc', 'asc', 'asc'],
          )
        );
        setTotal(tot);
      }
    });
  }, [enqueueSnackbar]);

  return { total, userTotals, loading, reload };
}