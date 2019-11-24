import {INIT} from '../actions'

const info_reducer = (state = [], action) => {
  switch (action.type) {
    case INIT:
      return {...state, ...action.payload};
    default:
      return state;
  }
}

export default info_reducer;
