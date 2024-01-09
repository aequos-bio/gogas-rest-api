import React, { useState } from 'react';
import { Avatar, Card, CardContent, CardHeader, Grid, Typography } from "@material-ui/core";
import { Visibility as VisibilityIcon, VisibilityOff as VisibilityOffIcon } from '@material-ui/icons';
import { makeStyles } from '@material-ui/core/styles';
import { TextField, InputAdornment, IconButton } from '@material-ui/core';

interface Props {
  label: string;
  onChange: (evt: any) => void;
  error: boolean;
  errorMessage: string;
}

const useStyles = makeStyles(theme => ({
  field: {
    marginBottom: theme.spacing(2),
  }
}));

export const PasswordField: React.FC<Props> = ({ label, onChange, error, errorMessage }) => {
  const classes = useStyles();
  const [showPassword, setShowPassword] = useState<boolean>(false);

  const handleClickShowPassword = () => {
    setShowPassword(!showPassword);
  };

  const handleMouseDownPassword = (event: any) => {
    event.preventDefault();
  };

  return (
    <TextField
      className={classes.field}
      label={label}
      type={showPassword ? "text" : "password"}
      variant="outlined"
      size="small"
      InputLabelProps={{
        shrink: true,
      }}
      onChange={onChange}
      error={error}
      helperText={error ? errorMessage : null}
      fullWidth
      InputProps={{
        endAdornment:
          <InputAdornment position="end">
            <IconButton
              onClick={handleClickShowPassword}
              onMouseDown={handleMouseDownPassword}
            >
              {showPassword ? (
                <VisibilityIcon />
              ) : (
                <VisibilityOffIcon />
              )}
            </IconButton>
          </InputAdornment>
      }}
    />
  )
}