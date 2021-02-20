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

const defaultState = {
  authentication: {
    running: false,
    jwtToken: jwt,
    userDetails: undefined,
  },
  info: {},
  accounting: {
    currentYear: Number.parseInt(moment().format('YYYY'), 10),
  },
};

const loadState = () => {
  let state = defaultState;
  try {
    const serializedState = localStorage.getItem('state');
    if (serializedState !== null) {
      state = JSON.parse(serializedState);
    }
  } catch (err) {
    console.error('Error while trying to idrate the state', err);
  }
  return state;
};

const saveState = state => {
  try {
    const serializedState = JSON.stringify(state);
    localStorage.setItem('state', serializedState);
  } catch (err) {
    console.error(err);
    // ignore write errors
  }
};

export const Store = createStore(
  reducers,
  loadState(),
  composeWithDevTools(applyMiddleware(thunkMiddleware, loggerMiddleware))
);

Store.subscribe(() => {
  saveState(Store.getState());
});
