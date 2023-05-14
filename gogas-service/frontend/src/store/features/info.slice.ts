import { createSlice } from '@reduxjs/toolkit';
import { apiGetJson } from '../../utils/axios_utils';
import { AppDispatch } from '../store';
import { InfoState } from '../types';

const initialState: InfoState = {
  'gas.nome': '',
  'colli.soglia_arrotondamento': '',
  'aequos.password': '',
  'aequos.username': '',
  'visualizzazione.utenti': 'NC'
};

export const infoSlice = createSlice({
  name: 'info',
  initialState,
  reducers: {
    setInfo: (state, action) => {
      state = action.payload;
      if (action.payload && action.payload['gas.nome'])
        document.title = action.payload['gas.nome'] || 'GoGas';
    },
  },
});

export const init = () => (dispatch: AppDispatch) => {
  apiGetJson('/info')
    .then((infoJson) => {
      dispatch(setInfo(infoJson));
    })
    .catch((err) => {
      console.error('errore', err);
      dispatch(setInfo(undefined));
    });
};

const { setInfo } = infoSlice.actions;
export default infoSlice.reducer;
