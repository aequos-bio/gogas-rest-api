import { combineReducers } from 'redux';
import authentication_reducer from './authentication_reducer';
import info_reducer from './info_reducer';

export default combineReducers({
  authentication: authentication_reducer,
  info: info_reducer
});
