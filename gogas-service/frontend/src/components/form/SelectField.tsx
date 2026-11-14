import React, { useMemo } from 'react';
import { InputLabel, FormControl, FormHelperText } from '@material-ui/core';
import { makeStyles } from '@material-ui/core/styles';
import Select from 'react-select';

export interface SelectOption<V = string> {
  value: V;
  label: React.ReactNode;
}

interface Props<V = string> {
  label?: string;
  options: SelectOption<V>[];
  value?: V;
  onChange: (value: V | undefined) => void;
  placeholder?: string;
  isClearable?: boolean;
  isDisabled?: boolean;
  error?: boolean;
  helperText?: string;
}

const useStyles = makeStyles((theme) => ({
  control: {
    marginTop: theme.spacing(2),
  },
  label: {
    marginBottom: theme.spacing(0.5),
  },
}));

/**
 * Shared single-select field over react-select, replacing the legacy select2 /
 * jqxDropDownList dropdowns. Works with simple `{ value, label }` options and
 * reports the selected raw value (or undefined when cleared).
 */
function SelectField<V = string>({
  label,
  options,
  value,
  onChange,
  placeholder = 'Seleziona...',
  isClearable = false,
  isDisabled = false,
  error = false,
  helperText,
}: Props<V>) {
  const classes = useStyles();

  const selected = useMemo(() => {
    if (value === undefined || value === null) return null;
    // Match case-insensitively for strings: backend record ids and select
    // option ids can differ in case (e.g. SQL Server UUIDs), which would
    // otherwise leave the dropdown empty when editing an existing record.
    const matches = (optionValue: V) => {
      if (typeof optionValue === 'string' && typeof value === 'string') {
        return optionValue.toLowerCase() === value.toLowerCase();
      }
      return optionValue === value;
    };
    return options.find((o) => matches(o.value)) ?? null;
  }, [options, value]);

  return (
    <FormControl fullWidth error={error} className={label ? classes.control : undefined}>
      {label ? (
        <InputLabel shrink className={classes.label}>
          {label}
        </InputLabel>
      ) : null}
      <Select
        options={options as any}
        value={selected as any}
        placeholder={placeholder}
        isClearable={isClearable}
        isDisabled={isDisabled}
        menuPortalTarget={typeof document !== 'undefined' ? document.body : undefined}
        styles={{ menuPortal: (base: any) => ({ ...base, zIndex: 1400 }) }}
        onChange={(option: any) => onChange(option ? option.value : undefined)}
      />
      {helperText ? <FormHelperText>{helperText}</FormHelperText> : null}
    </FormControl>
  );
}

export default SelectField;
