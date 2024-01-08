import { useCallback, useState } from "react";
import { apiPut, extractResponseFromError } from "../../utils/axios_utils";
import { useSnackbar } from "notistack";
import { ErrorResponse } from "../../store/types";

export const useResetPasswordAPI = () => {
  const { enqueueSnackbar } = useSnackbar();
  const [loading, setLoading] = useState<boolean>(false);

  const resetPassword = useCallback(async (username: string, email: string) : Promise<ResetPasswordResult> => {
    const params = {
      username: username,
      email: email,
    };

    setLoading(true);
    try {
      await apiPut('/api/user/password/reset', params);
      enqueueSnackbar('Reset password effettuato con successo, a breve riceverai una email con la nuova password');
      return { error: false };
    } catch (error) {
      var response = extractResponseFromError(error);

      if (!response) {
        throw error;
      }

      if (response.status == 401 || response.status == 403) {
        return { error: true, errorMessage: 'Utente non autorizzato' };
      }

      if (response.status == 404) {
        return { error: true, errorMessage: 'Nessun utente trovato, controllare i valori inseriti' };
      }

      return { error: true, errorMessage: response.data.message as string };
    } finally {
      setLoading(false);
    }
  }, []);

  return { loading, resetPassword }
}

export const isValidEmail = (email : string) => {
  const expression: RegExp = /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i;
  return expression.test(email);
}

export interface ResetPasswordResult {
  error: boolean;
  errorMessage?: string;
}