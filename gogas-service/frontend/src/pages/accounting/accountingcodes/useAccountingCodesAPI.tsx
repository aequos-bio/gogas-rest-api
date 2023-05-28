import { useCallback, useState } from "react";
import { AccountingCode } from "./types";
import { useSnackbar } from "notistack";
import { apiGetJson, apiPut } from "../../../utils/axios_utils";
import { ErrorResponse } from "../../../store/types";
import { orderBy } from "lodash";

export const useAccountingCodesAPI = () => {
  const [accountingCodes, setAccountingCodes] = useState<AccountingCode[]>([]);
  const [aequosAccountingCode, setAequosAccountingCode] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const { enqueueSnackbar } = useSnackbar();

  const reload = useCallback(() => {
    setLoading(true);
    apiGetJson<AccountingCode[] | ErrorResponse>('/api/ordertype/accounting', {}).then((response) => {
      setLoading(false);
      if (typeof response === 'object' && (response as ErrorResponse).error) {
        enqueueSnackbar((response as ErrorResponse).errorMessage, { variant: 'error' });
      } else {
        setAccountingCodes(
          orderBy((response as AccountingCode[]), [
            (o: AccountingCode) => (o.id === 'aequos' ? 'A' : 'Z'),
            (o: AccountingCode) => o.description,
          ]),
        );
        const aequos = (response as AccountingCode[]).filter((a) => a.id === 'aequos');
        if (aequos.length) {
          setAequosAccountingCode(aequos[0].accountingCode);
        }

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

  return { accountingCodes, aequosAccountingCode, loading, reload, saveAccountingCode }
}