import React from 'react';
import { Route, Redirect } from 'react-router-dom';
import useJwt from './JwtHooks';

const PrivateRoute = ({ component: Component, ...rest }) => {
  const validJwt = useJwt();

  return (
    <Route
      {...rest}
      render={(props) => {
        if (validJwt) {
          return <Component {...props} />;
        }

        return (
          <Redirect
            to={{
              pathname: '/login',
              state: { from: props.location },
            }}
          />
        );
      }}
    />
  );
};

export default PrivateRoute;
