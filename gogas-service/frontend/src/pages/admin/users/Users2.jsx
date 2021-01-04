/* eslint-disable jsx-a11y/no-static-element-interactions */
/* eslint-disable jsx-a11y/click-events-have-key-events */
/* eslint-disable jsx-a11y/control-has-associated-label */
import React, { useMemo, useCallback, useEffect, useState } from 'react';
import { connect } from 'react-redux';
import { Container, Fab, List } from '@material-ui/core';
import { AddSharp as PlusIcon } from '@material-ui/icons';
import _ from 'lodash';
import { makeStyles } from '@material-ui/core/styles';
import { withSnackbar } from 'notistack';
import { apiGetJson } from '../../../utils/axios_utils';
import PageTitle from '../../../components/PageTitle';
import UserItem from './UserItem';
import LoadingListItem from '../../../components/LoadingListItem';

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

function Users({ info, enqueueSnackbar }) {
  const classes = useStyles();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
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
    enqueueSnackbar('Funzione non implementata!', { variant: 'error' });
  }, [enqueueSnackbar]);

  const editUser = useCallback(
    id => {
      enqueueSnackbar('Funzione non implementata!', { variant: 'error' });
      console.warn(`Edit user ${id}`);
    },
    [enqueueSnackbar]
  );

  const deleteUser = useCallback(
    id => {
      enqueueSnackbar('Funzione non implementata!', { variant: 'error' });
      console.warn(`Delete user ${id}`);
    },
    [enqueueSnackbar]
  );

  const passwordReset = useCallback(
    id => {
      enqueueSnackbar('Funzione non implementata!', { variant: 'error' });
      console.warn(`Reset password of user ${id}`);
    },
    [enqueueSnackbar]
  );

  const items = useMemo(() => {
    if (loading) {
      return <LoadingListItem />;
    }
    return users.map(user => (
      <UserItem
        key={user.idUtente}
        user={user}
        friend={mapRef(user.idReferente)}
        sort={sort}
        onEdit={editUser}
        onDelete={deleteUser}
        onPasswordReset={passwordReset}
      />
    ));
  }, [loading, users, mapRef, sort, editUser, deleteUser, passwordReset]);

  return (
    <Container maxWidth={false}>
      <PageTitle title="Utenti" />

      <List className={classes.userList}>{items}</List>

      <Fab className={classes.fab} color="secondary" onClick={newUser}>
        <PlusIcon />
      </Fab>
    </Container>
  );
}

const mapStateToProps = state => {
  return {
    authentication: state.authentication,
    info: state.info,
  };
};

const mapDispatchToProps = {};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(withSnackbar(Users));
