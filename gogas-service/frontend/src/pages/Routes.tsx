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
import LegacyOrderManagerPage from './legacy/LegacyFrame';

const privateRoutes = [
  { path: '/', component: HomePage },
  { path: '/years', component: YearsPage },
  { path: '/gasaccounting', component: GasAccountingPage },
  { path: '/gasmovements', component: GasMovementsPage },
  { path: '/invoices', component: InvoiceManagementPage },
  { path: '/useraccounting', component: UserAccountingTotalsPage },
  { path: '/useraccountingdetails', component: UserAccountingDetailPage },
  { path: '/users', component: UsersPage },
  { path: '/reasons', component: ReasonsPage },
  { path: '/ordertypes', component: OrderTypesPage },
  { path: '/accountingcodes', component: AccountingCodesPage },
  { path: '/managers', component: ManagersPage },
  { path: '/legacy/orderslist', component: LegacyOrderManagerPage },
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
            />
          ))}
        </Switch>
      </>
    </Router>
  );
};

export default Routes;
