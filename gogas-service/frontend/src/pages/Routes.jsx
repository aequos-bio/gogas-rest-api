/* eslint-disable react/no-array-index-key */
import React, { useEffect } from 'react';
import { connect } from 'react-redux';
import { BrowserRouter as Router, Route, Switch } from 'react-router-dom';
import Home from './Home';
import NavBar from '../components/NavBar';
import Login from './Login';
import Users from './admin/users/Users';
import Reasons from './admin/reasons/Reasons';
import GasAccounting from './accounting/GasAccounting';
import InvoiceManagement from './accounting/invoicemanagement/InvoiceManagement';
import UserAccounting from './accounting/UserAccounting';
import UserAccountingDetails from './accounting/UserAccountingDetails';
import Years from './accounting/Years';
import OrderTypes from './admin/orderTypes/OrderTypes';
import AccountingCodes from './admin/accountingcodes/AccountingCodes';
import Managers from './admin/Managers';
import { init } from '../store/actions';
import PrivateRoute from '../components/PrivateRoute';

const privateRoutes = [
  { path: '/', component: Home },
  { path: '/years', component: Years },
  { path: '/gasaccounting', component: GasAccounting },
  { path: '/invoices', component: InvoiceManagement },
  { path: '/useraccounting', component: UserAccounting },
  { path: '/useraccountingdetails', component: UserAccountingDetails },
  { path: '/users', component: Users },
  { path: '/reasons', component: Reasons },
  { path: '/ordertypes', component: OrderTypes },
  { path: '/accountingcodes', component: AccountingCodes },
  { path: '/managers', component: Managers },
];

function Routes({ authentication, ...props }) {
  useEffect(() => {
    props.init();
  }, [props]);

  return (
    <Router basename="/">
      <>
        <Switch>
          <Route exact path={['/login']} component={null} />
          <Route path={['/']} component={NavBar} />
        </Switch>

        <Switch>
          <Route path="/login" component={Login} />

          {privateRoutes.map((pr, i) => (
            <PrivateRoute
              key={`route-${i}`}
              exact
              path={pr.path}
              component={pr.component}
              jwtToken={authentication.jwtToken}
            />
          ))}
        </Switch>
      </>
    </Router>
  );
}

const mapStateToProps = state => {
  return {
    authentication: state.authentication,
  };
};

const mapDispatchToProps = {
  init,
};

export default connect(mapStateToProps, mapDispatchToProps)(Routes);
