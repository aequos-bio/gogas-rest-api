import React, { useCallback, useEffect, useState, useMemo } from 'react';
import {
  Container,
  Fab,
  Button,
  IconButton,
  TableContainer,
  Table,
  TableHead,
  TableFooter,
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
import { useSnackbar } from 'notistack';
import queryString from 'query-string';
import moment from 'moment-timezone';
import { orderBy } from 'lodash';
import { apiGetJson, apiDelete } from '../../../utils/axios_utils';
import EditUserMovementDialog from './EditUserMovementDialog';
import ActionDialog from '../../../components/ActionDialog';
import PageTitle from '../../../components/PageTitle';
import LoadingRow from '../../../components/LoadingRow';
import useJwt from '../../../hooks/JwtHooks';
import { useLocation } from 'react-router-dom';
import UserAccountingDetailRow from './UserAccountingDetailRow';
import UserAccountingDetailInitialBalanceRow from './UserAccountingDetailInitialBalanceRow';
import UserAccountingDetailFinalBalanceRow from './UserAccountingDetailFinalBalanceRow';

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
  tdButtons: {
    minWidth: '90px',
    width: '90px',
  },
  lockicon: {
    fontSize: '.875rem',
    marginLeft: theme.spacing(0.5),
  },
  footercell: {
    '& td': {
      fontSize: '.875rem',
    },
  },
}));

