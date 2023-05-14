import { createSlice } from '@reduxjs/toolkit';
import moment from 'moment-timezone';
import { AppDispatch } from '../store';
import { AccountingState } from '../types';

const initialState: AccountingState = {
  currentYear: Number.parseInt(moment().format('YYYY'), 10),
};

export const accountingSlice = createSlice({
  name: 'accounting',
  initialState,
  reducers: {
    setYear: (state, action) => {
      state.currentYear = action.payload;
    },
  },
});

export const setAccountingYear = (year: number) => (dispatch: AppDispatch) => {
  dispatch(setYear(year));
};

const { setYear } = accountingSlice.actions;
export default accountingSlice.reducer;
