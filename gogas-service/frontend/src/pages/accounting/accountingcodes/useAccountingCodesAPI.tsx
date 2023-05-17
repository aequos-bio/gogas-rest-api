import { useCallback, useState } from "react";
import { AccountingCode } from "./types";
import { useSnackbar } from "notistack";
import { apiGetJson, apiPut } from "../../../utils/axios_utils";
import { ErrorResponse } from "../../../store/types";
import { orderBy } from "lodash";

export const useAccountingCodesAPI = () => {
  const [accountingCodes, setAccountingCodes] = useState<AccountingCode[]>([]);
  const [loading, setLoading] = useState(false);
  const { enqueueSnackbar } = useSnackbar();

  const reload = useCallback(() => {
    setLoading(true);
    apiGetJson<AccountingCode[] | ErrorResponse>('/api/ordertype/accounting', {}).then((oo) => {
      setLoading(false);
      if (typeof oo === 'object' && (oo as ErrorResponse).error) {
        enqueueSnackbar((oo as ErrorResponse).errorMessage, { variant: 'error' });
      } else {
        setAccountingCodes(
          orderBy(oo, [
            (o: AccountingCode) => (o.id === 'aequos' ? 'A' : 'Z'),
            (o: AccountingCode) => o.description,
          ]),
        );
      }
    });
  }, [enqueueSnackbar]);

  const saveAccountingCode = useCallback((id: string, accountingCode: string) => {
    return apiPut(`/api/ordertype/${id}/accounting`, { accountingCode })
      .then(() => {
        enqueueSnackbar(`Codice contabile modificato'}`, { variant: 'success' });
      })
      .catch(err => {
        enqueueSnackbar(
          err.response?.statusText || 'Errore nel salvataggio della causale',
          { variant: 'error' }
        );
      });

  }, []);

  return { accountingCodes, loading, reload, saveAccountingCode }
}