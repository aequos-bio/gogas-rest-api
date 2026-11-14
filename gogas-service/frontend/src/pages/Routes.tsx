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
import SuppliersPage from './admin/suppliers/SuppliersPage';
import ConfigurationPage from './admin/configuration/ConfigurationPage';
import ProductsPage from './admin/products/ProductsPage';
import UserOrdersListPage from './orders/user/UserOrdersListPage';
import ManagerOrdersListPage from './orders/manager/ManagerOrdersListPage';
import OrderByProductPage from './orders/manager/detail/OrderByProductPage';
import OrderByUserPage from './orders/manager/detail/OrderByUserPage';
import ManageFriendsPage from './friends/ManageFriendsPage';
import PrivateRoute from '../components/PrivateRoute';
import { init } from '../store/features/info.slice';
import { useAppDispatch } from '../store/store';
import {
  LegacyUserOrderDetailsPage,
  LegacyFriendOrderDetailsPage
} from './legacy/LegacyPage';

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
  { path: '/legacy/orderslist', component: ManagerOrdersListPage },
  { path: '/legacy/orderdetails', component: OrderByProductPage },
  { path: '/legacy/orderdetailsbyuser', component: OrderByUserPage },
  { path: '/legacy/products', component: ProductsPage },
  { path: '/legacy/suppliers', component: SuppliersPage },
  { path: '/legacy/configuration', component: ConfigurationPage },
  { path: '/legacy/ordershistory', component: UserOrdersListPage },
  { path: '/legacy/ordersdetails', component: LegacyUserOrderDetailsPage },
  { path: '/legacy/friendorders', component: LegacyFriendOrderDetailsPage },
  { path: '/legacy/managefriends', component: ManageFriendsPage },
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
