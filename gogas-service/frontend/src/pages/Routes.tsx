import React, { useEffect } from 'react';
import { BrowserRouter as Router, Route, Switch } from 'react-router-dom';
import NavBar from '../components/navbar/NavBar';
import Login from './login/Login';
import HomePage from './home/HomePage';
import YearsPage from './accounting/years/YearsPage';
import GasAccountingPage from './accounting/gasaccounting/GasAccountingPage';
import GasMovementsPage from './accounting/gasmovements/GasMovementsPage';
import InvoiceManagementPage from './accounting/invoicemanagement/InvoiceManagementPage';
import UserAccountingTotalsPage from './accounting/useraccountingtotals/UserAccountingTotalsPage';
import UserAccountingDetailPage from './accounting/useraccountingdetail/UserAccountingDetailPage';
import UsersPage from './admin/users/UsersPage';
import ReasonsPage from './accounting/reasons/ReasonsPage';
import OrderTypesPage from './admin/orderTypes/OrderTypesPage';
import AccountingCodesPage from './accounting/accountingcodes/AccountingCodesPage';
import ManagersPage from './admin/managers/ManagersPage';
import PrivateRoute from '../components/PrivateRoute';
import { init } from '../store/features/info.slice';
import { useAppDispatch } from '../store/store';
import {
  LegacyOrderManagerPage,
  LegacyProductsManagerPage,
  LegacySuppliersManagerPage,
  LegacyConfigurationPage,
  LegacyUserOrderPage,
  LegacyUserOrderDetailsPage,
  LegacyFriendOrderDetailsPage,
  LegacyManageFriendsPage
} from './legacy/LegacyPage';
import OrderManagementPage from './management/orders/OrderListManagementPage';
import OrderDetailPage from './management/order/OrderDetailManagementPage';

const privateRoutes = [
  { path: '/', component: HomePage },
  { path: '/years', component: YearsPage },
  { path: '/gasaccounting', component: GasAccountingPage },
  { path: '/gasmovements', component: GasMovementsPage },
  { path: '/invoices', component: InvoiceManagementPage },
  { path: '/useraccounting', component: UserAccountingTotalsPage },
  { path: '/useraccountingdetails', component: UserAccountingDetailPage },
  { path: '/friendsaccounting', render: () => <UserAccountingTotalsPage friends={true} /> },
  { path: '/friendaccountingdetails', render: () => <UserAccountingDetailPage friends={true} /> },
  { path: '/users', component: UsersPage },
  { path: '/reasons', component: ReasonsPage },
  { path: '/ordertypes', component: OrderTypesPage },
  { path: '/accountingcodes', component: AccountingCodesPage },
  { path: '/managers', component: ManagersPage },
  { path: '/orders', component: OrderManagementPage },
  { path: '/orders/:id', component: OrderDetailPage },
  { path: '/legacy/orderslist', component: LegacyOrderManagerPage },
  { path: '/legacy/products', component: LegacyProductsManagerPage },
  { path: '/legacy/suppliers', component: LegacySuppliersManagerPage },
  { path: '/legacy/configuration', component: LegacyConfigurationPage },
  { path: '/legacy/ordershistory', component: LegacyUserOrderPage },
  { path: '/legacy/ordersdetails', component: LegacyUserOrderDetailsPage },
  { path: '/legacy/friendorders', component: LegacyFriendOrderDetailsPage },
  { path: '/legacy/managefriends', component: LegacyManageFriendsPage },
];

export const Routes = () => {
  const dispatch = useAppDispatch();

  useEffect(() => {
    dispatch(init());
  }, [dispatch]);

  return (
    <Router basename='/'>
      <>
        <Switch>
          <Route exact path={['/login']} component={undefined} />
          <Route path={['/']} component={NavBar} />
        </Switch>

        <Switch>
          <Route path='/login' component={Login} />

          {privateRoutes.map((pr, i) => (
            <PrivateRoute
              key={`route-${i}`}
              exact
              path={pr.path}
              component={pr.component}
              render={pr.render}
            />
          ))}
        </Switch>
      </>
    </Router>
  );
};

export default Routes;
