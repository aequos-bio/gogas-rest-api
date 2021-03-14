/* eslint-disable jsx-a11y/no-static-element-interactions */
/* eslint-disable jsx-a11y/click-events-have-key-events */
/* eslint-disable jsx-a11y/control-has-associated-label */
import React, { useMemo, useCallback, useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import {
  Container,
  Fab,
  List,
  FormControlLabel,
  Switch,
} from '@material-ui/core';
import { AddSharp as PlusIcon } from '@material-ui/icons';
import _ from 'lodash';
import { makeStyles } from '@material-ui/core/styles';
import { withSnackbar } from 'notistack';
import { apiGetJson, apiPut, apiDelete } from '../../../utils/axios_utils';
import PageTitle from '../../../components/PageTitle';
import UserItem from './UserItem';
import LoadingListItem from '../../../components/LoadingListItem';
import UserEditDialog from './UserEditDialog';
import ActionDialog from '../../../components/ActionDialog';

const useStyles = makeStyles(theme => ({
  fab: {
    position: 'fixed',
    bottom: theme.spacing(2),
    right: theme.spacing(2),
  },
  userList: {
    width: '100%',
    backgroundColor: theme.palette.background.paper,
  },
}));

function Users({ enqueueSnackbar }) {
  const classes = useStyles();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [hideDisabled, setHideDisabled] = useState(true);
  const [selectedUser, setSelectedUser] = useState();
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [resetPasswordDialogOpen, setResetPasswordDialogOpen] = useState(false);
  const info = useSelector(state => state.info);

  const sort = info['visualizzazione.utenti']
    ? info['visualizzazione.utenti']
    : 'NC';

  const reload = useCallback(() => {
    setLoading(true);
    apiGetJson('/api/user/list', {}).then(uu => {
      setLoading(false);
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
  }, [sort, enqueueSnackbar]);

  useEffect(() => {
    reload();
  }, [reload]);

  const mapRef = useCallback(
    uid => {
      let friend = null;
      if (uid)
        users.forEach(u => {
          if (u.idUtente === uid)
            friend =
              sort === 'NC'
                ? `${u.nome} ${u.cognome}`
                : `${u.cognome} ${u.nome}`;
        });
      return friend;
    },
    [users, sort]
  );

  const newUser = useCallback(() => {
    setSelectedUser();
    setEditDialogOpen(true);
  }, []);

  const editUser = useCallback(user => {
    setSelectedUser(user);
    setEditDialogOpen(true);
  }, []);

  const editDialogClosed = useCallback(
    refresh => {
      setEditDialogOpen(false);
      setSelectedUser();
      if (refresh) reload();
    },
    [reload]
  );

  const deleteUser = useCallback(user => {
    setSelectedUser(user);
    setDeleteDialogOpen(true);
  }, []);

  const doDeleteUser = useCallback(() => {
    apiDelete(`/api/user/${selectedUser.idUtente}`)
      .then(() => {
        setDeleteDialogOpen(false);
        setSelectedUser();
        enqueueSnackbar('Utente eliminato', { variant: 'success' });
        reload();
      })
      .catch(err => {
        enqueueSnackbar(
          err.response && err.response.data
            ? err.response.data.debugMessage || err.response.data.message
            : `Errore durante l'eliminazione dell'utente: ${err}`,
          {
            variant: 'error',
          }
        );
      });
  }, [selectedUser, enqueueSnackbar, reload]);

  const enableUser = useCallback(
    user => {
      apiPut(`/api/user/${user.idUtente}`, { ...user, attivo: true })
        .then(() => {
          enqueueSnackbar('Utente riabilitato', { variant: 'success' });
          reload();
        })
        .catch(err => {
          enqueueSnackbar(
            err.response && err.response.data
              ? err.response.data.debugMessage || err.response.data.message
              : `Errore durante la riabilitazione dell'utente: ${err}`,
            { variant: 'error' }
          );
        });
    },
    [enqueueSnackbar, reload]
  );

  const disableUser = useCallback(
    user => {
      apiPut(`/api/user/${user.idUtente}`, { ...user, attivo: false })
        .then(() => {
          enqueueSnackbar('Utente disabilitato', { variant: 'success' });
          reload();
        })
        .catch(err => {
          enqueueSnackbar(
            err.response && err.response.data
              ? err.response.data.debugMessage || err.response.data.message
              : `Errore durante la disabilitazione dell'utente: ${err}`,
            { variant: 'error' }
          );
        });
    },
    [enqueueSnackbar, reload]
  );

  const passwordReset = useCallback(
    user => {
      if (user.email && user.email.length > 5) {
        setSelectedUser(user);
        setResetPasswordDialogOpen(true);
      } else {
        enqueueSnackbar(
          "Impossibile resettare la password: l'utente è privo di email",
          { variant: 'warning' }
        );
      }
    },
    [enqueueSnackbar]
  );

  const doResetPassword = useCallback(() => {
    apiPut(`/api/user/${selectedUser.idUtente}/password/reset`)
      .then(() => {
        enqueueSnackbar("La password dell'utente è stata resettata", {
          variant: 'success',
        });
        setResetPasswordDialogOpen(false);
        setSelectedUser();
        reload();
      })
      .catch(err => {
        enqueueSnackbar(
          err.response && err.response.data
            ? err.response.data.debugMessage || err.response.data.message
            : `Errore durante il reset della password dell'utente: ${err}`,
          { variant: 'error' }
        );
      });
  }, [enqueueSnackbar, reload, selectedUser]);

  const items = useMemo(() => {
    if (loading) {
      return <LoadingListItem />;
    }
    return users
      .filter(u => !hideDisabled || u.attivo)
      .map(user => (
        <UserItem
          key={user.idUtente}
          user={user}
          friend={mapRef(user.idReferente)}
          sort={sort}
          onEdit={editUser}
          onDelete={deleteUser}
          onPasswordReset={passwordReset}
          onEnable={enableUser}
          onDisable={disableUser}
        />
      ));
  }, [
    hideDisabled,
    loading,
    users,
    mapRef,
    sort,
    editUser,
    deleteUser,
    passwordReset,
    enableUser,
    disableUser,
  ]);

  return (
    <Container maxWidth={false}>
      <PageTitle title="Utenti">
        <FormControlLabel
          control={
            <Switch
              checked={hideDisabled}
              onChange={evt => setHideDisabled(evt.target.checked)}
              name="hideDisabled"
              color="primary"
            />
          }
          label="Nascondi inattivi"
        />
      </PageTitle>

      <List className={classes.userList}>{items}</List>

      <Fab className={classes.fab} color="secondary" onClick={newUser}>
        <PlusIcon />
      </Fab>

      <UserEditDialog
        open={editDialogOpen}
        onClose={editDialogClosed}
        user={selectedUser}
      />

      <ActionDialog
        open={deleteDialogOpen}
        onCancel={() => setDeleteDialogOpen(false)}
        actions={['Ok']}
        onAction={doDeleteUser}
        title="Conferma eliminazione"
        message={`Sei sicuro di voler eliminare l'utente ${selectedUser?.username}?`}
      />
      <ActionDialog
        open={resetPasswordDialogOpen}
        onCancel={() => setResetPasswordDialogOpen(false)}
        actions={['Ok']}
        onAction={doResetPassword}
        title="Conferma reset password"
        message={`Sei sicuro di voler resettare la password dell'utente ${selectedUser?.username}?`}
      />
    </Container>
  );
}

export default withSnackbar(Users);
