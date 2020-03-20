import React, { useMemo, useState, useCallback, useEffect } from 'react';
import {
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  FormControl,
  FormControlLabel,
  FormLabel,
  RadioGroup,
  Radio,
} from '@material-ui/core';
import { withSnackbar } from 'notistack';
import { makeStyles } from '@material-ui/core/styles';
import { apiGetJson, apiPost } from '../../../utils/axios_utils';

const useStyles = makeStyles(theme => ({
  field: {
    marginBottom: theme.spacing(2),
  },
  radiogrp: {
    flexDirection: 'row',
  },
  icon: {
    color: theme.palette.grey[500],
  },
}));

const EditReasonDialog = ({ mode, onClose, reasonCode, enqueueSnackbar }) => {
  const classes = useStyles();
  const [code, setCode] = useState('');
  const [description, setDescription] = useState('');
  const [sign, setSign] = useState('+');
  const [accountingCode, setAccountingCode] = useState('');

  useEffect(() => {
    if (reasonCode) {
      apiGetJson(`/api/accounting/reason/${reasonCode}`, {})
        .then(rr => {
          if (rr.error) {
            enqueueSnackbar(rr.errorMessage, { variant: 'error' });
          } else {
            setCode(rr.reasonCode);
            setDescription(rr.description);
            setSign(rr.sign);
            setAccountingCode(rr.accountingCode || '');
          }
        })
        .catch(err => {
          enqueueSnackbar(
            err.response?.statusText || 'Errore nel caricamento delle causali',
            { variant: 'error' }
          );
        });
    } else {
      setCode('');
      setDescription('');
      setSign('+');
      setAccountingCode('');
    }
  }, [reasonCode, enqueueSnackbar]);

  const save = useCallback(() => {
    apiPost('/api/accounting/reason', {
      reasonCode: code,
      description,
      sign,
      accountingCode,
    })
      .then(() => {
        enqueueSnackbar(
          `Causale ${mode === 'new' ? 'salvata' : 'modificata'}`,
          { variant: 'success' }
        );
        onClose();
      })
      .catch(err => {
        enqueueSnackbar(
          err.response?.statusText || 'Errore nel salvataggio della causale',
          { variant: 'error' }
        );
      });
  }, [mode, code, description, sign, accountingCode, enqueueSnackbar, onClose]);

  const canSave = useMemo(() => {
    let ok = true;
    if (!code || code.includes(' ')) {
      ok = false;
    }
    if (!description) {
      ok = false;
    }
    if (accountingCode.includes(' ')) {
      ok = false;
    }
    return ok;
  }, [code, description, accountingCode]);

  return (
    <Dialog open={mode !== false} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{mode === 'new' ? 'Nuova' : 'Modifica'} causale</DialogTitle>

      <DialogContent className={classes.content}>
        <TextField
          className={classes.field}
          label="Codice"
          value={code}
          variant="outlined"
          size="small"
          InputLabelProps={{
            shrink: true,
          }}
          InputProps={{
            readOnly: mode === 'edit',
          }}
          onChange={evt => {
            setCode(evt.target.value.toUpperCase());
          }}
          fullWidth
        />

        <TextField
          className={classes.field}
          label="Descrizione"
          value={description}
          variant="outlined"
          size="small"
          InputLabelProps={{
            shrink: true,
          }}
          onChange={evt => {
            setDescription(evt.target.value);
          }}
          fullWidth
        />

        <FormControl component="fieldset" className={classes.field}>
          <FormLabel component="legend">Segno</FormLabel>
          <RadioGroup
            name="sign"
            value={sign}
            onChange={evt => setSign(evt.target.value)}
            className={classes.radiogrp}
          >
            <FormControlLabel
              value="+"
              control={<Radio size="small" disabled={mode === 'edit'} />}
              label="Accredito (+)"
            />
            <FormControlLabel
              value="-"
              control={<Radio size="small" disabled={mode === 'edit'} />}
              label="Addebito (-)"
            />
          </RadioGroup>
        </FormControl>

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
        <Button onClick={onClose} autoFocus>
          Annulla
        </Button>
        <Button onClick={() => save(false)} disabled={!canSave}>
          Salva
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default withSnackbar(EditReasonDialog);
