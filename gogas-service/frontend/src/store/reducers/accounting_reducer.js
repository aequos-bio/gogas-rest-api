/* eslint-disable camelcase */
import { SET_YEAR } from '../actions';

const accounting_reducer = (state = [], action) => {
  switch (action.type) {
    case SET_YEAR:
      return { ...state, currentYear: action.payload };
    default:
      return state;
  }
};

export default accounting_reducer;
