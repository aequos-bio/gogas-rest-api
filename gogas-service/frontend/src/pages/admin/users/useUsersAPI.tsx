import { useCallback, useEffect, useState } from "react";
import { orderBy } from 'lodash';
import { apiDelete, apiGetJson, apiPut } from "../../../utils/axios_utils";
import { User, BlacklistItem } from "./types";
import { ErrorResponse } from "../../../store/types";
import { useSnackbar } from "notistack";

export const useUsersAPI = (sort: string) => {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(false);
  const { enqueueSnackbar } = useSnackbar();

  const reload = useCallback(() => {
    setLoading(true);

    Promise.all([
      apiGetJson<User[] | ErrorResponse>('/api/user/list', {}),
      apiGetJson<BlacklistItem[] | ErrorResponse>('/api/ordertype/blacklist/count', {})
    ]).then(([uu, bl]) => {
      setLoading(false);

      if (typeof uu === 'object' && (uu as ErrorResponse).error) {
        enqueueSnackbar((uu as ErrorResponse).errorMessage, { variant: 'error' });
        return;
      }

      if (typeof bl === 'object' && (bl as ErrorResponse).error) {
        enqueueSnackbar((bl as ErrorResponse).errorMessage, { variant: 'error' });
        return;
      }

      var blacklistMap = (bl as BlacklistItem[]).reduce(function(map: Map<string, number>, entry) {
          map.set(entry.id, Number(entry.description));
          return map;
      }, new Map<string, number>);

      (uu as User[]).forEach(function (user) {
        user.blacklistCount = blacklistMap.get(user.idUtente);
      });

      setUsers(
        orderBy(
          uu,
          [
            'attivo',
            sort === 'NC' ? 'nome' : 'cognome',
            sort === 'NC' ? 'cognome' : 'nome',
          ],
          ['desc', 'asc', 'asc'],
        ),
      );
    });
  }, [sort, enqueueSnackbar]);

  const deleteUser = useCallback((id: string): Promise<void> => {
    return apiDelete(`/api/user/${id}`)
      .then(() => {
        enqueueSnackbar('Utente eliminato', { variant: 'success' });
        reload();
      })
      .catch((err) => {
        enqueueSnackbar(
          err.response && err.response.data
            ? err.response.data.debugMessage || err.response.data.message
            : `Errore durante l'eliminazione dell'utente: ${err}`,
          {
            variant: 'error',
          },
        );
      });
  }, [enqueueSnackbar]);

  const enableUser = useCallback((user: User): Promise<void> => {
    return apiPut(`/api/user/${user.idUtente}`, { ...user, attivo: true })
      .then(() => {
        enqueueSnackbar('Utente abilitato', { variant: 'success' });
        reload();
      })
      .catch((err) => {
        enqueueSnackbar(
          err.response && err.response.data
            ? err.response.data.debugMessage || err.response.data.message
            : `Errore durante l'abilitazione dell'utente: ${err}`,
          { variant: 'error' },
        );
      });
  }, [enqueueSnackbar]);

  const disableUser = useCallback((user: User): Promise<void> => {
    return apiPut(`/api/user/${user.idUtente}`, { ...user, attivo: false })
      .then(() => {
        enqueueSnackbar('Utente disabilitato', { variant: 'success' });
        reload();
      })
      .catch((err) => {
        enqueueSnackbar(
          err.response && err.response.data
            ? err.response.data.debugMessage || err.response.data.message
            : `Errore durante la disabilitazione dell'utente: ${err}`,
          { variant: 'error' },
        );
      });
  }, [enqueueSnackbar]);

  const resetPassword = useCallback((id: string): Promise<void> => {
    return apiPut(`/api/user/${id}/password/reset`)
      .then(() => {
        enqueueSnackbar("La password dell'utente è stata resettata", {
          variant: 'success',
        });
        reload();
      })
      .catch((err) => {
        enqueueSnackbar(
          err.response && err.response.data
            ? err.response.data.debugMessage || err.response.data.message || err.response.data.error
            : `Errore durante il reset della password dell'utente: ${err}`,
          { variant: 'error' },
        );
      });
  }, [enqueueSnackbar]);

  const getUser = useCallback((id: string) => {
    return apiGetJson<User | ErrorResponse>(`/api/user/${id}`, {}).then((u) => {
      if (typeof u === 'object' && (u as ErrorResponse).error) {
        enqueueSnackbar((u as ErrorResponse).errorMessage, { variant: 'error' });
      } else {
        return u as User;
      }
    });
  }, [])

  return {
    users, loading, reload, deleteUser, enableUser, disableUser, resetPassword, getUser
  }
}