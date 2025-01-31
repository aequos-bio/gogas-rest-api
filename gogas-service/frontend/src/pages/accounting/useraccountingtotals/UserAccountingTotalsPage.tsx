import React, { useCallback, useEffect, useState, useMemo } from 'react';
import {
  Container,
  Fab,
  Button,
  TableContainer,
  Table,
  TableHead,
  TableFooter,
  TableRow,
  TableCell,
  TableBody,
} from '@material-ui/core';
import {
  AddSharp as PlusIcon,
  SaveAltSharp as SaveIcon,
} from '@material-ui/icons';
import { makeStyles } from '@material-ui/core/styles';
import EditUserMovementDialog from '../useraccountingdetail/EditUserMovementDialog';
import PageTitle from '../../../components/PageTitle';
import LoadingRow from '../../../components/LoadingRow';
import ExportTypeSelectionDialog from '../components/ExportTypeSelectionDialog';
import { useHistory } from 'react-router';
import UserAccountingTotalRow from './UserAccountingTotalRow';
import { useUserAccountingTotalsAPI } from './useUserAccountingTotalsAPI';

const useStyles = makeStyles((theme) => ({
  fab: {
    position: 'fixed',
    bottom: theme.spacing(2),
    right: theme.spacing(2),
  },
  tdIcon: {
    color: 'red',
    textAlign: 'center',
    width: '30px',
  },
  tdAmount: {
    textAlign: 'right',
    width: '90px',
  },
  tdLink: {
    textAlign: 'center',
    width: '70px',
  },
  footercell: {
    '& td': {
      fontSize: '.875rem',
    },
  },
}));

const UserAccountingTotals = ({ friends }: { friends?: boolean }) => {
  const classes = useStyles();
  const [showDlg, setShowDlg] = useState(false);
  const [exportDlgOpen, setExportDlgOpen] = useState(false);
  const history = useHistory();
  const manageFriends = !!friends;

  const { total, userTotals, loading, reload } = useUserAccountingTotalsAPI(manageFriends);

  const exportXls = useCallback((type) => {
    setExportDlgOpen(false);

    const apiPath = manageFriends ? 'friend' : 'user';

    if (type === 'simple')
      window.open(`/api/accounting/${apiPath}/exportTotals`, '_blank');
    else if (type === 'full')
      window.open(
        `/api/accounting/${apiPath}/exportTotals?includeUsers=true`,
        '_blank',
      );
  }, []);

  const onCloseTransactionDlg = useCallback(
    (refresh) => {
      setShowDlg(false);
      if (refresh) {
        reload();
      }
    },
    [reload],
  );

  useEffect(() => {
    reload();
  }, [reload]);

  const onOpenDetail = useCallback((userId: string) => {
    history.push(manageFriends ? `/friendaccountingdetails?userId=${userId}` : `/useraccountingdetails?userId=${userId}`)
  }, [history])

  return (
    <Container maxWidth={false}>
      <PageTitle title='Situazione contabile utenti'>
        <Button
          onClick={() => setExportDlgOpen(true)}
          variant='outlined'
          startIcon={<SaveIcon />}
        >
          Esporta XLS
        </Button>
      </PageTitle>

      {manageFriends ? <></> : (
        <Fab
          className={classes.fab}
          color='secondary'
          onClick={() => setShowDlg(true)}
        >
          <PlusIcon />
        </Fab>
      )}

      <TableContainer>
        <Table size='small'>
          <TableHead>
            <TableRow>
              <TableCell className={classes.tdIcon} />
              <TableCell>Utente</TableCell>
              <TableCell>Saldo</TableCell>
              <TableCell />
            </TableRow>
          </TableHead>

          <TableBody>
            {loading ? (
              <LoadingRow colSpan={4} />
            ) : (
              userTotals.map((userTotal) => (
                <UserAccountingTotalRow key={`user-${userTotal.idUtente}`} userTotal={userTotal} onOpenDetail={onOpenDetail} />
              ))
            )}
          </TableBody>

          <TableFooter className={classes.footercell}>
            <TableRow>
              <TableCell />
              <TableCell>
                <strong>TOTALE</strong>
              </TableCell>
              <TableCell
                className={classes.tdAmount}
                style={{ color: total < 0 ? 'red' : 'inherited' }}
              >
                <strong>{total.toFixed(2)}</strong>
              </TableCell>
              <TableCell />
            </TableRow>
          </TableFooter>
        </Table>
      </TableContainer>

      <ExportTypeSelectionDialog
        open={exportDlgOpen}
        onCancel={() => setExportDlgOpen(false)}
        onExport={exportXls}
      />
      <EditUserMovementDialog open={showDlg} onClose={onCloseTransactionDlg} />
    </Container>
  );
}

export default UserAccountingTotals;
