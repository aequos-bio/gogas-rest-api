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
import { useSelector } from 'react-redux';
import _ from 'lodash';
import moment from 'moment-timezone';
import { makeStyles } from '@material-ui/core/styles';
import Select from 'react-select';
import { withSnackbar } from 'notistack';
import { apiGetJson, apiPost, apiPut } from '../../../utils/axios_utils';

const useStyles = makeStyles(theme => ({
  field: {
    marginBottom: theme.spacing(2),
  },
  icon: {
    color: theme.palette.grey[500],
  },
}));

const EditTransactionDialog = ({
  open,
  onClose,
  user,
  transactionId,
  enqueueSnackbar,
}) => {
  const classes = useStyles();
  const [date, setDate] = useState();
  const [reason, setReason] = useState();
  const [description, setDescription] = useState('');
  const [amount, setAmount] = useState(0);
  const [users, setUsers] = useState([]);
  const [reasons, setReasons] = useState([]);
  const [refreshNeeded, setRefreshNeeded] = useState(false);
  const info = useSelector(state => state.info);
  const sort = info['visualizzazione.utenti']
    ? info['visualizzazione.utenti']
    : 'NC';

  const userLabel = useCallback(
    u => {
      const name =
        sort === 'NC' ? `${u.nome} ${u.cognome}` : `${u.cognome} ${u.nome}`;
      const disa = u.attivo ? null : (
        <span className="fa fa-ban" style={{ color: 'red' }} />
      );

      return (
        <span>
          {disa} {name}
        </span>
      );
    },
    [sort]
  );
  const [_user, setUser] = useState(
    user ? { value: user, label: userLabel(user) } : undefined
  );

  useEffect(() => {
    if (!open) return;

    if (transactionId) {
      apiGetJson(`/api/accounting/user/entry/${transactionId}`, {})
        .then(t => {
          if (t.error) {
            enqueueSnackbar(t.errorMessage, { variant: 'error' });
          } else {
            setUser(user ? { value: user, label: userLabel(user) } : null);
            setReason({
              value: {
                reasonCode: t.codicecausale,
                description: t.nomecausale,
              },
              label: t.nomecausale,
            });
            setDate(moment(t.data, 'DD/MM/YYYY').format('YYYY-MM-DD'));
            setDescription(t.descrizione);
            setAmount(t.importo);
          }
        })
        .catch(err => {
          enqueueSnackbar(
            err.response?.statusText || 'Errore nel caricamento del movimento',
            { variant: 'error' }
          );
        });
    } else {
      setUser(user ? { value: user, label: userLabel(user) } : null);
      setReason(null);
      setDate();
      setDescription('');
      setAmount(0);
    }

    apiGetJson('/api/user/list', {}).then(uu => {
      if (uu.error) {
        enqueueSnackbar(uu.errorMessage, { variant: 'error' });
      } else {
        setUsers(
          _.orderBy(
            uu,
            [
              'attivo',
              sort === 'NC' ? 'nome' : 'cognome',
              sort === 'NC' ? 'cognome' : 'nome',
            ],
            ['desc', 'asc', 'asc']
          )
        );
      }
    });

    apiGetJson('/api/accounting/reason/list', {}).then(rr => {
      if (rr.error) {
        enqueueSnackbar(rr.errorMessage, { variant: 'error' });
      } else {
        setReasons(rr);
      }
    });
  }, [info, open, sort, user, userLabel, transactionId, enqueueSnackbar]);

  const canSave = useMemo(() => {
    return _user && reason && date && description.length && amount;
  }, [_user, reason, date, description, amount]);

  const close = useCallback(
    forcerefresh => {
      onClose(refreshNeeded || forcerefresh);
    },
    [refreshNeeded, onClose]
  );

  const save = useCallback(
    contnue => {
      const params = {
        data: moment(date, 'YYYY-MM-DD').format('DD/MM/YYYY'),
        idutente: _user.value.idUtente,
        nomeutente: `${_user.value.nome} ${_user.value.cognome}`,
        codicecausale: reason.value.reasonCode,
        nomecausale: reason.value.description,
        descrizione: description,
        importo: amount,
      };
      const thenFn = () => {
        enqueueSnackbar('Movimento salvato', { variant: 'success' });
        if (contnue) {
          setRefreshNeeded(true);
          setUser(user ? { value: user, label: userLabel(user) } : null);
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

      if (transactionId) {
        apiPut(`/api/accounting/user/entry/${transactionId}`, params)
          .then(thenFn)
          .catch(catchFn);
      } else {
        apiPost('/api/accounting/user/entry', params)
          .then(thenFn)
          .catch(catchFn);
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
      enqueueSnackbar,
    ]
  );

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Nuovo movimento</DialogTitle>

      <DialogContent className={classes.content}>
        <Select
          className={classes.field}
          menuPortalTarget={document.body}
          styles={{ menuPortal: base => ({ ...base, zIndex: 9999 }) }}
          options={users.map(u => ({ value: u, label: userLabel(u) }))}
          placeholder="Selezionare un utente"
          onChange={u => setUser(u)}
          value={_user}
          isClearable
          isDisabled={user !== undefined}
        />

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
          />
        </MuiPickersUtilsProvider>

        <Select
          className={classes.field}
          menuPortalTarget={document.body}
          styles={{ menuPortal: base => ({ ...base, zIndex: 9999 }) }}
          options={reasons.map(r => ({ value: r, label: r.description }))}
          placeholder="Selezionare una causale"
          onChange={r => setReason(r)}
          value={reason}
          isClearable
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

export default withSnackbar(EditTransactionDialog);
