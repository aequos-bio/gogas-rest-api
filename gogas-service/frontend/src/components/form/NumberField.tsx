import React, { useCallback } from 'react';
import { TextField, InputAdornment } from '@material-ui/core';

interface Props {
  label?: string;
  value: number | undefined;
  onChange: (value: number | undefined) => void;
  min?: number;
  max?: number;
  /** Number of decimal digits allowed. 0 => integers only. */
  decimals?: number;
  disabled?: boolean;
  error?: boolean;
  helperText?: string;
  fullWidth?: boolean;
  /** Optional trailing adornment, e.g. '€'. */
  endAdornment?: React.ReactNode;
}

/**
 * Shared numeric input replacing the legacy jqxNumberInput. Keeps the value as
 * a number (or undefined when empty), clamps to min/max, and limits decimals.
 * Accepts both comma and dot as decimal separators (Italian keyboards).
 */
const NumberField: React.FC<Props> = ({
  label,
  value,
  onChange,
  min,
  max,
  decimals = 2,
  disabled = false,
  error = false,
  helperText,
  fullWidth = true,
  endAdornment,
}) => {
  const handleChange = useCallback(
    (event: React.ChangeEvent<HTMLInputElement>) => {
      const raw = event.target.value.replace(',', '.').trim();

      if (raw === '') {
        onChange(undefined);
        return;
      }

      const parsed = Number(raw);
      if (isNaN(parsed)) {
        return; // ignore non-numeric input
      }

      let next = parsed;
      if (decimals <= 0) {
        next = Math.trunc(next);
      } else {
        const factor = Math.pow(10, decimals);
        next = Math.round(next * factor) / factor;
      }
      if (min !== undefined && next < min) next = min;
      if (max !== undefined && next > max) next = max;

      onChange(next);
    },
    [onChange, min, max, decimals],
  );

  return (
    <TextField
      label={label}
      type="number"
      value={value === undefined ? '' : value}
      onChange={handleChange}
      disabled={disabled}
      error={error}
      helperText={helperText}
      fullWidth={fullWidth}
      size="small"
      inputProps={{ min, max, step: decimals > 0 ? 1 / Math.pow(10, decimals) : 1 }}
      InputProps={
        endAdornment
          ? { endAdornment: <InputAdornment position="end">{endAdornment}</InputAdornment> }
          : undefined
      }
    />
  );
};

export default NumberField;
