import React, { useCallback, useState, useEffect, useMemo } from 'react';
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
import { MuiPickersUtilsProvider, DatePicker } from '@material-ui/pickers';
import MomentUtils from '@date-io/moment';
import Select from 'react-select';
import moment, { Moment } from 'moment-timezone';
import { makeStyles } from '@material-ui/core/styles';
import { User } from '../../admin/users/types';
import { useAppSelector } from '../../../store/store';
import { useUsersAPI } from '../../admin/users/useUsersAPI';
import { useReasonsAPI } from '../reasons/useReasonsAPI';
import { useUserMovementsAPI } from './useUserMovementsAPI';
import { MaterialUiPickersDate } from '@material-ui/pickers/typings/date';
import { Reason } from '../reasons/types';
import { UserMovement } from './types';

const useStyles = makeStyles((theme) => ({
  field: {
    marginBottom: theme.spacing(2),
  },
  icon: {
    color: theme.palette.grey[500],
  },
}));

interface Props {
  open: boolean;
  onClose: (refresh: boolean) => void;
  user?: User;
  transactionId?: string;
  friends?: boolean
}

const userLabel = (u: User, sort: string) => {
  const name =
    sort === 'NC' ? `${u.nome} ${u.cognome}` : `${u.cognome} ${u.nome}`;
  const disa = u.attivo ? null : (
    <span className='fa fa-ban' style={{ color: 'red' }} />
  );

  return (
    <span>
      {disa} {name}
    </span>
  );
};

const EditUserMovementDialog: React.FC<Props> = ({
  open,
  onClose,
  user,
  transactionId,
  friends
}) => {
  const classes = useStyles();
  const [date, setDate] = useState<string | undefined>(undefined);
  const [reason, setReason] = useState<{ value: { reasonCode: string, description: string }, label: string } | undefined>(undefined);
  const [description, setDescription] = useState('');
  const [amount, setAmount] = useState(0);
  const [refreshNeeded, setRefreshNeeded] = useState(false);
  const info = useAppSelector((state) => state.info);
  const sort = info['visualizzazione.utenti']
    ? info['visualizzazione.utenti']
    : 'NC';
  const { users, reload: reloadUsers } = useUsersAPI(sort);
  const [_user, setUser] = useState<{ value: User, label: JSX.Element } | undefined>(
    user ? { value: user, label: userLabel(user, sort) } : undefined,
  );
  const { reasons, reload: reloadReasons } = useReasonsAPI();
  const { getUserMovement, insertUserMovement, updateUserMovement } = useUserMovementsAPI(!!friends);

  useEffect(() => {
    if (!open) return;

    if (transactionId) {
      getUserMovement(transactionId).then(userMovement => {
        if (userMovement) {
          setUser(user ? { value: user, label: userLabel(user, sort) } : undefined);
          setReason({
            value: {
              reasonCode: userMovement.codicecausale,
              description: userMovement.nomecausale,
            },
            label: userMovement.nomecausale,
          });
          setDate(moment(userMovement.data, 'DD/MM/YYYY').format('YYYY-MM-DD'));
          setDescription(userMovement.descrizione);
          setAmount(userMovement.importo);
        }
      })
    } else {
      setUser(user ? { value: user, label: userLabel(user, sort) } : undefined);
      setReason(undefined);
      setDate(undefined);
      setDescription('');
      setAmount(0);
    }

    if (!friends) {
      reloadUsers();
    }

    reloadReasons();
  }, [info, open, sort, user, userLabel, transactionId]);

  const canSave = useMemo(() => {
    return _user && reason && date && description.length && amount;
  }, [_user, reason, date, description, amount]);

  const close = useCallback(
    (forcerefresh) => {
      onClose(refreshNeeded || forcerefresh);
    },
    [refreshNeeded, onClose],
  );

  const save = useCallback((contnue) => {
    if (!_user || !reason) return;
    const userMovement: UserMovement = {
      data: moment(date, 'YYYY-MM-DD').format('DD/MM/YYYY'),
      idutente: _user.value.idUtente,
      nomeutente: `${_user.value.nome} ${_user.value.cognome}`,
      codicecausale: reason.value.reasonCode,
      nomecausale: reason.value.description,
      descrizione: description,
      importo: amount,
    };
    if (transactionId) {
      insertUserMovement(transactionId, userMovement).then(() => {
        if (contnue) {
          setRefreshNeeded(true);
          setUser(user ? { value: user, label: userLabel(user, sort) } : undefined);
          setAmount(0);
        } else {
          close(true);
        }
      })
    } else {
      updateUserMovement(userMovement).then(() => {
        if (contnue) {
          setRefreshNeeded(true);
          setUser(user ? { value: user, label: userLabel(user, sort) } : undefined);
          setAmount(0);
        } else {
          close(true);
        }
      })
    }
  },
    [
      transactionId,
      _user,
      reason,
      date,
      description,
      amount,
      setRefreshNeeded,
      close,
      user,
      userLabel,
    ],
  );

  return (
    <Dialog open={open} onClose={onClose} maxWidth='xs' fullWidth>
      <DialogTitle>Nuovo movimento</DialogTitle>

      <DialogContent>
        <Select
          className={classes.field}
          menuPortalTarget={document.body}
          styles={{ menuPortal: (base: any) => ({ ...base, zIndex: 9999 }) }}
          options={users.map((u) => ({ value: u, label: userLabel(u, sort) }))}
          placeholder='Selezionare un utente'
          onChange={(u: { value: User, label: JSX.Element }) => setUser(u)}
          value={_user}
          isClearable
          isDisabled={user !== undefined}
        />

        <MuiPickersUtilsProvider
          libInstance={moment}
          utils={MomentUtils}
          locale='it'
        >
          <DatePicker
            className={classes.field}
            disableToolbar
            variant='inline'
            format='DD/MM/YYYY'
            margin='dense'
            id='date-picker-inline'
            label='Data del movimento'
            value={date ? moment(date, 'YYYY-MM-DD') : null}
            onChange={(date: MaterialUiPickersDate) => { setDate((date as Moment).format('YYYY-MM-DD')) }}
            autoOk
            inputVariant='outlined'
          />
        </MuiPickersUtilsProvider>

        <Select
          className={classes.field}
          menuPortalTarget={document.body}
          styles={{ menuPortal: (base: any) => ({ ...base, zIndex: 9999 }) }}
          options={reasons.map((r) => ({ value: r, label: r.description }))}
          placeholder='Selezionare una causale'
          onChange={(r: { value: Reason, label: string }) => setReason(r)}
          value={reason}
          isClearable
        />

        <TextField
          className={classes.field}
          label='Descrizione'
          value={description}
          variant='outlined'
          size='small'
          InputLabelProps={{
            shrink: true,
          }}
          onChange={(evt) => setDescription(evt.target.value)}
          fullWidth
        />

        <TextField
          className={classes.field}
          label='Importo'
          type='number'
          variant='outlined'
          size='small'
          InputLabelProps={{
            shrink: true,
          }}
          InputProps={{
            endAdornment: (
              <InputAdornment position='end'>
                <EuroIcon className={classes.icon} />
              </InputAdornment>
            ),
          }}
          value={amount}
          onChange={(evt) => setAmount(evt.target.value as unknown as number)}
        />
      </DialogContent>

      <DialogActions>
        <Button onClick={() => close(false)} autoFocus>
          Annulla
        </Button>
        {transactionId ? null : (
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

export default EditUserMovementDialog;
