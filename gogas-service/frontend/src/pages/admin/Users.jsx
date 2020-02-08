/* eslint-disable jsx-a11y/no-static-element-interactions */
/* eslint-disable jsx-a11y/click-events-have-key-events */
/* eslint-disable jsx-a11y/control-has-associated-label */
import React, { useMemo, useCallback, useEffect, useState } from "react";
import { connect } from "react-redux";
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
import _ from "lodash";
import { makeStyles } from '@material-ui/core/styles';
import { withSnackbar } from 'notistack';
import { getJson } from "../../utils/axios_utils";
import PageTitle from '../../components/PageTitle';

const useStyles = makeStyles(theme => ({
  fab: {
    position: 'fixed',
    bottom: theme.spacing(2),
    right: theme.spacing(2),
  },
  tdIcon: {
    color: "red", 
    textAlign: "center",
    width: '30px',
  },
  tdButtons: {
    fontSize: '130%', 
    textAlign: 'center',
  }
}));

function Users({ info, enqueueSnackbar }) {
  const classes = useStyles();
  const [users, setUsers] = useState([]);
  const sort = info["visualizzazione.utenti"]
    ? info["visualizzazione.utenti"].value
    : "NC";

  const reload = useCallback(() => {
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
  }, [sort, enqueueSnackbar]);

  useEffect(() => {
    reload();
  }, [reload]);

  const mapRoles = useCallback(user => {
    if (user.ruolo === "U") return "Utente";
    if (user.ruolo === "A") return "Amministratore";
    if (user.ruolo === "S") return "Amico";
    return user.ruolo;
  }, []);

  const mapRef = useCallback(
    uid => {
      let friend = "";
      if (uid)
        users.forEach(u => {
          if (u.idUtente === uid)
            friend =
              sort === "NC"
                ? `${u.nome} ${u.cognome}`
                : `${u.cognome} ${u.nome}`;
        });
      return friend;
    },
    [users, sort]
  );

  const newUser = useCallback(() => {
    enqueueSnackbar('Funzione non implementata!',{variant:'error'})
  }, [enqueueSnackbar])

  const editUser = useCallback((id) => {
    enqueueSnackbar('Funzione non implementata!',{variant:'error'})
    console.warn(`Edit user ${id}`);
  }, [enqueueSnackbar]);
  
  const deleteUser = useCallback((id) => {
    enqueueSnackbar('Funzione non implementata!',{variant:'error'})
    console.warn(`Delete user ${id}`);
  }, [enqueueSnackbar]);

  const rows = useMemo(() => {
    if (users) {
      return users.map(u => (
        <TableRow key={`user-${u.idUtente}`} hover>
          <TableCell className={classes.tdIcon}>
            {u.attivo ? (
              ""
            ) : (
              <BlockIcon fontSize='small'/>
            )}
          </TableCell>
          <TableCell>{sort === "NC" ? u.nome : u.cognome}</TableCell>
          <TableCell>{sort === "NC" ? u.cognome : u.nome}</TableCell>
          <TableCell>{u.username}</TableCell>
          <TableCell>{u.email}</TableCell>
          <TableCell>{mapRoles(u)}</TableCell>
          <TableCell>{mapRef(u.idReferente)}</TableCell>
          <TableCell className={classes.tdButtons}>
            <IconButton onClick={() => { editUser(u.idUtente) }}>
              <EditIcon/>
            </IconButton>
            <IconButton onClick={() => { deleteUser(u.idUtente) }}>
              <DeleteIcon/>
            </IconButton>
          </TableCell>
        </TableRow>
      ));
    }
    return null;
  }, [users, classes, deleteUser, editUser, mapRef, mapRoles, sort]);

  return (
    <Container maxWidth='xl' >
      <PageTitle title='Gestione utenti'/>

      <Fab className={classes.fab} color='secondary' onClick={newUser}>
        <PlusIcon/>
      </Fab>

      <TableContainer >
        <Table size='small'>
          <TableHead>
            <TableRow>
              <TableCell className={classes.tdIcon}/>
              <TableCell>{sort === "NC" ? "Nome" : "Cognome"}</TableCell>
              <TableCell>{sort === "NC" ? "Cognome" : "Nome"}</TableCell>
              <TableCell>Username</TableCell>
              <TableCell>Email</TableCell>
              <TableCell>Ruolo</TableCell>
              <TableCell>Ref amico</TableCell>
              <TableCell/>
            </TableRow>
          </TableHead>

          <TableBody>
            {rows}
          </TableBody>
        </Table>
      </TableContainer>
    </Container>
  );
}

const mapStateToProps = state => {
  return {
    authentication: state.authentication,
    info: state.info
  };
};

const mapDispatchToProps = {};

export default connect(mapStateToProps, mapDispatchToProps)(withSnackbar(Users));
