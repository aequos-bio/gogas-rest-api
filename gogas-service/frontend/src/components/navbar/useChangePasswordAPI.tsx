import { useCallback, useEffect, useState } from "react";
import useJwt from "../../hooks/JwtHooks";
import { apiPut, extractResponseFromError } from "../../utils/axios_utils";
import { useSnackbar } from "notistack";
import { ErrorResponse } from "../../store/types";

export const useChangePasswordAPI = () => {
  const jwt = useJwt();
  const { enqueueSnackbar } = useSnackbar();
  const [loading, setLoading] = useState<boolean>(false);

  const changePassword = useCallback(async (oldPassword: string, newPassword: string, confirmPassword: string) : Promise<ChangePasswordResult> => {
    if (newPassword.length < 8) {
      return { status: ChangePasswordStatus.PASSWORD_LENGTH };
    }

    if (newPassword != confirmPassword) {
      return { status: ChangePasswordStatus.PASSWORD_MISMATCH };
    }

    if (!jwt || !jwt.id || jwt.expired) {
      return { status: ChangePasswordStatus.GENERIC_ERROR, errorMessage: 'Sessione scaduta' };
    }

    const params = {
      oldPassword: oldPassword,
      newPassword: newPassword,
    };

    setLoading(true);
    try {
      await apiPut('/api/user/password/change', params);
      enqueueSnackbar('Password aggiornata con successo');
      return { status: ChangePasswordStatus.OK };
    } catch (error) {
      var response = extractResponseFromError(error);

      if (!response) {
        throw error;
      }

      if (response.status == 400 && response.data.message == 'Wrong password') {
        return { status: ChangePasswordStatus.WRONG_PASSWORD };
      } else {
        return { status: ChangePasswordStatus.GENERIC_ERROR, errorMessage: response.data.message as string };
      }
    } finally {
      setLoading(false);
    }
  }, [jwt]);

  return { loading, changePassword }
}

export interface ChangePasswordResult {
  status: ChangePasswordStatus;
  errorMessage?: string;
}

export enum ChangePasswordStatus {
  OK,
  GENERIC_ERROR,
  WRONG_PASSWORD,
  PASSWORD_MISMATCH,
  PASSWORD_LENGTH,
}