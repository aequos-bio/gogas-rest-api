/* eslint-disable import/prefer-default-export */
import thunkMiddleware from 'redux-thunk';
import { createLogger } from 'redux-logger';
import { createStore, applyMiddleware } from 'redux';
import Cookies from 'cookies-js';
import { composeWithDevTools } from 'redux-devtools-extension';
import moment from 'moment-timezone';
import reducers from './reducers';

const loggerMiddleware = createLogger();

const jwt = Cookies.get('jwt-token');

export const Store = createStore(
  reducers,
  {
    authentication: {
      running: false,
      jwtToken: jwt,
      userDetails: undefined,
    },
    info: {},
    accounting: {
      currentYear: Number.parseInt(moment().format('YYYY'), 10),
    },
  },
  composeWithDevTools(applyMiddleware(thunkMiddleware, loggerMiddleware))
);
