import { useCallback, useState } from "react";
import { OrderTypeManager } from "./types";
import { apiGetJson } from "../../../utils/axios_utils";
import { ErrorResponse } from "../../../store/types";
import { useSnackbar } from "notistack";

const buildManagersMap = (managersList: OrderTypeManager[]) => {
  const _managers: { [id: string]: OrderTypeManager[] } = {};
  (managersList).forEach((manager: OrderTypeManager) => {
    let list: OrderTypeManager[];
    if (_managers[manager.userId]) {
      list = _managers[manager.userId];
    } else {
      list = [];
      _managers[manager.userId] = list;
    }
    list.push(manager);
  });
  return _managers;
}

export const useManagersAPI = () => {
  const { enqueueSnackbar } = useSnackbar();
  const [managers, setManagers] = useState<{ [userId: string]: OrderTypeManager[] }>({});
  const [loading, setLoading] = useState(false);

  const reload = useCallback(() => {
    setLoading(true);
    apiGetJson<OrderTypeManager[] | ErrorResponse>('/api/ordertype/manager/list', {}).then((managersList) => {
      setLoading(false);
      if (typeof managersList === 'object' && (managersList as ErrorResponse).error) {
        enqueueSnackbar((managersList as ErrorResponse).errorMessage, { variant: 'error' });
      } else {
        setManagers(buildManagersMap(managersList as OrderTypeManager[] || []));
      }
    });
  }, [enqueueSnackbar]);

  const reloadSync = useCallback(async () => {
    setLoading(true);
    var managersList = await apiGetJson<OrderTypeManager[] | ErrorResponse>('/api/ordertype/manager/list', {});
    setLoading(false);

    if (typeof managersList === 'object' && (managersList as ErrorResponse).error) {
      enqueueSnackbar((managersList as ErrorResponse).errorMessage, { variant: 'error' });
      return {};
    }

    return buildManagersMap(managersList as OrderTypeManager[] || []);
  }, [enqueueSnackbar]);

  return { managers, loading, reload, reloadSync };
}

