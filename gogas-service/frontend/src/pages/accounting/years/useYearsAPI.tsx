import { useCallback, useState } from "react";
import moment from 'moment-timezone';
import { Year } from "./types";
import { apiGetJson, apiPost, apiPut } from "../../../utils/axios_utils";
import { useSnackbar } from "notistack";
import { ErrorResponse } from "../../../store/types";
import { orderBy } from "lodash";

export const useYearsAPI = () => {
  const [years, setYears] = useState<Year[]>([]);
  const [loading, setLoading] = useState(false);
  const { enqueueSnackbar } = useSnackbar();

  const checkCurrentYear = (_years: Year[]) => {
    const currentYear = Number.parseInt(moment().format('YYYY'), 10);
    const existing = _years.filter((y) => y.year === currentYear);
    if (!existing.length) {
      const result = confirm(
        `Vuoi aprire un nuovo anno contabile per il ${currentYear}?`,
      );
      if (result) {
        apiPost(`/api/year/${currentYear}`).then((y: any | ErrorResponse) => {
          if (typeof y === 'object' && (y as ErrorResponse).error) {
            enqueueSnackbar((y as ErrorResponse).errorMessage, { variant: 'error' });
          } else {
            alert(`Creato nuovo anno contabile ${y.data.data.year}`);
            reload();
          }
        });
      }
    }
  };

  const reload = useCallback(() => {
    setLoading(true);
    apiGetJson<{ data: Year[] } | ErrorResponse>('/api/year/all', {}).then((yy) => {
      setLoading(false);
      if (typeof yy === 'object' && (yy as ErrorResponse).error) {
        enqueueSnackbar((yy as ErrorResponse).errorMessage, { variant: 'error' });
      } else {
        setYears(orderBy((yy as { data: Year[] }).data, 'year', 'desc'));
        checkCurrentYear((yy as { data: Year[] }).data);
      }
    });
  }, []);

  const closeYear = useCallback((year: number) => {
    return apiPut(`/api/year/close/${year}`)
      .then(() => {
        reload();
        enqueueSnackbar('Anno chiuso', { variant: 'success' });
      })
      .catch((err) => {
        enqueueSnackbar(
          err.response?.statusText ||
          "Errore nell'eliminazione della categoria",
          { variant: 'error' },
        );
      });
  }, []);

  return { years, loading, reload, closeYear }
}