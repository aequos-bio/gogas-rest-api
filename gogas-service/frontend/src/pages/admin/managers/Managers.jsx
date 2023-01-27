import React, { useEffect, useCallback, useState, useMemo } from 'react';
import {
  Container,
  IconButton,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
  Switch,
  FormControlLabel,
} from '@material-ui/core';
import { EditSharp as EditIcon } from '@material-ui/icons';
import { withSnackbar } from 'notistack';
import { makeStyles } from '@material-ui/core/styles';
import _ from 'lodash';
import { apiGetJson } from '../../../utils/axios_utils';
import PageTitle from '../../../components/PageTitle';
import LoadingRow from '../../../components/LoadingRow';
import ManagerEditDialog from './ManagerEditDialog';

const useStyles = makeStyles((theme) => ({
  fab: {
    position: 'fixed',
    bottom: theme.spacing(2),
    right: theme.spacing(2),
  },
  tableCell: {
    paddingLeft: theme.spacing(1),
    paddingRight: theme.spacing(1),
    verticalAlign: 'top',
    width: '25%',
  },
  tdButtons: {
    fontSize: '130%',
    textAlign: 'center',
    minWidth: '44px',
    width: '44px',
  },
  cellHeader: {
    paddingLeft: theme.spacing(1),
    paddingRight: theme.spacing(1),
  },
  inactiveLabel: {
    fontSize: '80%',
    marginLeft: theme.spacing(2),
    padding: theme.spacing(0, 0.5),
    borderRadius: '3px',
    color: theme.palette.getContrastText(theme.palette.error.main),
    backgroundColor: theme.palette.error.main,
  },
}));

const userSorter = (user1, user2) => {
  if (user1.attivo === user2.attivo) {
    if (user1.nome > user2.nome) return 1;
    if (user1.nome < user2.nome) return -1;
    if (user1.cognome > user2.cognome) return 1;
    if (user1.cognome < user2.cognome) return -1;
    return 0;
  }
  if (user1.attivo) return -1;
  if (user2.attivo) return 1;
  return 0;
};

const Managers = ({ enqueueSnackbar }) => {
  const classes = useStyles();
  const [users, setUsers] = useState({});
  const [managers, setManagers] = useState({});
  const [loading, setLoading] = useState(false);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [selectedManager, setSelectedManager] = useState();
  const [hideDisabled, setHideDisabled] = useState(true);

  const reload = useCallback(() => {
    setLoading(true);
    apiGetJson('/api/user/list', {})
      .then((userList) => {
        const _users = {};
        userList.forEach((user) => {
          _users[user.idUtente] = user;
        });
        setUsers(_users);
      })
      .catch(() => {
        enqueueSnackbar('Errore nel caricamento della lista degli utenti');
      });
    apiGetJson('/api/ordertype/manager/list', {}).then((mm) => {
      setLoading(false);
      if (mm.error) {
        enqueueSnackbar(mm.errorMessage, { variant: 'error' });
      } else {
        const _managers = {};
        mm.forEach((m) => {
          let list;
          if (_managers[m.userId]) {
            list = _managers[m.userId];
          } else {
            list = [];
            _managers[m.userId] = list;
          }
          list.push(m);
        });
        setManagers(_managers);
      }
    });
  }, [enqueueSnackbar]);

  useEffect(() => {
    reload();
  }, [reload]);

  const editManager = useCallback((user) => {
    setSelectedManager(user);
    setEditDialogOpen(true);
  }, []);

  const rows = useMemo(() => {
    const activeUsers = Object.values(users)
      .filter((user) => (hideDisabled ? user.attivo : true))
      .sort(userSorter);
    return loading ? (
      <LoadingRow colSpan={3} />
    ) : (
      activeUsers.map((user) => {
        const orderTypes = _.sortBy(managers[user.idUtente], (i) =>
          i.orderTypeName.toUpperCase(),
        );
        const sliceSize = Math.max(
          7,
          Number.parseInt(orderTypes.length / 3, 10) + 1,
        );
        return (
          <TableRow key={`user-${user.idUtente}`} hover>
            <TableCell className={classes.tableCell}>
              {user.nome} {user.cognome}{' '}
              {user.attivo ? (
                ''
              ) : (
                <span className={classes.inactiveLabel}>INATTIVO</span>
              )}
            </TableCell>
            <TableCell className={classes.tableCell}>
              {orderTypes.length === 0 ? (
                <span>- nessun ordine assegnato -</span>
              ) : (
                <ul>
                  {orderTypes.slice(0, sliceSize).map((item) => (
                    <li key={item.id}>{item.orderTypeName}</li>
                  ))}
                </ul>
              )}
            </TableCell>
            <TableCell className={classes.tableCell}>
              <ul>
                {orderTypes.slice(sliceSize, sliceSize * 2).map((item) => (
                  <li key={item.id}>{item.orderTypeName}</li>
                ))}
              </ul>
            </TableCell>
            <TableCell className={classes.tableCell}>
              <ul>
                {orderTypes.slice(sliceSize * 2, 100).map((item) => (
                  <li key={item.id}>{item.orderTypeName}</li>
                ))}
              </ul>
            </TableCell>
            <TableCell className={classes.tableCell}>
              <IconButton
                onClick={() => {
                  editManager(user);
                }}
              >
                <EditIcon fontSize='small' />
              </IconButton>
            </TableCell>
          </TableRow>
        );
      })
    );
  }, [classes, managers, editManager, loading, users, hideDisabled]);

  return (
    <>
      <Container maxWidth={false}>
        <PageTitle title='Referenti'>
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

        <TableContainer>
          <Table size='small'>
            <TableHead>
              <TableRow>
                <TableCell className={classes.cellHeader}>Referente</TableCell>
                <TableCell className={classes.cellHeader} colSpan={3}>
                  Ordini
                </TableCell>
                <TableCell className={classes.tdButtons} />
              </TableRow>
            </TableHead>

            <TableBody className={classes.tableBody}>{rows}</TableBody>
          </Table>
        </TableContainer>
      </Container>
      <ManagerEditDialog
        open={editDialogOpen}
        onClose={(forceReload) => {
          setEditDialogOpen(false);
          setSelectedManager();
          if (forceReload) reload();
        }}
        manager={selectedManager}
      />
    </>
  );
};

export default withSnackbar(Managers);
