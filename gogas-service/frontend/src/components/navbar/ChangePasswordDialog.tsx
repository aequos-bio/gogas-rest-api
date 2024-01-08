import React, { useCallback, useMemo, useState, useEffect } from 'react';
import {
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  InputAdornment,
} from '@material-ui/core';
import { EuroSharp as EuroIcon } from '@material-ui/icons';
import { makeStyles } from '@material-ui/core/styles';
import { useChangePasswordAPI, ChangePasswordStatus } from './useChangePasswordAPI'
import { PasswordField } from './PasswordField'

const useStyles = makeStyles(theme => ({
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

const ChangePasswordDialog: React.FC<Props> = ({
  open, handleClose
}) => {
  const classes = useStyles();
  const [oldPassword, setOld] = useState<string | undefined>(undefined);
  const [newPassword, setNew] = useState<string | undefined>(undefined);
  const [confirmPassword, setConfirm] = useState<string | undefined>(undefined);
  const [status, setStatus] = useState<ChangePasswordStatus | undefined>(undefined);
  const [errorMessage, setErrorMessage] = useState<string | undefined>(undefined);
  const { loading, changePassword } = useChangePasswordAPI();

  const resetStatus = useCallback(() => {
    setStatus(undefined);
    setErrorMessage(undefined);
  }, []);

  const resetForm = useCallback(() => {
    resetStatus();
    setOld(undefined);
    setNew(undefined);
    setConfirm(undefined);
  }, [resetStatus]);

  const canSave = useMemo(() => {
    if (!oldPassword) return false;
    if (!newPassword) return false;
    if (!confirmPassword) return false;
    return true;
  }, [oldPassword, newPassword, confirmPassword]);

  const close = useCallback(
    () => {
      resetForm();
      handleClose();
    },
    [handleClose, resetStatus]
  );

  const save = useCallback(
    async () => {
      if (!oldPassword || !newPassword || !confirmPassword) {
        return;
      }

      resetStatus();

      let result = await changePassword(oldPassword, newPassword, confirmPassword);
      setStatus(result.status);
      setErrorMessage(result.errorMessage);

      if (result.status == ChangePasswordStatus.OK) {
        close();
      }
    },
    [oldPassword, newPassword, confirmPassword, close, resetStatus]
  );

  return (
    <Dialog open={open} maxWidth="xs" fullWidth>
      <DialogTitle>Cambio password</DialogTitle>

      <DialogContent>
        <PasswordField
          label="Password attuale"
          onChange={evt => setOld(evt.target.value)}
          error={status == ChangePasswordStatus.WRONG_PASSWORD}
          errorMessage="Password non corretta, riprovare"
        />

        <PasswordField
          label="Nuova password"
          onChange={evt => setNew(evt.target.value)}
          error={status == ChangePasswordStatus.PASSWORD_LENGTH}
          errorMessage="La password deve essere di almeno 8 caratteri"
        />

        <PasswordField
          label="Conferma password"
          onChange={evt => setConfirm(evt.target.value)}
          error={status == ChangePasswordStatus.PASSWORD_MISMATCH}
          errorMessage="Password di conferma non corrispondente, riprovare"
        />
      </DialogContent>

      <DialogActions>
        <Button onClick={() => close()} autoFocus>
          Annulla
        </Button>
        <Button onClick={() => save()} disabled={!canSave}>
          Conferma
        </Button>
      </DialogActions>

      <p className={errorMessage ? classes.errorContainer : classes.hidden}>{errorMessage}</p>
    </Dialog>
  );
};

export default ChangePasswordDialog;
