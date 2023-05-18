import React, { useEffect } from 'react';
import { BrowserRouter as Router, Route, Switch } from 'react-router-dom';
import Home from './home/HomePage';
import NavBar from '../components/NavBar';
import Login from './login/Login';
import Users from './admin/users/Users';
import Reasons from './accounting/reasons/Reasons';
import GasAccounting from './accounting/gasaccounting/GasAccounting';
import InvoiceManagement from './accounting/invoicemanagement/InvoiceManagement';
import UserAccountingTotals from './accounting/useraccountingtotals/UserAccountingTotals';
import UserAccountingDetail from './accounting/useraccountingdetail/UserAccountingDetail';
import Years from './accounting/years/Years';
import OrderTypes from './admin/orderTypes/OrderTypes';
import AccountingCodes from './accounting/accountingcodes/AccountingCodes';
import Managers from './admin/managers/Managers';
import PrivateRoute from '../components/PrivateRoute';
import GasMovements from './accounting/gasmovements/GasMovements';
import { init } from '../store/features/info.slice';
import { RootState, useAppDispatch } from '../store/store';

const privateRoutes = [
  { path: '/', component: Home },
  { path: '/years', component: Years },
  { path: '/gasaccounting', component: GasAccounting },
  { path: '/gasmovements', component: GasMovements },
  { path: '/invoices', component: InvoiceManagement },
  { path: '/useraccounting', component: UserAccountingTotals },
  { path: '/useraccountingdetails', component: UserAccountingDetail },
  { path: '/users', component: Users },
  { path: '/reasons', component: Reasons },
  { path: '/ordertypes', component: OrderTypes },
  { path: '/accountingcodes', component: AccountingCodes },
  { path: '/managers', component: Managers },
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
