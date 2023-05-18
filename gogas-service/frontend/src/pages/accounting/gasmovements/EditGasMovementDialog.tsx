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
import moment, { Moment } from 'moment-timezone';
import { makeStyles } from '@material-ui/core/styles';
import { GasMovement, GasMovementView } from './types';
import { useReasonsAPI } from '../reasons/useReasonsAPI';
import { useGasMovementsAPI } from './useGasMovementsAPI';
import { MaterialUiPickersDate } from '@material-ui/pickers/typings/date';

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

interface Props {
  open: boolean;
  onClose: (refresh: boolean) => void;
  movement?: GasMovementView;
}

const EditGasMovementDialog: React.FC<Props> = ({
  open,
  onClose,
  movement,
}) => {
  const classes = useStyles();
  const [id, setId] = useState<string | undefined>(undefined);
  const [date, setDate] = useState<string | undefined>(undefined);
  const [reason, setReason] = useState<string | undefined>(undefined);
  const [description, setDescription] = useState<string>('');
  const [amount, setAmount] = useState<number>(0);
  const [refreshNeeded, setRefreshNeeded] = useState(false);
  const { reasons, reload } = useReasonsAPI();
  const { insertGasMovement, updateGasMovement } = useGasMovementsAPI();

  useEffect(() => {
    if (!open) return;
    reload();

    if (movement) {
      setId(movement.id);
      setReason(movement.codicecausale);
      setDate(moment(movement.data, 'DD/MM/YYYY').format('YYYY-MM-DD'));
      setDescription(movement.descrizione);
      setAmount(movement.importo);
    } else {
      setId(undefined);
      setReason(undefined);
      setDate(undefined);
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
      const params: GasMovement = {
        data: moment(date, 'YYYY-MM-DD').format('DD/MM/YYYY'),
        codicecausale: reason,
        descrizione: description,
        importo: amount,
      };
      if (id) {
        insertGasMovement(id, params).then(() => {
          if (contnue) {
            setRefreshNeeded(true);
            setAmount(0);
          } else {
            close(true);
          }
        })
      } else {
        updateGasMovement(params).then(() => {
          if (contnue) {
            setRefreshNeeded(true);
            setAmount(0);
          } else {
            close(true);
          }
        })
      }
    },
    [close, id, date, reason, description, amount]
  );

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{movement ? 'Modifica' : 'Nuovo'} movimento</DialogTitle>

      <DialogContent>
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
            onChange={(date: MaterialUiPickersDate) => { setDate((date as Moment).format('YYYY-MM-DD')) }}
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
            onChange={evt => setReason(evt.target.value as string)}
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
          onChange={evt => setAmount(evt.target.value as unknown as number)}
        />
      </DialogContent>

      <DialogActions>
        <Button onClick={() => close(false)} autoFocus>
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

export default EditGasMovementDialog;
