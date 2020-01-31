/* eslint-disable react/jsx-props-no-spreading */
import React from "react";
import { Route, Redirect } from "react-router-dom";

const PrivateRoute = ({ component: Component, ...rest }) => {
	return (
		<Route
			{...rest}
			render={props => {
				if (rest.jwtToken) {
					return (
						<Component
							{...props}
						/>
					);
				}

				console.warn("redirecting from %s to login", rest.path, rest.jwtToken)
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