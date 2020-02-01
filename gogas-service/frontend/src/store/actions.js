import Cookies from "cookies-js";
import { getJson, apiPost } from "../utils/axios_utils";

export const INIT = "INIT";
export const SET_AUTH = "SET_AUTH";
export const LOGIN_START = "LOGIN_START";
export const LOGIN_END = "LOGIN_END";
export const LOGIN_ERROR = "LOGIN_ERROR";
export const LOGOUT_END = "LOGOUT_END";
export const LOGOUT_ERROR = "LOGOUT_ERROR";

// ////////////
// INFO
// ////////////
function setInfo(info) {
  return {
    type: INIT,
    payload: info
  };
}

export const init = () => {
  return dispatch => {
    return getJson("/info")
      .then(infoJson => {
        dispatch(setInfo(infoJson));
      })
      .catch(err => {
        console.error("errore", err);
        dispatch(setInfo());
      });
  };
};

// ////////////
// LOGIN
// ////////////
function loginStart() {
  return {
    type: LOGIN_START
  };
}

function loginEnd(response) {
  return {
    type: LOGIN_END,
    payload: response
  };
}

function loginError(message) {
  return {
    type: LOGIN_ERROR,
    payload: message
  };
}

export const login = (username, password) => {
  return async dispatch => {
    dispatch(loginStart());

    try {
      const auth = await apiPost("/authenticate", { username, password });
      if (auth.redirected && auth.url.endsWith("error")) {
        dispatch(loginError("Errore di autenticazione"));
      } else {
        dispatch(loginEnd(auth.data.data));
      }
    } catch (err) {
      dispatch(loginError(err.response ? err.response.data.message : err.name));
    }
  };
};

// ////////////
// LOGOUT
// ////////////

export const logout = () => {
  return dispatch => {
    Cookies.expire("jwt-token");
    dispatch({
      type: LOGOUT_END
    });
  };
};
