/* eslint-disable camelcase */
import { INIT } from '../actions';

const info_reducer = (state = [], action) => {
  switch (action.type) {
    case INIT:
      if (action.payload && action.payload['gas.nome'])
        document.title = action.payload['gas.nome'] || 'GoGas';
      return { ...state, ...action.payload };
    default:
      return state;
  }
};

export default info_reducer;
