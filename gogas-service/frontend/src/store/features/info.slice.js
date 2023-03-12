import { createSlice } from '@reduxjs/toolkit';
import { apiGetJson } from '../../utils/axios_utils';

const initialState = {};

export const infoSlice = createSlice({
  name: 'info',
  initialState,
  reducers: {
    setInfo: (state, action) => {
      if (action.payload && action.payload['gas.nome'])
        document.title = action.payload['gas.nome'] || 'GoGas';
    },
  },
});

export const init = () => (dispatch) => {
  apiGetJson('/info')
    .then((infoJson) => {
      dispatch(setInfo(infoJson));
    })
    .catch((err) => {
      console.error('errore', err);
      dispatch(setInfo());
    });
};

const { setInfo } = infoSlice.actions;
export default infoSlice.reducer;
