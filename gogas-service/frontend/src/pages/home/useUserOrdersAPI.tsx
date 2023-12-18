import { orderBy } from "lodash";
import { useEffect, useState } from "react";
import useJwt from "../../hooks/JwtHooks";
import { apiGetJson, apiPostJson } from "../../utils/axios_utils";
import { UserOpenOrder, UserDeliveryOrder } from "./types";
import { Friend } from '../users/types';

export const useUserOrdersAPI = () => {
  const [openOrders, setOpenOrders] = useState<UserOpenOrder[]>([]);
  const [userSelect, setUserSelect] = useState<UserSelect[]>([]);
  const [deliveryOrders, setDeliveryOrders] = useState<UserDeliveryOrder[]>([]);

  const jwt = useJwt();

  useEffect(() => {
    if (!jwt || !jwt.id || jwt.expired) return;
    apiGetJson<UserOpenOrder[]>('/api/order/user/open').then((orders) =>
      setOpenOrders(orderBy(orders, (o) => o.userOrders.length, 'desc')),
    );
    apiGetJson<UserSelect[]>('/api/friend/select?includeReferral=true').then((userSelect) =>
      setUserSelect(orderBy(userSelect, (f) => f.description, 'asc')),
    );
    apiPostJson<UserDeliveryOrder[]>('/api/order/user/list', {inDelivery: true}).then((orders) =>
      setDeliveryOrders(orders),
    );
  }, [jwt]);

  return { openOrders, userSelect, deliveryOrders };

}