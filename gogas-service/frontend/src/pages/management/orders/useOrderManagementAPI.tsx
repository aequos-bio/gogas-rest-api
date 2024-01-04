import { useCallback, useState } from "react"
import { useSnackbar } from "notistack";
import { ManagedOrder, ManagedOrderQueryFilter } from "./types";
import { apiPostJson } from "../../../utils/axios_utils";
import { ErrorResponse } from "../../../store/types";
import moment from "moment";

const defaultFilter: ManagedOrderQueryFilter = {
  filterscount: 0,
  groupscount: 0,
  sortorder: "",
  pagenum: 0,
  pagesize: 10,
  recordstartindex: 0,
  recordendindex: 28,
  orderType: "",
  paid: "",
  dueDateFrom: moment().add(-2, 'month').format('DD/MM/YYYY'),
  dueDateTo: "",
  deliveryDateFrom: "",
  deliveryDateTo: "",
  status: ["0", "1"]
}

export const useOrderManaggementAPI = () => {
  const [filter, setFilter] = useState(defaultFilter);
  const [loading, setLoading] = useState(false);
  const [orders, setOrders] = useState<ManagedOrder[]>([]);
  const { enqueueSnackbar } = useSnackbar();

  const reload = useCallback((newfilter?: ManagedOrderQueryFilter) => {
    setLoading(true);
    if (newfilter) setFilter(newfilter);
    apiPostJson<ManagedOrder[] | ErrorResponse>('/api/order/manage/list', newfilter || filter).then(response => {
      setLoading(false);
      if (typeof response === 'object' && (response as ErrorResponse).error) {
        enqueueSnackbar((response as ErrorResponse).errorMessage, {
          variant: 'error',
        });
      } else {
        setOrders(response as ManagedOrder[]);
      }
    });
  }, [enqueueSnackbar]);

  return { filter, loading, orders, reload };
}