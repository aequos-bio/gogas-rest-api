import React, { useCallback, useState, useEffect, useMemo } from 'react';
import {
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  InputAdornment
} from '@material-ui/core';
import {
  EuroSharp as EuroIcon,
  EventSharp as CalendarIcon
} from '@material-ui/icons';
import { connect } from "react-redux";
import NumPad from "react-numpad";
import _ from "lodash";
import moment from "moment-timezone";
import { makeStyles } from '@material-ui/core/styles';
import Select from "react-select";
import { withSnackbar } from 'notistack';
import { getJson, apiPost } from "../../../utils/axios_utils";

const useStyles = makeStyles((theme) => ({
  field: {
    marginBottom: theme.spacing(2),
  },
  icon: {
    color: theme.palette.grey[500]
  }
}));

const NewTransactionDialog = ({ open, onClose, info, user, enqueueSnackbar }) => {
  const classes = useStyles();
  const sort = info["visualizzazione.utenti"]
    ? info["visualizzazione.utenti"].value
    : "NC";
  const userLabel = useCallback(u => {
    const name =
      sort === "NC" ? `${u.nome} ${u.cognome}` : `${u.cognome} ${u.nome}`;
    const disa = u.attivo ? null : (
      <span className="fa fa-ban" style={{ color: "red" }} />
    );

    return (
      <span>
        {disa} {name}
      </span>
    );
  }, [sort]);

  const [_user, setUser] = useState(
    user ? { value: user, label: userLabel(user) } : undefined
  );
  const [date, setDate] = useState();
  const [reason, setReason] = useState();
  const [description, setDescription] = useState("");
  const [amount, setAmount] = useState(0);

  const [users, setUsers] = useState([]);
  const [reasons, setReasons] = useState([]);
  const [refreshNeeded, setRefreshNeeded] = useState(false);

  useEffect(() => {
    if (!open) return;

    setUser(user ? { value: user, label: userLabel(user) } : null);
    setReason(null);
    setDate();
    setDescription("");
    setAmount(0);

    getJson("/api/user/list", {}).then(uu => {
      if (uu.error) {
        enqueueSnackbar(uu.errorMessage,{variant:'error'})
      } else {
        setUsers(
          _.orderBy(
            uu,
            [
              "attivo",
              sort === "NC" ? "nome" : "cognome",
              sort === "NC" ? "cognome" : "nome"
            ],
            ["desc", "asc", "asc"]
          )
        );
      }
    });

    getJson("/api/accounting/reason/list", {}).then(rr => {
      if (rr.error) {
        enqueueSnackbar(rr.errorMessage,{variant:'error'})
      } else {
        setReasons(rr);
      }
    });
  }, [info, open, sort, user, userLabel, enqueueSnackbar]);

  const canSave = useMemo(() => {
    return _user && reason && date && description.length && amount;
  }, [_user, reason, date, description, amount]);

  const close = useCallback(
    forcerefresh => {
      onClose(refreshNeeded || forcerefresh);
    },
    [refreshNeeded, onClose]
  );

  const save = useCallback(contnue => {
    apiPost("/api/accounting/user/entry", {
      data: moment(date, "YYYY-MM-DD").format("DD/MM/YYYY"),
      idutente: _user.value.idUtente,
      nomeutente: `${_user.value.nome} ${_user.value.cognome}`,
      codicecausale: reason.value.reasonCode,
      nomecausale: reason.value.description,
      descrizione: description,
      importo: amount
    }).then(() => {
      if (contnue) {
        setRefreshNeeded(true);
        setUser(user ? { value: user, label: userLabel(user) } : null);
        setAmount(0);
      } else {
        close(true);
      }
    }).catch(err => {
      enqueueSnackbar(err,{variant:'error'})
    });
  }, [
    _user,
    reason,
    date,
    description,
    amount,
    setRefreshNeeded,
    close,
    user,
    userLabel,
    enqueueSnackbar
  ]);

  return (
    <Dialog open={open} onClose={onClose} maxWidth='xs' fullWidth>
      <DialogTitle>
        Nuovo movimento
      </DialogTitle>

      <DialogContent className={classes.content}>
        <Select className={classes.field}
          menuPortalTarget={document.body}
          styles={{ menuPortal: base => ({ ...base, zIndex: 9999 }) }}
          options={users.map(u => ({ value: u, label: userLabel(u) }))}
          placeholder="Selezionare un utente"
          onChange={u => setUser(u)}
          value={_user}
          isClearable
          isDisabled={user !== undefined}
        />

        <NumPad.Calendar
          position="startBottomLeft"
          locale="it"
          dateFormat="DD/MM/YYYY"
          onClick={e => {
            e.preventDefault();
            e.stopPropagation();
          }}
          onChange={value =>
            setDate(moment(value, "DD/MM/YYYY").format("YYYY-MM-DD"))
          }
          confirm={() => { }}
          value={date ? moment(date, "YYYY-MM-DD").format("DD/MM/YYYY") : ""}
        >
          <TextField className={classes.field}
            label="Data del movimento"
            value={date ? moment(date, "YYYY-MM-DD").format("DD/MM/YYYY") : ""}
            variant="outlined"
            size="small"
            InputLabelProps={{
              shrink: true,
            }}
            InputProps={{
              readOnly: true,
              endAdornment: (
                <InputAdornment position="end">
                  <CalendarIcon className={classes.icon} />
                </InputAdornment>
              ),
            }}

          />
        </NumPad.Calendar>

        <Select className={classes.field}
          menuPortalTarget={document.body}
          styles={{ menuPortal: base => ({ ...base, zIndex: 9999 }) }}
          options={reasons.map(r => ({ value: r, label: r.description }))}
          placeholder="Selezionare una causale"
          onChange={r => setReason(r)}
          value={reason}
          isClearable
        />

        <TextField className={classes.field}
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

        <TextField className={classes.field}
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
        <Button onClick={close} autoFocus>
          Annulla
        </Button>
        <Button onClick={() => save(true)} color='secondary' disabled={!canSave}>
          Salva e continua
        </Button>
        <Button onClick={() => save(false)} color='secondary' disabled={!canSave}>
          Salva
        </Button>
      </DialogActions>
    </Dialog>
  );
}

const mapStateToProps = state => {
  return {
    info: state.info
  };
};

const mapDispatchToProps = {};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(withSnackbar(NewTransactionDialog));
