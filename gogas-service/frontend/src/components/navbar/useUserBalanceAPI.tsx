import { useCallback, useEffect, useState } from "react";
import { useSnackbar } from "notistack";
import useJwt from "../../hooks/JwtHooks";
import { apiGetJson } from "../../utils/axios_utils";
import { ErrorResponse } from "../../store/types";

export const useUserBalanceAPI = () => {
  const jwt = useJwt();
  const { enqueueSnackbar } = useSnackbar();
  const [balance, setBalance] = useState(0);
  const [loading, setLoading] = useState(false);


  useEffect(() => {
    reload();
  }, [jwt]);

  const reload = useCallback(() => {
    if (!jwt || !jwt.id || jwt.expired) return;
    apiGetJson<number | ErrorResponse>(`/api/accounting/user/${jwt.id}/balance`)
      .then(response => {
        setLoading(false);
        if (typeof response === 'object' && (response as ErrorResponse).error) {
          enqueueSnackbar(`Errore nel caricamento del saldo: ${(response as ErrorResponse).errorMessage}`, {
            variant: 'error',
          });
        } else {
          setBalance(response as number);
        }
      })
      .catch((err) => {
        enqueueSnackbar(
          `Errore nel caricamento del saldo: ${err.debugMessage || err.errorMessage}`,
          { variant: 'error' },
        );
      });
  }, [jwt, enqueueSnackbar]);

  return { balance, loading, reload }
}