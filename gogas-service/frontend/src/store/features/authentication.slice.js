import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import Cookies from 'cookies-js';
import { apiPost } from '../../utils/axios_utils';
const jwt = Cookies.get('jwt-token');

const initialState = {
  running: false,
  error_message: undefined,
  jwtToken: jwt,
  userDetails: undefined,
};

export const authenticationSlice = createSlice({
  name: 'authentication',
  initialState,
  reducers: {
    setLoggedOut: (state) => {
      state.running = false;
      state.jwtToken = undefined;
    },
  },
  extraReducers: (builder) => {
    builder.addCase(login.pending, (state) => {
      state.running = true;
      state.error_message = undefined;
    });
    builder.addCase(login.fulfilled, (state, action) => {
      state.running = false;
      state.error_message = undefined;
      state.jwtToken = action.payload;
    });
    builder.addCase(login.rejected, (state, action) => {
      state.running = false;
      state.error_message = action.payload;
      state.jwtToken = undefined;
    });
  },
});

export const login = createAsyncThunk(
  'authentication',
  async ({ username, password }, thunkApi) => {
    const auth = await apiPost('/authenticate', { username, password });
    return auth.data.data;
  },
);

export const logout = () => (dispatch) => {
  Cookies.expire('jwt-token');
  dispatch(setLoggedOut());
};

const { setLoggedOut } = authenticationSlice.actions;
export default authenticationSlice.reducer;
