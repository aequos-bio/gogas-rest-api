import { configureStore } from '@reduxjs/toolkit';
import { TypedUseSelectorHook, useDispatch, useSelector } from 'react-redux'
import Cookies from 'cookies-js';
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

export type RootState = ReturnType<typeof store.getState>
export type AppDispatch = typeof store.dispatch;
export const useAppDispatch: () => AppDispatch = useDispatch; // Export a hook that can be reused to resolve types
export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector;
export default store;