import React, { useMemo } from 'react';
import { InputLabel, FormControl } from '@material-ui/core';
import { makeStyles } from '@material-ui/core/styles';
import Select from 'react-select';
import { SelectOption } from './SelectField';

interface Props<V = string> {
  label?: string;
  options: SelectOption<V>[];
  value: V[];
  onChange: (values: V[]) => void;
  placeholder?: string;
  isDisabled?: boolean;
}

const useStyles = makeStyles((theme) => ({
  control: { marginTop: theme.spacing(2) },
  label: { marginBottom: theme.spacing(0.5) },
}));

/**
 * Multi-select field over react-select, used for the status filter on the
 * orders list (replacing the legacy select2 multiple). Reports the selected
 * raw values as an array.
 */
function MultiSelectField<V = string>({
  label,
  options,
  value,
  onChange,
  placeholder = 'Seleziona...',
  isDisabled = false,
}: Props<V>) {
  const classes = useStyles();

  const selected = useMemo(() => {
    const has = (optionValue: V) =>
      value.some((v) =>
        typeof optionValue === 'string' && typeof v === 'string'
          ? optionValue.toLowerCase() === v.toLowerCase()
          : optionValue === v,
      );
    return options.filter((o) => has(o.value));
  }, [options, value]);

  return (
    <FormControl fullWidth className={label ? classes.control : undefined}>
      {label ? (
        <InputLabel shrink className={classes.label}>
          {label}
        </InputLabel>
      ) : null}
      <Select
        isMulti
        options={options as any}
        value={selected as any}
        placeholder={placeholder}
        isDisabled={isDisabled}
        menuPortalTarget={typeof document !== 'undefined' ? document.body : undefined}
        styles={{ menuPortal: (base: any) => ({ ...base, zIndex: 1400 }) }}
        onChange={(opts: any) => onChange(opts ? opts.map((o: any) => o.value) : [])}
      />
    </FormControl>
  );
}

export default MultiSelectField;
