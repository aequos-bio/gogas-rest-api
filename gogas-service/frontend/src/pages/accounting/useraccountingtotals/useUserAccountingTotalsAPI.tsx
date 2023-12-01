import { useCallback, useState } from "react";
import { useSnackbar } from "notistack";
import { UserAccountingTotal } from "./types";
import { DataResponse, ErrorResponse } from "../../../store/types";
import { apiGetJson } from "../../../utils/axios_utils";
import { orderBy } from "lodash";

export const useUserAccountingTotalsAPI = () => {
  const { enqueueSnackbar } = useSnackbar();
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [userTotals, setUserTotals] = useState<UserAccountingTotal[]>([]);

  const reload = useCallback(() => {
    setLoading(true);
    apiGetJson<DataResponse<UserAccountingTotal[]>>('/api/useraccounting/userTotals', {}).then((response) => {
      setLoading(false);
      if (response && response.error) {
        enqueueSnackbar(response.errorMessage, { variant: 'error' });
      } else if (response) {
        let tot = 0;
        response.data.forEach((t) => {
          tot += t.total;
        });

        setUserTotals(
          orderBy(
            response.data,
            [
              (total: UserAccountingTotal) => total.user.enabled,
              (total: UserAccountingTotal) => total.user.firstName,
              (total: UserAccountingTotal) => total.user.lastName
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