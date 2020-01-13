import { LOGIN_START, LOGIN_END, LOGIN_ERROR, LOGOUT_END, LOGOUT_ERROR } from '../actions'

const authentication_reducer = (state = [], action) => {
  switch (action.type) {
    case LOGIN_START:
      return { ...state, running: true, error_message: undefined };
    case LOGIN_END:
      return { ...state, running: false, jwtToken: action.payload }
    case LOGIN_ERROR:
      return { ...state, running: false, jwtToken: undefined, error_message: action.payload }

    case LOGOUT_END:
      return { ...state, running: false, jwtToken: undefined, error_message: undefined }
    case LOGOUT_ERROR:
      return { ...state, running: false, error_message: action.payload.errorMessage }
    default:
      return state;
  }
}

export default authentication_reducer;

