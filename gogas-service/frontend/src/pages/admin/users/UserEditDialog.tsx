import React, { useMemo, useState, useCallback, useEffect, ChangeEvent } from 'react';
import {
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Grid,
  FormControl,
  FormLabel,
  FormControlLabel,
  RadioGroup,
  Radio,
  TextField,
  Select,
  InputLabel,
  MenuItem,
  ListItemText,
  ListItemIcon,
} from '@material-ui/core';
import {
  Check as CheckIcon,
  ErrorOutline as ErrorIcon,
  Block as DisabledIcon,
} from '@material-ui/icons';
import { useSnackbar } from 'notistack';
import { makeStyles } from '@material-ui/core/styles';
import { apiPut, apiPost, apiGetJson } from '../../../utils/axios_utils';
import { Friend, User } from './types';
import { useAppSelector } from '../../../store/store';

const MIN_PASSWORD_LEN = 6;

const useStyles = makeStyles(() => ({
  password: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    '&>:first-child': {
      flexGrow: 1,
    },
  },
  spacer: {
    width: '24px',
  },
  legend: {
    fontSize: '75%',
  },
}));

interface Props {
  open: boolean;
  onClose: (refresh?: boolean) => void;
  user?: User;
}

const UserEditDialog: React.FC<Props> = ({ open, onClose, user }) => {
  const classes = useStyles();
  const [username, setUsername] = useState(user?.username || '');
  const [firstname, setFirstname] = useState(user?.nome || '');
  const [lastname, setLastname] = useState(user?.cognome || '');
  const [email, setEmail] = useState(user?.email || '');
  const [password, setPassword] = useState('');
  const [passwordCheck, setPasswordCheck] = useState('');
  const [role, setRole] = useState('U');
  const [friends, setFriends] = useState<Friend[]>([]);
  const [friendRef, setFriendRef] = useState<string | undefined>(user?.idReferente || '');
  const info = useAppSelector((state) => state.info);
  const { enqueueSnackbar } = useSnackbar();

  useEffect(() => {
    if (!open) return;
    apiGetJson<User[]>('/api/user/list', { role: 'U' }).then((list) => {
      if (list) {
        setFriends(
          list
            .map(f => ({
              id: f.idUtente,
              description:
                info['visualizzazione.utenti'] === 'NC'
                  ? `${f.nome} ${f.cognome}`
                  : `${f.cognome} ${f.nome}`,
              attivo: f.attivo,
            }))
            .sort((f1, f2) => {
              if (f1.attivo === f2.attivo) {
                return f1.description > f2.description ? 1 : -1;
              }
              return f1.attivo > f2.attivo ? -1 : 1;
            })
        );
      }
    });
  }, [open, info]);

  useEffect(() => {
    if (!open) return;
    setUsername(user?.username || '');
    setFirstname(user?.nome || '');
    setLastname(user?.cognome || '');
    setEmail(user?.email || '');
    setPassword('');
    setPasswordCheck('');
    setRole(user?.ruolo || 'U');
    setFriendRef(user?.idReferente);
  }, [open, user]);

  const canSave = useMemo(() => {
    return (
      username.length > 1 &&
      firstname.length > 1 &&
      lastname.length > 1 &&
      email.length > 1 &&
      (user ||
        (password.length >= MIN_PASSWORD_LEN && password === passwordCheck)) &&
      role.length === 1 &&
      (role !== 'S' || friendRef !== undefined)
    );
  }, [
    username,
    firstname,
    lastname,
    email,
    password,
    passwordCheck,
    role,
    friendRef,
    user,
  ]);

  const save = useCallback(() => {
    if (user) {
      // update
      const _user = { ...user };
      _user.nome = firstname;
      _user.cognome = lastname;
      _user.email = email;
      _user.ruolo = role;
      _user.idReferente = role === 'S' ? friendRef : undefined;
      apiPut(`/api/user/${_user.idUtente}`, _user)
        .then(() => {
          enqueueSnackbar(`Utente salvato con successo`, {
            variant: 'success',
          });
          onClose(true);
        })
        .catch(err => {
          enqueueSnackbar(`Errore nel salvataggio: ${err}`, {
            variant: 'error',
          });
        });
    } else {
      // insert
      const _user = {
        username,
        nome: firstname,
        cognome: lastname,
        email,
        password,
        ruolo: role,
        attivo: true,
        idReferente: role === 'S' ? friendRef : undefined,
      };
      apiPost('/api/user', _user)
        .then(() => {
          enqueueSnackbar(`Utente salvato con successo`, {
            variant: 'success',
          });
          onClose(true);
        })
        .catch(err => {
          enqueueSnackbar(`Errore nel salvataggio: ${err}`, {
            variant: 'error',
          });
        });
    }
  }, [
    user,
    username,
    firstname,
    lastname,
    email,
    password,
    role,
    friendRef,
    onClose,
    enqueueSnackbar,
  ]);

  const cancel = useCallback(() => {
    onClose();
  }, [onClose]);

  const passwordError = useMemo(() => {
    return password.length && password.length < MIN_PASSWORD_LEN;
  }, [password]);

  const passwordCheckError = useMemo(() => {
    return password !== passwordCheck;
  }, [password, passwordCheck]);

  return (
    <Dialog open={open} onClose={cancel} maxWidth="xs" fullWidth>
      <DialogTitle>{user ? 'Modifica' : 'Nuovo'} utente</DialogTitle>

      <DialogContent>
        <Grid container spacing={2}>
          <Grid item xs={12}>
            <TextField
              disabled={user !== undefined}
              fullWidth
              label="Nome utente"
              value={username}
              size="small"
              onChange={evt => setUsername(evt.target.value)}
            />
          </Grid>
          <Grid item xs={12}>
            <TextField
              fullWidth
              label="Nome"
              value={firstname}
              size="small"
              onChange={evt => setFirstname(evt.target.value)}
            />
          </Grid>
          <Grid item xs={12}>
            <TextField
              fullWidth
              label="Cognome"
              value={lastname}
              size="small"
              onChange={evt => setLastname(evt.target.value)}
            />
          </Grid>
          <Grid item xs={12}>
            <TextField
              fullWidth
              label="Email"
              value={email}
              size="small"
              onChange={evt => setEmail(evt.target.value)}
            />
          </Grid>
          {user ? null : (
            <Grid item xs={12} className={classes.password}>
              <FormControl error>
                <TextField
                  error={!!passwordError}
                  fullWidth
                  autoComplete="new-password"
                  label="Password"
                  value={password}
                  size="small"
                  type="password"
                  onChange={evt => setPassword(evt.target.value)}
                  helperText={
                    passwordError
                      ? `La password deve essere di almeno ${MIN_PASSWORD_LEN} caratteri`
                      : ''
                  }
                />
              </FormControl>
              {password.length ? (
                passwordError ? (
                  <ErrorIcon />
                ) : (
                  <CheckIcon />
                )
              ) : (
                <div className={classes.spacer} />
              )}
            </Grid>
          )}
          {user ? null : (
            <Grid item xs={12} className={classes.password}>
              <TextField
                error={passwordCheckError}
                fullWidth
                autoComplete="password-check"
                label="Password (verifica)"
                value={passwordCheck}
                size="small"
                type="password"
                onChange={evt => setPasswordCheck(evt.target.value)}
                helperText={
                  passwordCheckError ? 'Le due password sono diverse' : ''
                }
              />
              {password.length || passwordCheck.length ? (
                passwordCheckError ? (
                  <ErrorIcon />
                ) : (
                  <CheckIcon />
                )
              ) : (
                <div className={classes.spacer} />
              )}
            </Grid>
          )}

          <Grid item xs={12}>
            <FormControl component="fieldset">
              <FormLabel component="legend" className={classes.legend}>
                Ruolo
              </FormLabel>
              <RadioGroup
                name="ruolo"
                value={role}
                onChange={evt => setRole(evt.target.value)}
              >
                <FormControlLabel
                  value="U"
                  control={<Radio />}
                  label="Utente"
                />
                <FormControlLabel
                  value="A"
                  control={<Radio />}
                  label="Amministratore"
                />
                <FormControlLabel value="S" control={<Radio />} label="Amico" />
              </RadioGroup>
            </FormControl>
          </Grid>

          {role === 'S' ? (
            <Grid item xs={12}>
              <FormControl fullWidth>
                <InputLabel id="friend-ref-label">Referente amico</InputLabel>
                <Select
                  labelId="friend-ref-label"
                  value={friendRef}
                  onChange={(evt: any) => {
                    setFriendRef(evt.target.value);
                  }}
                >
                  {friends.map(f => {
                    return (
                      <MenuItem key={`friend-ref-${f.id}`} value={f.id}>
                        <ListItemIcon>
                          {f.attivo ? null : <DisabledIcon />}
                        </ListItemIcon>
                        <ListItemText>{f.description}</ListItemText>
                      </MenuItem>
                    );
                  })}
                </Select>
              </FormControl>
            </Grid>
          ) : null}
        </Grid>
      </DialogContent>

      <DialogActions>
        <Button onClick={cancel}>Annulla</Button>

        <Button onClick={save} disabled={!canSave}>
          Salva
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default UserEditDialog;
