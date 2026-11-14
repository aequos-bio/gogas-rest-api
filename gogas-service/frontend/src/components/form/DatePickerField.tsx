import React from 'react';
import { MuiPickersUtilsProvider, KeyboardDatePicker } from '@material-ui/pickers';
import MomentUtils from '@date-io/moment';
import { MaterialUiPickersDate } from '@material-ui/pickers/typings/date';
import moment from 'moment-timezone';

interface Props {
  label?: string;
  /** ISO date string (YYYY-MM-DD) or undefined. */
  value?: string;
  onChange: (value: string | undefined) => void;
  disabled?: boolean;
  clearable?: boolean;
  error?: boolean;
  helperText?: string;
  fullWidth?: boolean;
}

/**
 * Shared date field over @material-ui/pickers, replacing the legacy
 * jqxDateTimeInput / bootstrap-datepicker inputs. Displays dates in the
 * Italian DD/MM/YYYY format and emits an ISO (YYYY-MM-DD) string.
 */
const DatePickerField: React.FC<Props> = ({
  label,
  value,
  onChange,
  disabled = false,
  clearable = true,
  error = false,
  helperText,
  fullWidth = true,
}) => {
  const handleChange = (date: MaterialUiPickersDate) => {
    if (!date || !date.isValid()) {
      onChange(undefined);
      return;
    }
    onChange(date.format('YYYY-MM-DD'));
  };

  return (
    <MuiPickersUtilsProvider libInstance={moment} utils={MomentUtils}>
      <KeyboardDatePicker
        label={label}
        value={value ? moment(value, 'YYYY-MM-DD') : null}
        onChange={handleChange}
        format="DD/MM/YYYY"
        disabled={disabled}
        clearable={clearable}
        error={error}
        helperText={helperText}
        fullWidth={fullWidth}
        size="small"
        autoOk
        cancelLabel="Annulla"
        clearLabel="Pulisci"
        okLabel="Ok"
        invalidDateMessage="Data non valida"
      />
    </MuiPickersUtilsProvider>
  );
};

export default DatePickerField;
