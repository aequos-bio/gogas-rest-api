import React, { useCallback, useMemo, useState, useEffect } from 'react';
import {
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  InputAdornment,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@material-ui/core';
import { EuroSharp as EuroIcon } from '@material-ui/icons';
import { MuiPickersUtilsProvider, DatePicker } from '@material-ui/pickers';
import MomentUtils from '@date-io/moment';
import moment from 'moment-timezone';
import { withSnackbar } from 'notistack';
import { makeStyles } from '@material-ui/core/styles';
import { apiGetJson, apiPost, apiPut } from '../../../utils/axios_utils';

moment.locale('it');

const useStyles = makeStyles(theme => ({
  field: {
    marginBottom: theme.spacing(2),
  },
  selectLabel: {
    backgroundColor: 'white',
    padding: '0px 5px',
  },
  icon: {
    color: theme.palette.grey[500],
  },
}));

const EditGasMovementDialog = ({
  open,
  onClose,
  movement,
  enqueueSnackbar,
}) => {
  const classes = useStyles();
  const [id, setId] = useState();
  const [date, setDate] = useState();
  const [reason, setReason] = useState();
  const [description, setDescription] = useState('');
  const [amount, setAmount] = useState(0);
  const [reasons, setReasons] = useState([]);
  const [refreshNeeded, setRefreshNeeded] = useState(false);

  useEffect(() => {
    apiGetJson('/api/accounting/reason/list', {}).then(rr => {
      if (rr.error) {
        enqueueSnackbar(rr.errorMessage, { variant: 'error' });
      } else {
        setReasons(rr);
      }
    });
  }, [enqueueSnackbar]);

  useEffect(() => {
    if (!open) return;

    if (movement) {
      setId(movement.id);
      setReason(movement.codicecausale);
      setDate(moment(movement.data, 'DD/MM/YYYY').format('YYYY-MM-DD'));
      setDescription(movement.descrizione);
      setAmount(movement.importo);
    } else {
      setId();
      setReason(null);
      setDate();
      setDescription('');
      setAmount(0);
    }
  }, [movement, open]);

  const canSave = useMemo(() => {
    if (!date) return false;
    if (!reason) return false;
    if (!description.length) return false;
    if (!amount) return false;
    return true;
  }, [date, reason, description, amount]);

  const close = useCallback(
    forcerefresh => {
      onClose(refreshNeeded || forcerefresh);
    },
    [onClose, refreshNeeded]
  );

  const save = useCallback(
    contnue => {
      const params = {
        data: moment(date, 'YYYY-MM-DD').format('DD/MM/YYYY'),
        codicecausale: reason,
        descrizione: description,
        importo: amount,
      };
      const thenFn = () => {
        enqueueSnackbar('Movimento salvato', { variant: 'success' });
        if (contnue) {
          setRefreshNeeded(true);
          setAmount(0);
        } else {
          close(true);
        }
      };
      const catchFn = err => {
        enqueueSnackbar(
          err.response?.statusText ||
            'Errore nel salvataggio del movimento contabile',
          { variant: 'error' }
        );
      };
      if (id) {
        apiPut(`/api/accounting/gas/entry/${id}`, params)
          .then(thenFn)
          .catch(catchFn);
      } else {
        apiPost('/api/accounting/gas/entry', params)
          .then(thenFn)
          .catch(catchFn);
      }
    },
    [close, id, date, reason, description, amount, enqueueSnackbar]
  );

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{movement ? 'Modifica' : 'Nuovo'} movimento</DialogTitle>

      <DialogContent className={classes.content}>
        <MuiPickersUtilsProvider
          libInstance={moment}
          utils={MomentUtils}
          locale="it"
        >
          <DatePicker
            className={classes.field}
            disableToolbar
            variant="inline"
            format="DD/MM/YYYY"
            margin="dense"
            id="date-picker-inline"
            label="Data del movimento"
            value={date ? moment(date, 'YYYY-MM-DD') : null}
            onChange={setDate}
            autoOk
            inputVariant="outlined"
            disableFuture
          />
        </MuiPickersUtilsProvider>

        <FormControl className={classes.field} variant="outlined" fullWidth>
          <InputLabel className={classes.selectLabel} id="reason-label">
            Causale
          </InputLabel>
          <Select
            labelId="reason-label"
            value={reason}
            onChange={evt => setReason(evt.target.value)}
            margin="dense"
          >
            {reasons.map(r => (
              <MenuItem key={`reason-${r.reasonCode}`} value={r.reasonCode}>
                {r.description} ({r.sign})
              </MenuItem>
            ))}
          </Select>
        </FormControl>

        <TextField
          className={classes.field}
          label="Descrizione"
          value={description}
          variant="outlined"
          size="small"
          InputLabelProps={{
            shrink: true,
          }}
          onChange={evt => setDescription(evt.target.value)}
          fullWidth
        />

        <TextField
          className={classes.field}
          label="Importo"
          type="number"
          variant="outlined"
          size="small"
          InputLabelProps={{
            shrink: true,
          }}
          InputProps={{
            endAdornment: (
              <InputAdornment position="end">
                <EuroIcon className={classes.icon} />
              </InputAdornment>
            ),
          }}
          value={amount}
          onChange={evt => setAmount(evt.target.value)}
        />
      </DialogContent>

      <DialogActions>
        <Button onClick={() => close()} autoFocus>
          Annulla
        </Button>
        {movement ? null : (
          <Button onClick={() => save(true)} disabled={!canSave}>
            Salva e continua
          </Button>
        )}
        <Button onClick={() => save(false)} disabled={!canSave}>
          Salva
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default withSnackbar(EditGasMovementDialog);
