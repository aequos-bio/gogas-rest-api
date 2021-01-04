import { combineReducers } from 'redux';
import authenticationReducer from './authentication_reducer';
import infoReducer from './info_reducer';
import accountingReducer from './accounting_reducer';

export default combineReducers({
  authentication: authenticationReducer,
  info: infoReducer,
  accounting: accountingReducer,
});
