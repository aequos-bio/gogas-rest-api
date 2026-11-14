import { useCallback, useState } from 'react';
import { orderBy } from 'lodash';
import { useSnackbar } from 'notistack';
import { apiDelete, apiGetJson, apiPost, apiPut } from '../../utils/axios_utils';
import { ErrorResponse } from '../../store/types';
import { Friend, FriendInput } from './types';

const errorText = (err: any, fallback: string) =>
  err?.response?.data
    ? err.response.data.debugMessage || err.response.data.message || err.response.data.error
    : `${fallback}: ${err}`;

const isError = (res: any): res is ErrorResponse =>
  res && typeof res === 'object' && (res as ErrorResponse).error;

/**
 * Data hook for the "Manage friends" (Gestione amici) page.
 *
 * Endpoints:
 *  - GET    /api/friend/list      (friends of the current user)
 *  - POST   /api/friend           (create)
 *  - PUT    /api/friend/{id}      (update)
 *  - DELETE /api/friend/{id}      (delete)
 *  - GET    /api/user/exists?username=  (username availability, create only)
 */
export const useFriendsAPI = () => {
  const [friends, setFriends] = useState<Friend[]>([]);
  const [loading, setLoading] = useState(false);
  const { enqueueSnackbar } = useSnackbar();

  const reload = useCallback(() => {
    setLoading(true);
    apiGetJson<Friend[] | ErrorResponse>('/api/friend/list', {}).then((res) => {
      setLoading(false);
      if (isError(res)) {
        enqueueSnackbar((res as ErrorResponse).errorMessage, { variant: 'error' });
      } else {
        setFriends(orderBy(res as Friend[], ['attivo', 'cognome', 'nome'], ['desc', 'asc', 'asc']));
      }
    });
  }, [enqueueSnackbar]);

  const usernameExists = useCallback((username: string): Promise<boolean> => {
    return apiGetJson<{ data: boolean }>('/api/user/exists', { username })
      .then((res) => !!(res && res.data))
      .catch(() => false);
  }, []);

  const createFriend = useCallback(
    (friend: FriendInput): Promise<boolean> => {
      return apiPost('/api/friend', friend)
        .then(() => {
          enqueueSnackbar('Amico creato con successo', { variant: 'success' });
          reload();
          return true;
        })
        .catch((err) => {
          enqueueSnackbar(errorText(err, "Errore durante la creazione dell'amico"), { variant: 'error' });
          return false;
        });
    },
    [enqueueSnackbar, reload],
  );

  const updateFriend = useCallback(
    (id: string, friend: FriendInput): Promise<boolean> => {
      return apiPut(`/api/friend/${id}`, friend)
        .then(() => {
          enqueueSnackbar('Amico modificato con successo', { variant: 'success' });
          reload();
          return true;
        })
        .catch((err) => {
          enqueueSnackbar(errorText(err, "Errore durante la modifica dell'amico"), { variant: 'error' });
          return false;
        });
    },
    [enqueueSnackbar, reload],
  );

  const deleteFriend = useCallback(
    (id: string): Promise<void> => {
      return apiDelete(`/api/friend/${id}`)
        .then(() => {
          enqueueSnackbar('Amico eliminato con successo', { variant: 'success' });
          reload();
        })
        .catch((err) => {
          enqueueSnackbar(errorText(err, "Errore durante l'eliminazione dell'amico"), { variant: 'error' });
        });
    },
    [enqueueSnackbar, reload],
  );

  return { friends, loading, reload, usernameExists, createFriend, updateFriend, deleteFriend };
};