function UserAccountingDetail() {
  const classes = useStyles();
  const location = useLocation();
  const search = queryString.parse(location.search);
  const [user, setUser] = useState({});
  const [transactions, setTransactions] = useState([]);
  const [totals, setTotals] = useState({ accrediti: 0, addebiti: 0 });
  const [showDlg, setShowDlg] = useState(false);
  const [deleteDlgOpen, setDeleteDlgOpen] = useState(false);
  const [selectedId, setSelectedId] = useState();
  const [years, setYears] = useState({});
  const [loading, setLoading] = useState(false);
  const jwt = useJwt();
  const {enqueueSnackbar} = useSnackbar();

  const reload = useCallback(() => {
    setLoading(true);
    apiGetJson(`/api/user/${search.userId}`, {}).then((u) => {
      if (u.error) {
        enqueueSnackbar(u.errorMessage, { variant: 'error' });
      } else setUser(u);
    });

    apiGetJson(
      `/api/useraccounting/userTransactions?userId=${search.userId}`,
      {},
    ).then((t) => {
      setLoading(false);
      if (t.error) {
        enqueueSnackbar(t.errorMessage, { variant: 'error' });
        setTransactions([]);
        setTotals({ accrediti: 0, addebiti: 0 });
      } else {
        const tt = orderBy(t.data, 'date', 'desc');
        let saldo = 0;
        let accrediti = 0;
        let addebiti = 0;
        if (tt.length)
          for (let f = tt.length - 1; f >= 0; f--) {
            const m = tt[f].amount * (tt[f].sign === '-' ? -1 : 1);
            tt[f].saldo = saldo + m;
            saldo = tt[f].saldo;
            if (m < 0) {
              addebiti += -1 * m;
            } else {
              accrediti += m;
            }
          }
        setTransactions(tt);
        setTotals({ accrediti, addebiti });
      }
    });
  }, [search.userId, enqueueSnackbar]);

  const downloadXls = useCallback(() => {
    window.open(
      `/api/useraccounting/exportUserDetails?userId=${user.idUtente}`,
      '_blank',
    );
  }, [user]);

  useEffect(() => {
    apiGetJson('/api/year/all', {}).then((yy) => {
      if (yy.error) {
        enqueueSnackbar(yy.errorMessage, { variant: 'error' });
      } else {
        const _years = {};
        yy.data.forEach((y) => {
          _years[y.year] = y.closed;
        });
        setYears(_years);
      }
    });

    reload();
  }, [reload, enqueueSnackbar]);

  const dialogClosed = useCallback(
    (refresh) => {
      setShowDlg(false);
      if (refresh) reload();
    },
    [setShowDlg, reload],
  );

  const newTransaction = useCallback(() => {
    setSelectedId();
    setShowDlg(true);
  }, []);

  const editTransaction = useCallback((id) => {
    setSelectedId(id);
    setShowDlg(true);
  }, []);

  const deleteTransaction = useCallback((id) => {
    setSelectedId(id);
    setDeleteDlgOpen(true);
  }, []);

  const doDeleteTransaction = useCallback(() => {
    apiDelete(`/api/accounting/user/entry/${selectedId}`)
      .then(() => {
        setDeleteDlgOpen(false);
        reload();
        enqueueSnackbar('Movimento eliminato', { variant: 'success' });
      })
      .catch((err) => {
        enqueueSnackbar(
          err.response?.statusText || "Errore nell'eliminazione del movimento",
          { variant: 'error' },
        );
      });
  }, [enqueueSnackbar, reload, selectedId]);

  const rows = useMemo(() => {
    if (loading) {
      return <LoadingRow colSpan={5} />;
    }
    if (!transactions || !transactions.length) return [];
    const rr = [];
    let lastYear = '';
    let lastYearPlus;
    let lastYearMinus;

    transactions.forEach((t, i) => {
      const year = moment(t.date).format('YYYY');
      if (year !== lastYear) {
        if (lastYearPlus !== undefined || lastYearMinus !== undefined) {
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
              balancePlus={lastYearPlus}
              balanceMinus={lastYearMinus} />
          );
        }
        rr.push(
          <TableRow key={`year-${year}`} hover>
            <TableCell colSpan={jwt.role === 'A' ? '6' : '5'}>
              <strong>Anno {year}</strong>
              {years[year] ? <CloedIcon className={classes.lockicon} /> : null}
            </TableCell>
          </TableRow>,
        );
        lastYearPlus = 0;
        lastYearMinus = 0;
        lastYear = year;
      }
      lastYearPlus += t.sign === '+' ? Math.abs(t.amount) : 0;
      lastYearMinus += t.sign === '-' ? Math.abs(t.amount) : 0;

      rr.push(
        <UserAccountingDetailRow 
          key={`transaction-${i}`} 
          transaction={t} 
          yearIsClosed={years[year]} 
          onEditTransaction={editTransaction} 
          onDeleteTransaction={deleteTransaction}
          admin={jwt.role === 'A'}/>
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
    deleteTransaction,
    editTransaction,
    jwt,
    years,
    loading,
  ]);

  return (
    <Container maxWidth={false}>
      <PageTitle
        title={`Dettaglio situazione contabile di ${user.nome ||
          ''} ${user.cognome || ''}`}
      >
        <Button
          onClick={downloadXls}
          variant='outlined'
          startIcon={<SaveIcon />}
        >
          Esporta XLS
        </Button>
      </PageTitle>

      {jwt && jwt.sub && jwt.role === 'A' ? (
        <Fab className={classes.fab} color='secondary' onClick={newTransaction}>
          <PlusIcon />
        </Fab>
      ) : null}

      <TableContainer>
        <Table size='small'>
          <TableHead>
            <TableRow>
              <TableCell>Data</TableCell>
              <TableCell>Descrizione</TableCell>
              <TableCell align='center'>Accrediti</TableCell>
              <TableCell align='center'>Addebiti</TableCell>
              <TableCell align='center'>Saldo</TableCell>
              {jwt.role === 'A' ? (
                <TableCell className={classes.tdButtons} />
              ) : null}
            </TableRow>
          </TableHead>

          <TableBody>{rows}</TableBody>

          <TableFooter className={classes.footercell}>
            <TableRow>
              <TableCell />
              <TableCell>
                <strong>TOTALE</strong>
              </TableCell>
              <TableCell className={classes.tdAmount}>
                <strong>
                  {totals && !Number.isNaN(totals.accrediti)
                    ? totals.accrediti.toFixed(2)
                    : ''}
                </strong>
              </TableCell>
              <TableCell className={classes.tdAmount}>
                <strong>
                  {totals && !Number.isNaN(totals.addebiti)
                    ? totals.addebiti.toFixed(2)
                    : ''}
                </strong>
              </TableCell>
              <TableCell />
              {jwt.role === 'A' ? <TableCell /> : null}
            </TableRow>
          </TableFooter>
        </Table>
      </TableContainer>

      <EditUserMovementDialog
        user={user}
        open={showDlg}
        onClose={dialogClosed}
        transactionId={selectedId}
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
