import { useCallback, useState } from "react";
import { useSnackbar } from "notistack";
import moment from "moment-timezone";
import { UserMovementView } from "../useraccountingdetail/types";
import { GasMovementView } from "../gasmovements/types";
import { apiGetJson } from "../../../utils/axios_utils";
import { ErrorResponse } from "../../../store/types";

const checkDateFormat = (data: string) => {
  return data.includes('/')
    ? moment(data, 'DD/MM/YYYY').format('YYYY-MM-DD')
    : data;
};

export const useGasAccountingAPI = (year: number) => {
  const [loading, setLoading] = useState(false);
  const [userMovements, setUserMovements] = useState<UserMovementView[]>([]);
  const [gasMovements, setGasMovements] = useState<GasMovementView[]>([]);
  const { enqueueSnackbar } = useSnackbar();

  const reload = useCallback(() => {
    setLoading(true);

    Promise.all([
      apiGetJson<UserMovementView[] | ErrorResponse>(`/api/accounting/user/entry/list`, {
        dateFrom: `01/01/${year}`,
        dateTo: `31/12/${year}`,
      }),
      apiGetJson<GasMovementView[] | ErrorResponse>(`/api/accounting/gas/report/${year}`, {}),
    ]).then(([user, gas]) => {
      setLoading(false);
      if ((typeof user === 'object' && (user as ErrorResponse).error) ||
        (typeof gas === 'object' && (gas as ErrorResponse).error)) {
        enqueueSnackbar((user as ErrorResponse).errorMessage || (gas as ErrorResponse).errorMessage, {
          variant: 'error',
        });
      } else {
        setUserMovements(
          (user as UserMovementView[]).map((u) => ({
            ...u,
            data: checkDateFormat(u.data),
          })),
        );
        setGasMovements(
          (gas as GasMovementView[]).map((g) => ({
            ...g,
            data: checkDateFormat(g.data),
          })),
        );
      }
    });
  }, [enqueueSnackbar, year]);

  return { userMovements, gasMovements, loading, reload }
}