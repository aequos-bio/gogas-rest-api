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
import { AccountingCode } from './types';
import { useAccountingCodesAPI } from './useAccountingCodesAPI';

interface Props {
  open: boolean;
  onClose: (refresh: boolean) => void;
  orderTypeId?: string;
  code?: string;
}

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

const EditAccountingCodeDialog: React.FC<Props> = ({
  open,
  onClose,
  orderTypeId,
  code,
}) => {
  const classes = useStyles();
  const [accountingCode, setAccountingCode] = useState(code);
  const { saveAccountingCode } = useAccountingCodesAPI();

  useEffect(() => {
    if (open) setAccountingCode(code);
  }, [open, code]);

  const canSave = useMemo(() => {
    return !!orderTypeId && !!accountingCode;
  }, [orderTypeId, accountingCode]);

  const save = useCallback(() => {
    saveAccountingCode(orderTypeId as string, accountingCode as string).then(() => {
      onClose(true);
    })
  }, [orderTypeId, accountingCode, onClose]);

  return (
    <Dialog open={open} onClose={() => onClose(false)} maxWidth="xs" fullWidth>
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

export default EditAccountingCodeDialog;
