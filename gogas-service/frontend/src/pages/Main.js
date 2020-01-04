import React, { Fragment, useEffect } from "react";
import { BrowserRouter as Router, Route, Switch } from "react-router-dom";
import Home from "./Home";
import Login from "./Login";
import Users from "./Users";
import UserAccounting from "./accounting/UserAccounting";
import UserAccountingDetails from "./accounting/UserAccountingDetails";
import { connect } from "react-redux";
import { init } from "../store/actions";
import PageHeader from "../components/PageHeader";
import PrivateRoute from '../components/PrivateRoute';

function Main({info, authentication, init}) {
	useEffect(() => {
		init();
	}, [init]);

	return (
		<Router basename="/">

			<Fragment>
				<Switch>
					<Route exact path={["/login"]} component={null} />
					<Route path={["/"]} component={PageHeader} />
				</Switch>

				<Switch>
					<Route path="/login" component={Login} />
					<PrivateRoute
						exact
						path="/"
						component={Home}
						jwtToken={authentication.jwtToken}
						onLogoutDone={() => { }}
						info={info}
					/>
					<PrivateRoute
						exact
						path="/users"
						component={Users}
						jwtToken={authentication.jwtToken}
						onLogoutDone={() => { }}
						info={info}
					/>
					<PrivateRoute
						exact
						path="/useraccounting"
						component={UserAccounting}
						jwtToken={authentication.jwtToken}
						onLogoutDone={() => { }}
						info={info}
					/>
					<PrivateRoute
						exact
						path="/useraccountingdetails"
						component={UserAccountingDetails}
						jwtToken={authentication.jwtToken}
						onLogoutDone={() => { }}
						info={info}
					/>
				</Switch>
			</Fragment>

		</Router>
	);
}

const mapStateToProps = state => {
	return {
		authentication: state.authentication
	};
};

const mapDispatchToProps = {
	init
};

export default connect(
	mapStateToProps,
	mapDispatchToProps
)(Main);
