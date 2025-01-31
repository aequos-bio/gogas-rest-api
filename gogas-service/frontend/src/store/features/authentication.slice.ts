import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import Cookies from 'cookies-js';
import { apiPost, extractResponseFromError } from '../../utils/axios_utils';
import { AppDispatch, RootState } from '../store';
import { AuthenticationState } from '../types';
const jwt = Cookies.get('jwt-token');

const initialState: AuthenticationState = {
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

export const login = createAsyncThunk<any, { username: string, password: any }, { rejectValue: string, state: RootState }>(
  'authentication',
  async ({ username, password }, thunkApi) => {
    try {
      const auth = await apiPost('/authenticate', { username, password });
      return auth.data.data;
    } catch (error) {
      var response = extractResponseFromError(error);

      if (!response) {
        return thunkApi.rejectWithValue('Error generico: ' + error);
      }

      return thunkApi.rejectWithValue(response.data.message)
    }
  },
);

export const logout = () => (dispatch: AppDispatch) => {
  Cookies.expire('jwt-token');
  dispatch(setLoggedOut());
};

const { setLoggedOut } = authenticationSlice.actions;
export default authenticationSlice.reducer;
