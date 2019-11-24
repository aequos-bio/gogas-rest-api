import thunkMiddleware from 'redux-thunk';
import { createLogger } from 'redux-logger';
import { createStore, applyMiddleware } from 'redux';
import reducers from './reducers';
const loggerMiddleware = createLogger();

export const Store = createStore(
  reducers,
  {
    authentication: {
      running: false,
      jwtToken: undefined,
      userDetails: undefined
    },
    info: {
    }
  }, 
  applyMiddleware(thunkMiddleware, loggerMiddleware)
);


