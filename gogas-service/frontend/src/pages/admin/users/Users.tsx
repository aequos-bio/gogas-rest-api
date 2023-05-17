import React, { useMemo, useCallback, useState } from 'react';
import { useSelector } from 'react-redux';
import {
  Container,
  Fab,
  List,
  FormControlLabel,
  Switch,
} from '@material-ui/core';
import { AddSharp as PlusIcon } from '@material-ui/icons';
import { makeStyles } from '@material-ui/core/styles';
import PageTitle from '../../../components/PageTitle';
import UserRow from './UserRow';
import LoadingListItem from '../../../components/LoadingListItem';
import UserEditDialog from './UserEditDialog';
import ActionDialog from '../../../components/ActionDialog';
import { RootState } from '../../../store/store';
import { useUsersAPI } from './useUsersAPI';
import { User } from './types';
import { useSnackbar } from 'notistack';

const useStyles = makeStyles((theme) => ({
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

const mapReference = (uid: string | undefined, users: User[], sort: string) => {
  let friend = undefined;
  if (uid)
    users.forEach((u) => {
      if (u.idUtente === uid)
        friend =
          sort === 'NC'
            ? `${u.nome} ${u.cognome}`
            : `${u.cognome} ${u.nome}`;
    });
  return friend;
};

const Users: React.FC = () => {
  const classes = useStyles();
  const [hideDisabled, setHideDisabled] = useState(true);
  const [selectedUser, setSelectedUser] = useState<User | undefined>(undefined);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [resetPasswordDialogOpen, setResetPasswordDialogOpen] = useState(false);
  const info = useSelector((state: RootState) => state.info);
  const sort = info['visualizzazione.utenti']
    ? info['visualizzazione.utenti']
    : 'NC';
  const { users, loading, reload, deleteUser, enableUser, disableUser, resetPassword } = useUsersAPI(sort);
  const { enqueueSnackbar } = useSnackbar();

  const newUser = useCallback(() => {
    setSelectedUser(undefined);
    setEditDialogOpen(true);
  }, []);

  const editUser = useCallback((user) => {
    setSelectedUser(user);
    setEditDialogOpen(true);
  }, []);

  const editDialogClosed = useCallback(
    (refresh) => {
      setEditDialogOpen(false);
      setSelectedUser(undefined);
      if (refresh) reload();
    },
    [reload],
  );

  const _deleteUser = useCallback((user) => {
    setSelectedUser(user);
    setDeleteDialogOpen(true);
  }, []);

  const doDeleteUser = useCallback(() => {
    if (!selectedUser) return;
    deleteUser(selectedUser.idUtente).then(() => {
      setDeleteDialogOpen(false);
      setSelectedUser(undefined);
    })
  }, [selectedUser]);

  const _enableUser = useCallback(
    (user) => {
      enableUser(user).then(() => { })
    },
    [],
  );

  const _disableUser = useCallback(
    (user) => {
      disableUser(user).then(() => { })
    },
    [],
  );

  const passwordReset = useCallback(
    (user) => {
      if (user.email && user.email.length > 5) {
        setSelectedUser(user);
        setResetPasswordDialogOpen(true);
      } else {
        enqueueSnackbar(
          "Impossibile resettare la password: l'utente è privo di email",
          { variant: 'warning' },
        );
      }
    },
    [enqueueSnackbar],
  );

  const doResetPassword = useCallback(() => {
    if (!selectedUser) return;
    resetPassword(selectedUser.idUtente).then(() => {
      setResetPasswordDialogOpen(false);
      setSelectedUser(undefined);
    })
  }, [enqueueSnackbar, reload, selectedUser]);

  const items = useMemo(() => {
    if (loading) {
      return <LoadingListItem />;
    }
    return users
      .filter((u) => !hideDisabled || u.attivo)
      .map((user) => (
        <UserRow
          key={user.idUtente}
          user={user}
          friend={mapReference(user.idReferente, users, sort)}
          sort={sort}
          onEdit={editUser}
          onDelete={_deleteUser}
          onPasswordReset={passwordReset}
          onEnable={_enableUser}
          onDisable={_disableUser}
        />
      ));
  }, [
    hideDisabled,
    loading,
    users,
    sort,
    editUser,
    deleteUser,
    passwordReset,
    enableUser,
    disableUser,
  ]);

  return (
    <Container maxWidth={false}>
      <PageTitle title='Utenti'>
        <FormControlLabel
          control={
            <Switch
              checked={hideDisabled}
              onChange={(evt) => setHideDisabled(evt.target.checked)}
              name='hideDisabled'
              color='primary'
            />
          }
          label='Nascondi inattivi'
        />
      </PageTitle>

      <List className={classes.userList}>{items}</List>

      <Fab className={classes.fab} color='secondary' onClick={newUser}>
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
        title='Conferma eliminazione'
        message={`Sei sicuro di voler eliminare l'utente ${selectedUser?.username}?`}
      />
      <ActionDialog
        open={resetPasswordDialogOpen}
        onCancel={() => setResetPasswordDialogOpen(false)}
        actions={['Ok']}
        onAction={doResetPassword}
        title='Conferma reset password'
        message={`Sei sicuro di voler resettare la password dell'utente ${selectedUser?.username}?`}
      />
    </Container>
  );
}

export default Users;
