import { configureStore } from '@reduxjs/toolkit';
import Cookies from 'cookies-js';
import moment from 'moment-timezone';
import accountingReducer from './features/accounting.slice';
import authenticationReducer from './features/authentication.slice';
import infoReducer from './features/info.slice';
import { loadState, saveState } from './hydration';

const jwt = Cookies.get('jwt-token');

export const store = configureStore({
  reducer: {
    authentication: authenticationReducer,
    info: infoReducer,
    accounting: accountingReducer,
  },
  preloadedState: loadState(),
});

store.subscribe(() => {
  saveState(store.getState());
});
