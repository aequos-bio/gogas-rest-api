import { orderBy } from "lodash";
import { useEffect, useState } from "react";
import useJwt from "../../hooks/JwtHooks";
import { apiGetJson } from "../../utils/axios_utils";
import { UserOpenOrder } from "./types";

export const useUserOrdersAPI = () => {
  const [openOrders, setOpenOrders] = useState<UserOpenOrder[]>([]);
  const jwt = useJwt();

  useEffect(() => {
    if (!jwt || !jwt.id || jwt.expired) return;
    apiGetJson<UserOpenOrder[]>('/api/order/user/open').then((orders) =>
      setOpenOrders(orderBy(orders, (o) => o.userOrders.length, 'desc')),
    );
  }, [jwt]);

  return { openOrders };

}