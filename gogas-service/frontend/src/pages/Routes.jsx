import React, { useEffect } from "react";
import { connect } from "react-redux";
import { BrowserRouter as Router, Route, Switch } from "react-router-dom";
import Home from "./Home";
import NavBar from "../components/NavBar";
import Login from "./Login";
import Users from "./admin/Users";
import UserAccounting from "./accounting/UserAccounting";
import UserAccountingDetails from "./accounting/UserAccountingDetails";
import Years from './accounting/Years';
import { init } from "../store/actions";
import PrivateRoute from '../components/PrivateRoute';

function Routes({authentication, ...props}) {
	useEffect(() => {
		props.init();
	}, [props]);

	return (
		<Router basename="/">
			<>
				<Switch>
					<Route exact path={["/login"]} component={null} />
					<Route path={["/"]} component={NavBar} />
				</Switch>

				<Switch>
					<Route path="/login" component={Login} />
					<PrivateRoute
						exact
						path="/"
						component={Home}
						jwtToken={authentication.jwtToken}
					/>
					<PrivateRoute
						exact
						path="/users"
						component={Users}
						jwtToken={authentication.jwtToken}
					/>
					<PrivateRoute
						exact
						path="/useraccounting"
						component={UserAccounting}
						jwtToken={authentication.jwtToken}
					/>
					<PrivateRoute
						exact
						path="/useraccountingdetails"
						component={UserAccountingDetails}
						jwtToken={authentication.jwtToken}
					/>
					<PrivateRoute
						exact
						path="/years"
						component={Years}
						jwtToken={authentication.jwtToken}
					/>
				</Switch>
			</>
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
)(Routes);
