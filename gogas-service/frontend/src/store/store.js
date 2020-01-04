import thunkMiddleware from 'redux-thunk';
import { createLogger } from 'redux-logger';
import { createStore, applyMiddleware } from 'redux';
import Cookies from 'cookies-js';
import reducers from './reducers';
const loggerMiddleware = createLogger();

const jwt = Cookies.get('jwt-token');

export const Store = createStore(
  reducers,
  {
    authentication: {
      running: false,
      jwtToken: jwt,
      userDetails: undefined
    },
    info: {
    }
  }, 
  applyMiddleware(thunkMiddleware, loggerMiddleware)
);


