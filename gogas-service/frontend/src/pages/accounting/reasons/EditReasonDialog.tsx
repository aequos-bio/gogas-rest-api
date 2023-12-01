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
import { makeStyles } from '@material-ui/core/styles';
import { useReasonsAPI } from './useReasonsAPI';

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

interface Props {
  mode: false | 'edit' | 'new';
  onClose: (refresh: boolean) => void;
  reasonCode?: string;
}

const EditReasonDialog: React.FC<Props> = ({ mode, onClose, reasonCode }) => {
  const classes = useStyles();
  const [code, setCode] = useState('');
  const [description, setDescription] = useState('');
  const [sign, setSign] = useState<'+' | '-'>('+');
  const [accountingCode, setAccountingCode] = useState('');
  const { getReason, saveReason } = useReasonsAPI();

  useEffect(() => {
    if (reasonCode) {
      getReason(reasonCode).then((reason) => {
        setCode(reason?.reasonCode || '');
        setDescription(reason?.description || '');
        setSign(reason?.sign || '+');
        setAccountingCode(reason?.accountingCode || '');
      })
    } else {
      setCode('');
      setDescription('');
      setSign('+');
      setAccountingCode('');
    }
  }, [reasonCode]);

  const save = useCallback(() => {
    saveReason({
      reasonCode: code,
      description,
      sign,
      accountingCode,
    }, mode).then(() => {
      onClose(true);
    });
  }, [mode, code, description, sign, accountingCode, onClose]);

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
    <Dialog
      open={mode !== false}
      onClose={() => onClose(false)}
      maxWidth="xs"
      fullWidth
    >
      <DialogTitle>{mode === 'new' ? 'Nuova' : 'Modifica'} causale</DialogTitle>

      <DialogContent>
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
            onChange={evt => setSign(evt.target.value as ('+' | '-'))}
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
        <Button onClick={() => onClose(false)} autoFocus>
          Annulla
        </Button>
        <Button onClick={() => save()} disabled={!canSave}>
          Salva
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default EditReasonDialog;
