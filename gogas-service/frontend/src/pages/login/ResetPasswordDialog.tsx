import React, { useCallback, useMemo, useState, useEffect } from 'react';
import {
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField
} from '@material-ui/core';
import { EuroSharp as EuroIcon } from '@material-ui/icons';
import { makeStyles } from '@material-ui/core/styles';
import { useResetPasswordAPI, isValidEmail } from './useResetPasswordAPI'

const useStyles = makeStyles(theme => ({
  field: {
    marginBottom: theme.spacing(2),
  },
  errorContainer: {
    color: 'white',
    backgroundColor: 'red',
    margin: '0px',
    padding: '8px',
    textAlign: 'center',
    fontSize: 'small',
  },
  hidden: {
    display: 'none'
  }
}));

interface Props {
  open: boolean;
  handleClose: () => void;
}

const ResetPasswordDialog: React.FC<Props> = ({
  open, handleClose
}) => {
  const classes = useStyles();
  const [username, setUsername] = useState<string | undefined>(undefined);
  const [email, setEmail] = useState<string | undefined>(undefined);
  const [validEmail, setValidEmail] = useState<boolean>(true);
  const [errorMessage, setErrorMessage] = useState<string | undefined>(undefined);
  const { loading, resetPassword } = useResetPasswordAPI();

  const resetStatus = useCallback(() => {
    setErrorMessage(undefined);
    setValidEmail(true);
  }, []);

  const resetForm = useCallback(() => {
    resetStatus();
    setUsername(undefined);
    setEmail(undefined);
  }, [resetStatus]);

  const canSend = useMemo(() => {
    return username && email && validEmail
  }, [username, email]);

  const close = useCallback(
    () => {
      resetForm();
      handleClose();
    },
    [handleClose, resetStatus]
  );

  const send = useCallback(
    async () => {
      if (!username || !email) {
        return;
      }

      resetStatus();

      let result = await resetPassword(username, email);
      if (result.error) {
        setErrorMessage(result.errorMessage);
      } else {
        close();
      }
    },
    [username, email, close, resetStatus]
  );

  return (
    <Dialog open={open} maxWidth="xs" fullWidth>
      <DialogTitle>Reset password</DialogTitle>

      <DialogContent>
        <TextField
          className={classes.field}
          label="Username"
          type="text"
          variant="outlined"
          size="small"
          InputLabelProps={{
            shrink: true,
          }}
          onChange={evt => setUsername(evt.target.value)}
          fullWidth
        />

        <TextField
          className={classes.field}
          label="E-mail"
          type="text"
          variant="outlined"
          size="small"
          InputLabelProps={{
            shrink: true,
          }}
          onChange={evt => {
            if (!evt.target.value) {
              setValidEmail(true);
              return;
            }
            setValidEmail(isValidEmail(evt.target.value));
            setEmail(evt.target.value)
          }}
          error={!validEmail}
          helperText={!validEmail ? 'E-mail non valida' : null}
          fullWidth
        />
      </DialogContent>

      <DialogActions>
        <Button onClick={() => close()} autoFocus>
          Annulla
        </Button>
        <Button onClick={() => send()} disabled={!canSend}>
          Conferma
        </Button>
      </DialogActions>

      <p className={errorMessage ? classes.errorContainer : classes.hidden}>{errorMessage}</p>
    </Dialog>
  );
};

export default ResetPasswordDialog;
