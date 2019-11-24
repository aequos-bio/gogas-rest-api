import React from "react";
import { Route } from "react-router-dom";
import { Redirect } from "react-router-dom";

const PrivateRoute = ({ component: Component, ...rest }) => {
	return (
		<Route
			{...rest}
			render={props => {
				if (rest.jwtToken) {
					return (
						<Component
							{...props}
							onLogoutDone={rest.onLogoutDone}
							info={rest.info}
						/>
					);
				} else {
					console.warn("redirecting from %s to login", rest.path, rest.jwtToken)
					return (
						<Redirect
							to={{
								pathname: "/login",
								state: { from: props.location }
							}}
						/>
					);
				}
			}}
		/>
	);
};

export default PrivateRoute;