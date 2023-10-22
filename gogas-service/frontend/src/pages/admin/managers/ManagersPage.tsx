import React, { useEffect, useCallback, useState, useMemo } from 'react';
import {
  Container,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
  Switch,
  FormControlLabel,
} from '@material-ui/core';
import { makeStyles } from '@material-ui/core/styles';
import { sortBy } from 'lodash';
import PageTitle from '../../../components/PageTitle';
import LoadingRow from '../../../components/LoadingRow';
import ManagerEditDialog from './ManagerEditDialog';
import { User } from '../users/types';
import { useUsersAPI } from '../users/useUsersAPI';
import { useManagersAPI } from './useManagersAPI';
import ManagerRow from './ManagerRow';

const useStyles = makeStyles((theme) => ({
  fab: {
    position: 'fixed',
    bottom: theme.spacing(2),
    right: theme.spacing(2),
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
}));

const userSorter = (user1: User, user2: User) => {
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

const Managers: React.FC = () => {
  const classes = useStyles();
  const { users: userList, loading: usersLoading, reload: reloadUsers } = useUsersAPI('NC');
  const { managers, loading: managersLoading, reload: reloadManagers } = useManagersAPI();
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [selectedManager, setSelectedManager] = useState();
  const [hideDisabled, setHideDisabled] = useState(true);
  const loading = usersLoading || managersLoading;

  const users: { [id: string]: User } = {};
  userList.forEach(user => {
    users[user.idUtente] = user;
  })

  const reload = useCallback(() => {
    reloadUsers();
    reloadManagers();
  }, [reloadUsers, reloadManagers]);

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
    return (loading) ? (
      <LoadingRow colSpan={3} />
    ) : (
      activeUsers.map((user) => {
        const orderTypes = sortBy(managers[user.idUtente], (i) =>
          i.orderTypeName.toUpperCase(),
        );
        return (
          <ManagerRow key={`user-${user.idUtente}`} user={user} orderTypes={orderTypes} onEditManager={editManager} />
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

            <TableBody>{rows}</TableBody>
          </Table>
        </TableContainer>
      </Container>

      <ManagerEditDialog
        open={editDialogOpen}
        onClose={(forceReload) => {
          setEditDialogOpen(false);
          setSelectedManager(undefined);
          if (forceReload) reload();
        }}
        manager={selectedManager}
      />
    </>
  );
};

export default Managers;
