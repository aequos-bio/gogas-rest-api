import { createSlice } from '@reduxjs/toolkit';
import moment from 'moment-timezone';

const initialState = {
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

export const setAccountingYear = (year) => (dispatch) => {
  dispatch(setYear(year));
};

const { setYear } = accountingSlice.actions;
export default accountingSlice.reducer;
