/* eslint-disable react/jsx-props-no-spreading */
import React, {useMemo} from "react";
import { Route, Redirect } from "react-router-dom";
import Jwt from 'jsonwebtoken';
import moment from 'moment-timezone';
import { logout } from "../store/actions";

const PrivateRoute = ({ component: Component, ...rest }) => {
	const validJwt = useMemo(() => {
		if (rest.jwtToken) {
			const jwt = Jwt.decode(rest.jwtToken);
			if (moment(jwt.exp * 1000).isBefore(moment())) {
				logout()
				return false;
			}
			return true;
		} 

		return false;
	}, [rest.jwtToken]);

	return (
		<Route
			{...rest}
			render={props => {
				if (validJwt) {
					return (
						<Component
							{...props}
						/>
					);
				}

				return (
					<Redirect
						to={{
							pathname: "/login",
							state: { from: props.location }
						}}
					/>
				);
			}}
		/>
	);
};

export default PrivateRoute;