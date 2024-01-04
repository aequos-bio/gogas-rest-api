import { useCallback, useState } from "react";
import { OrderDetail, OrderProduct } from "./types"
import { apiGetJson } from "../../../utils/axios_utils";
import { ErrorResponse } from "../../../store/types";
import { useSnackbar } from "notistack";

export const useOrderDetailManagementAPI = (id: string) => {
  const [loading, setLoading] = useState(true);
  const [order, setOrder] = useState<OrderDetail | undefined>(undefined);
  const [products, setProducts] = useState<OrderProduct[]>([]);
  const { enqueueSnackbar } = useSnackbar();

  const reload = useCallback(() => {
    setLoading(true);
    Promise.all([
      apiGetJson<OrderDetail | ErrorResponse>(`/api/order/manage/${id}`, {}),
      apiGetJson<OrderProduct[] | ErrorResponse>(`/api/order/manage/${id}/product`, {})
    ]).then(([orderResponse, productsResponse]) => {
      setLoading(false);
      if (typeof orderResponse === 'object' && (orderResponse as ErrorResponse).error) {
        enqueueSnackbar((orderResponse as ErrorResponse).errorMessage, {
          variant: 'error',
        });
      } else {
        setOrder(orderResponse as OrderDetail);
      }

      if (typeof productsResponse === 'object' && (productsResponse as ErrorResponse).error) {
        enqueueSnackbar((productsResponse as ErrorResponse).errorMessage, {
          variant: 'error',
        });
      } else {
        setProducts(productsResponse as OrderProduct[]);
      }
    })
  }, [id]);

  return { loading, order, products, reload }
}