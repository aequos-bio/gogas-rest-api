/* eslint-disable jsx-a11y/no-static-element-interactions */
/* eslint-disable jsx-a11y/click-events-have-key-events */
/* eslint-disable jsx-a11y/control-has-associated-label */
import React, { useMemo, useCallback, useEffect, useState } from 'react';
import { connect } from 'react-redux';
import {
  Container,
  Fab,
  IconButton,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
} from '@material-ui/core';
import {
  EditSharp as EditIcon,
  DeleteSharp as DeleteIcon,
  BlockSharp as BlockIcon,
  AddSharp as PlusIcon,
} from '@material-ui/icons';
import _ from 'lodash';
import { makeStyles } from '@material-ui/core/styles';
import { withSnackbar } from 'notistack';
import { apiGetJson } from '../../utils/axios_utils';
import PageTitle from '../../components/PageTitle';
import LoadingRow from '../../components/LoadingRow';

const useStyles = makeStyles(theme => ({
  fab: {
    position: 'fixed',
    bottom: theme.spacing(2),
    right: theme.spacing(2),
  },
  tableHead: {
    '@media screen and (max-width: 960px)': {
      display: 'none',
    },
  },
  tableBody: {
    '@media screen and (max-width: 960px)': {
      display: 'block',
    },
  },
  tableRow: {
    '@media screen and (max-width: 960px)': {
      display: 'block',
      border: '1px solid #e0e0e0',
      borderRadius: '5px',
      marginBottom: theme.spacing(2),
    },
  },
  tableCell: {
    '@media screen and (max-width: 960px)': {
      backgroundColor: 'white',
      display: 'block',
      verticalAlign: 'middle',
      textAlign: 'right',
      borderWidth: 0,
      minHeight: '20px',

      '&:before': {
        content: 'attr(data-title)',
        float: 'left',
        fontSize: 'inherit',
        fontWeight: 'bold',
      },
    },
  },
  tdIcon: {
    color: 'red',
    textAlign: 'center',
    width: '30px',
  },
  tdButtons: {
    fontSize: '130%',
    textAlign: 'center',
    minWidth: '88px',
  },
}));

function Users({ info, enqueueSnackbar }) {
  const classes = useStyles();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const sort = info['visualizzazione.utenti']
    ? info['visualizzazione.utenti'].value
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

  const mapRoles = useCallback(user => {
    if (user.ruolo === 'U') return 'Utente';
    if (user.ruolo === 'A') return 'Amministratore';
    if (user.ruolo === 'S') return 'Amico';
    return user.ruolo;
  }, []);

  const mapRef = useCallback(
    uid => {
      let friend = '';
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

  const rows = useMemo(() => {
    if (loading) {
      return <LoadingRow colSpan={8} />;
    }
    if (users) {
      return users.map(u => (
        <TableRow key={`user-${u.idUtente}`} hover className={classes.tableRow}>
          <TableCell className={`${classes.tdIcon} ${classes.tableCell}`}>
            {u.attivo ? '' : <BlockIcon fontSize="small" />}
          </TableCell>
          <TableCell
            className={classes.tableCell}
            data-title={sort === 'NC' ? 'Nome' : 'Cognome'}
          >
            {sort === 'NC' ? u.nome : u.cognome}
          </TableCell>
          <TableCell
            className={classes.tableCell}
            data-title={sort === 'NC' ? 'Cognome' : 'Nome'}
          >
            {sort === 'NC' ? u.cognome : u.nome}
          </TableCell>
          <TableCell className={classes.tableCell} data-title="Username">
            {u.username}
          </TableCell>
          <TableCell className={classes.tableCell} data-title="Email">
            {u.email}
          </TableCell>
          <TableCell className={classes.tableCell} data-title="Ruolo">
            {mapRoles(u)}
          </TableCell>
          <TableCell className={classes.tableCell} data-title="Ref. amico">
            {mapRef(u.idReferente)}
          </TableCell>
          <TableCell className={`${classes.tdButtons} ${classes.tableCell}`}>
            <IconButton
              onClick={() => {
                editUser(u.idUtente);
              }}
            >
              <EditIcon fontSize="small" />
            </IconButton>
            <IconButton
              onClick={() => {
                deleteUser(u.idUtente);
              }}
            >
              <DeleteIcon fontSize="small" />
            </IconButton>
          </TableCell>
        </TableRow>
      ));
    }
    return null;
  }, [users, classes, deleteUser, editUser, mapRef, mapRoles, sort, loading]);

  return (
    <Container maxWidth={false}>
      <PageTitle title="Utenti" />

      <Fab className={classes.fab} color="secondary" onClick={newUser}>
        <PlusIcon />
      </Fab>

      <TableContainer>
        <Table size="small">
          <TableHead className={classes.tableHead}>
            <TableRow>
              <TableCell className={classes.tdIcon} />
              <TableCell>{sort === 'NC' ? 'Nome' : 'Cognome'}</TableCell>
              <TableCell>{sort === 'NC' ? 'Cognome' : 'Nome'}</TableCell>
              <TableCell>Username</TableCell>
              <TableCell>Email</TableCell>
              <TableCell>Ruolo</TableCell>
              <TableCell>Ref amico</TableCell>
              <TableCell />
            </TableRow>
          </TableHead>

          <TableBody className={classes.tableBody}>{rows}</TableBody>
        </Table>
      </TableContainer>
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
