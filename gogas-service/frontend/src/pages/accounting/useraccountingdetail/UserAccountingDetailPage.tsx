import React, { useCallback, useEffect, useState, useMemo } from 'react';
import {
  Container,
  Fab,
  Button,
  TableContainer,
  Table,
  TableRow,
  TableCell,
  TableBody,
} from '@material-ui/core';
import {
  EditSharp as EditIcon,
  DeleteSharp as DeleteIcon,
  AddSharp as PlusIcon,
  SaveAltSharp as SaveIcon,
  LockSharp as CloedIcon,
} from '@material-ui/icons';
import { makeStyles } from '@material-ui/core/styles';
import queryString from 'query-string';
import moment from 'moment-timezone';
import EditUserMovementDialog from './EditUserMovementDialog';
import ActionDialog from '../../../components/ActionDialog';
import PageTitle from '../../../components/PageTitle';
import LoadingRow from '../../../components/LoadingRow';
import useJwt from '../../../hooks/JwtHooks';
import { useLocation } from 'react-router-dom';
import UserAccountingDetailRow from './UserAccountingDetailRow';
import UserAccountingDetailInitialBalanceRow from './UserAccountingDetailInitialBalanceRow';
import UserAccountingDetailFinalBalanceRow from './UserAccountingDetailFinalBalanceRow';
import { useYearsAPI } from '../years/useYearsAPI';
import { useUsersAPI } from '../../admin/users/useUsersAPI';
import { useUserTransactionsAPI } from './useUserTransactionsAPI';
import { User } from '../../admin/users/types';
import TableHeader from './TableHeader';
import TableFoot from './TableFooter';

const useStyles = makeStyles((theme) => ({
  fab: {
    position: 'fixed',
    bottom: theme.spacing(2),
    right: theme.spacing(2),
  },
  tdAmount: {
    textAlign: 'right',
    width: '90px',
  },
  lockicon: {
    fontSize: '.875rem',
    marginLeft: theme.spacing(0.5),
  },
}));

