import React, { useMemo, useState, useCallback, useEffect } from 'react';
import {
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
} from '@material-ui/core';
import { withSnackbar } from 'notistack';
import { makeStyles } from '@material-ui/core/styles';
import { apiPut } from '../../../utils/axios_utils';

const useStyles = makeStyles(theme => ({
  content: {
    display: 'flex',
    flexDirection: 'column',
  },
  field: {
    marginBottom: theme.spacing(1),
    marginTop: theme.spacing(1),
  },
  radiogrp: {
    flexDirection: 'row',
  },
  icon: {
    color: theme.palette.grey[500],
  },
}));

const EditAccountingCodeDialog = ({
  open,
  onClose,
  orderTypeId,
  code,
  enqueueSnackbar,
}) => {
  const classes = useStyles();
  const [accountingCode, setAccountingCode] = useState(code);

  useEffect(() => {
    if (open) setAccountingCode(code);
  }, [open, code]);

  const canSave = useMemo(() => {
    return accountingCode !== undefined && accountingCode !== '';
  }, [accountingCode]);

  const save = useCallback(() => {
    const params = {
      accountingCode,
    };

    const thenFn = () => {
      enqueueSnackbar(`Codice contabile modificato'}`, { variant: 'success' });
      onClose(true);
    };

    const catchFn = err => {
      enqueueSnackbar(
        err.response?.statusText || 'Errore nel salvataggio della causale',
        { variant: 'error' }
      );
    };

    apiPut(`/api/ordertype/${orderTypeId}/accounting`, params)
      .then(thenFn)
      .catch(catchFn);
  }, [orderTypeId, accountingCode, enqueueSnackbar, onClose]);

  return (
    <Dialog open={open} onClose={() => onClose()} maxWidth="xs" fullWidth>
      <DialogTitle>Modifica codice contabile</DialogTitle>

      <DialogContent className={classes.content}>
        <TextField
          className={classes.field}
          label="Codice contabile"
          value={accountingCode}
          variant="outlined"
          size="small"
          InputLabelProps={{
            shrink: true,
          }}
          onChange={evt => {
            setAccountingCode(evt.target.value);
          }}
          fullWidth
        />
      </DialogContent>

      <DialogActions>
        <Button onClick={() => onClose()} autoFocus>
          Annulla
        </Button>
        <Button onClick={() => save(false)} disabled={!canSave}>
          Salva
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default withSnackbar(EditAccountingCodeDialog);
