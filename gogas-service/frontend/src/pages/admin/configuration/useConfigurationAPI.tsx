import { useCallback, useState } from 'react';
import { orderBy } from 'lodash';
import { useSnackbar } from 'notistack';
import { apiGetJson, apiPut } from '../../../utils/axios_utils';
import { ErrorResponse } from '../../../store/types';
import { ConfigurationItem } from './types';

const errorText = (err: any, fallback: string) =>
  err?.response?.data
    ? err.response.data.debugMessage || err.response.data.message || err.response.data.error
    : `${fallback}: ${err}`;

/**
 * Data hook for the Configuration (Configurazione generale) page.
 *
 * Endpoints: GET /api/configuration (visible items),
 * PUT /api/configuration (update a single item by key — the whole item is
 * sent back, matching the legacy grid behaviour).
 */
export const useConfigurationAPI = () => {
  const [items, setItems] = useState<ConfigurationItem[]>([]);
  const [loading, setLoading] = useState(false);
  const { enqueueSnackbar } = useSnackbar();

  const reload = useCallback(() => {
    setLoading(true);
    apiGetJson<ConfigurationItem[] | ErrorResponse>('/api/configuration', {}).then((res) => {
      setLoading(false);
      if (typeof res === 'object' && (res as ErrorResponse).error) {
        enqueueSnackbar((res as ErrorResponse).errorMessage, { variant: 'error' });
      } else {
        setItems(orderBy(res as ConfigurationItem[], ['description'], ['asc']));
      }
    });
  }, [enqueueSnackbar]);

  const updateItem = useCallback(
    (item: ConfigurationItem): Promise<boolean> => {
      return apiPut('/api/configuration', item)
        .then(() => {
          enqueueSnackbar('Configurazione aggiornata con successo', { variant: 'success' });
          reload();
          return true;
        })
        .catch((err) => {
          enqueueSnackbar(errorText(err, "Errore durante l'aggiornamento della configurazione"), {
            variant: 'error',
          });
          return false;
        });
    },
    [enqueueSnackbar, reload],
  );

  return { items, loading, reload, updateItem };
};