const UserAccountingDetail = ({ friends }: { friends?: boolean }) => {
  const classes = useStyles();
  const location = useLocation();
  const search = queryString.parse(location.search);
  const [user, setUser] = useState<User | undefined>(undefined);
  const [showDlg, setShowDlg] = useState(false);
  const [deleteDlgOpen, setDeleteDlgOpen] = useState(false);
  const [selectedId, setSelectedId] = useState();
  const [years, setYears] = useState<{ [year: number]: boolean }>({});
  const jwt = useJwt();
  const { years: yearList, reload: reloadYears } = useYearsAPI();
  const { getUser } = useUsersAPI("NC");

  const manageFriends = !!friends;
  const { loading, reload: reloadTransactions, transactions, totals, deleteTransaction } = useUserTransactionsAPI(search.userId as string, manageFriends)

  const reload = useCallback(() => {
    if (!search.userId) return;
    getUser(search.userId as string).then(user => setUser(user as User));
    reloadTransactions();

  }, [search.userId]);

  const downloadXls = useCallback(() => {
    if (!user) return;

    const apiPath = manageFriends ? 'friend' : 'user';

    window.open(
      `/api/accounting/${apiPath}/exportDetails?userId=${user.idUtente}`,
      '_blank',
    );
  }, [user]);

  useEffect(() => {
    reloadYears();
    reload();
  }, [reload, reloadYears]);

  useEffect(() => {
    if (!yearList) return;
    const _years: { [year: number]: boolean } = {};
    yearList.forEach((y) => {
      _years[y.year] = y.closed;
    });
    setYears(_years);
  }, [yearList])

  const dialogClosed = useCallback(
    (refresh) => {
      setShowDlg(false);
      if (refresh) reload();
    },
    [setShowDlg, reload],
  );

  const newTransaction = useCallback(() => {
    setSelectedId(undefined);
    setShowDlg(true);
  }, []);

  const editTransaction = useCallback((id) => {
    setSelectedId(id);
    setShowDlg(true);
  }, []);

  const _deleteTransaction = useCallback((id) => {
    setSelectedId(id);
    setDeleteDlgOpen(true);
  }, []);

  const doDeleteTransaction = useCallback(() => {
    if (!selectedId) return;
    deleteTransaction(selectedId).then(() => {
      setDeleteDlgOpen(false);
    });
  }, [selectedId]);

  const rows = useMemo(() => {
    if (loading) {
      return <LoadingRow colSpan={5} />;
    }
    if (!transactions || !transactions.length) return [];
    const rr = [];
    let lastYear = 0;
    let lastYearPlus = 0;
    let lastYearMinus = 0;

    transactions.forEach((t, i) => {
      const year = Number.parseInt(moment(t.data, 'DD/MM/YYYY').format('YYYY'), 10);
      if (year !== lastYear) {
        if (lastYear !== 0) {
          rr.push(
            <UserAccountingDetailInitialBalanceRow
              key={`initialamt-${lastYear}`}
              year={lastYear}
              balance={t.saldo} />
          );

          rr.push(
            <UserAccountingDetailFinalBalanceRow
              key={`totals-${lastYear}`}
              year={lastYear}
              balancePlus={lastYearPlus as number | 0}
              balanceMinus={lastYearMinus as number | 0} />
          );
        }

        rr.push(
          <TableRow key={`year-${year}`} hover>
            <TableCell colSpan={jwt?.role === 'A' ? 6 : 5}>
              <strong>Anno {year}</strong>
              {years[year] ? <CloedIcon className={classes.lockicon} /> : null}
            </TableCell>
          </TableRow>,
        );
        lastYearPlus = 0;
        lastYearMinus = 0;
        lastYear = year;
      }
      lastYearPlus += t.importo >= 0 ? Math.abs(t.importo) : 0;
      lastYearMinus += t.importo < 0 ? Math.abs(t.importo) : 0;

      rr.push(
        <UserAccountingDetailRow
          key={`transaction-${i}`}
          transaction={t}
          yearIsClosed={years[year]}
          onEditTransaction={editTransaction}
          onDeleteTransaction={_deleteTransaction}
          admin={jwt?.role === 'A' || manageFriends} />
      );
    });

    rr.push(
      <UserAccountingDetailInitialBalanceRow
        key={`initialamt-${lastYear}`}
        year={lastYear}
        balance={0} />
    );

    rr.push(
      <UserAccountingDetailFinalBalanceRow
        key={`totals-${lastYear}`}
        year={lastYear}
        balancePlus={lastYearPlus}
        balanceMinus={lastYearMinus} />
    );

    return rr;
  }, [
    transactions,
    classes,
    _deleteTransaction,
    editTransaction,
    jwt,
    years,
    loading,
  ]);

  return (
    <Container maxWidth={false}>
      <PageTitle
        title={`Dettaglio situazione contabile di ${user?.nome ||
          ''} ${user?.cognome || ''}`}
      >
        <Button
          onClick={downloadXls}
          variant='outlined'
          startIcon={<SaveIcon />}
        >
          Esporta XLS
        </Button>
      </PageTitle>

      {(jwt && jwt.sub && jwt.role === 'A') || manageFriends ? (
        <Fab className={classes.fab} color='secondary' onClick={newTransaction}>
          <PlusIcon />
        </Fab>
      ) : null}

      <TableContainer>
        <Table size='small'>
          <TableHeader admin={jwt?.role === 'A'} />
          <TableBody>{rows}</TableBody>
          <TableFoot admin={jwt?.role === 'A'} totals={totals} />
        </Table>
      </TableContainer>

      <EditUserMovementDialog
        user={user}
        open={showDlg}
        onClose={dialogClosed}
        transactionId={selectedId}
        friends={manageFriends}
      />
      <ActionDialog
        open={deleteDlgOpen}
        onCancel={() => setDeleteDlgOpen(false)}
        actions={['Ok']}
        onAction={doDeleteTransaction}
        title='Conferma eliminazione'
        message='Sei sicuro di voler eliminare il movimento selezionato?'
      />
    </Container>
  );
}

export default UserAccountingDetail;
