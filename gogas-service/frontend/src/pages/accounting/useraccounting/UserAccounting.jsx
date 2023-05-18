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
  ArrowForwardIosSharp as EditIcon,
  BlockSharp as BlockIcon,
  AddSharp as PlusIcon,
  SaveAltSharp as SaveIcon,
} from '@material-ui/icons';
import { makeStyles } from '@material-ui/core/styles';
import { withSnackbar } from 'notistack';
import { orderBy } from 'lodash';
import { apiGetJson } from '../../../utils/axios_utils';
import EditTransactionDialog from '../components/EditTransactionDialog';
import PageTitle from '../../../components/PageTitle';
import LoadingRow from '../../../components/LoadingRow';
import ExportTypeSelectionDialog from '../components/ExportTypeSelectionDialog';

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

function UserAccounting({ history, enqueueSnackbar }) {
  const classes = useStyles();
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [totals, setTotals] = useState([]);
  const [showDlg, setShowDlg] = useState(false);
  const [exportDlgOpen, setExportDlgOpen] = useState(false);

  const reload = useCallback(() => {
    setLoading(true);
    apiGetJson('/api/useraccounting/userTotals', {}).then((tt) => {
      setLoading(false);
      if (tt.error) {
        enqueueSnackbar(tt.errorMessage, { variant: 'error' });
      } else {
        let tot = 0;
        tt.data.forEach((t) => {
          tot += t.total;
        });
        setTotals(tt.data);
        setTotal(tot);
      }
    });
  }, [enqueueSnackbar]);

  const exportXls = useCallback((type) => {
    setExportDlgOpen(false);

    if (type === 'simple')
      window.open('/api/useraccounting/exportUserTotals', '_blank');
    else if (type === 'full')
      window.open(
        '/api/useraccounting/exportUserTotals?includeUsers=true',
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

  const rows = useMemo(() => {
    if (loading) {
      return <LoadingRow colSpan={4} />;
    }

    if (!totals) return null;

    const tt = orderBy(
      totals,
      ['user.enabled', 'user.firstName', 'user.lastName'],
      ['desc', 'asc', 'asc'],
    );
    return tt.map((t) => (
      <TableRow key={`user-${t.user.id}`} hover>
        <TableCell className={classes.tdIcon}>
          {t.user.enabled ? [] : <BlockIcon fontSize='small' />}
        </TableCell>
        <TableCell>{`${t.user.firstName} ${t.user.lastName}`}</TableCell>
        <TableCell
          className={classes.tdAmount}
          style={{ color: t.total < 0 ? 'red' : 'inheried' }}
        >
          {t.total.toFixed(2)}
        </TableCell>
        <TableCell className={classes.tdLink}>
          <IconButton
            onClick={() =>
              history.push(`/useraccountingdetails?userId=${t.user.id}`)
            }
            size='small'
          >
            <EditIcon fontSize='small' />
          </IconButton>
        </TableCell>
      </TableRow>
    ));
  }, [totals, history, classes, loading]);

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

      <Fab
        className={classes.fab}
        color='secondary'
        onClick={() => setShowDlg(true)}
      >
        <PlusIcon />
      </Fab>

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

          <TableBody>{rows}</TableBody>

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
      <EditTransactionDialog open={showDlg} onClose={onCloseTransactionDlg} />
    </Container>
  );
}

export default withSnackbar(UserAccounting);
