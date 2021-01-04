/* eslint-disable react/no-array-index-key */
/* eslint-disable no-nested-ternary */
import React, { useState, useCallback, useEffect, useMemo } from 'react';
import { connect } from 'react-redux';
import {
  Container,
  Fab,
  Button,
  IconButton,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
} from '@material-ui/core';
import {
  AddSharp as PlusIcon,
  EditSharp as EditIcon,
  EuroSharp as PayIcon,
} from '@material-ui/icons';
import { makeStyles } from '@material-ui/core/styles';
import { withSnackbar } from 'notistack';
import _ from 'lodash';
import moment from 'moment-timezone';
import { apiGetJson, apiGet } from '../../../utils/axios_utils';
import PageTitle from '../../../components/PageTitle';
import LoadingRow from '../../../components/LoadingRow';
import PaymentDialog from './PaymentDialog';
import InvoiceDialog from './InvoiceDialog';

const useStyles = makeStyles(theme => ({
  tableContainer: {
    marginBottom: theme.spacing(2),
  },
  cellAmount: {
    textAlign: 'right',
  },
  cellPaid: {
    textAlign: 'center',
  },
  cellDate: {
    textAlign: 'right',
  },
  cellButtons: {
    minWidth: '90px',
    width: '90px',
    textAlign: 'right',
  },
  fab: {
    position: 'fixed',
    bottom: theme.spacing(2),
    right: theme.spacing(2),
    zIndex: 1000,
  },
}));

const InvoiceManagement = ({ enqueueSnackbar, accounting }) => {
  const classes = useStyles();
  // eslint-disable-next-line no-unused-vars
  const [aequosAccountingCode, setAequosAccountingCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [entries, setEntries] = useState([]);
  const [selectedInvoice, setSelectedInvoice] = useState();
  const [showPayDlg, setShowPayDlg] = useState(false);
  const [showInvoiceDlg, setShowInvoiceDlg] = useState(false);

  const reload = useCallback(() => {
    setLoading(true);
    apiGetJson(`/api/ordertype/accounting`, {}).then(accountingCodes => {
      const aequos = accountingCodes.filter(a => a.id === 'aequos');
      if (aequos.length) {
        setAequosAccountingCode(aequos[0].accountingCode);
      }
    });

    apiGetJson(
      `/api/accounting/gas/invoices/${accounting.currentYear}`,
      {}
    ).then(tt => {
      setLoading(false);
      if (tt.error) {
        enqueueSnackbar(tt.errorMessage, { variant: 'error' });
      } else {
        setEntries(_.orderBy(tt, ['invoiceDate']));
      }
    });
  }, [enqueueSnackbar, accounting]);

  useEffect(() => {
    reload();
  }, [reload]);

  const onPayInvoice = invoice => {
    setSelectedInvoice(invoice);
    setShowPayDlg(true);
  };

  const onPayDialogClosed = useCallback(
    needrefresh => {
      setShowPayDlg(false);
      setSelectedInvoice();
      if (needrefresh) {
        reload();
      }
    },
    [reload]
  );

  const onNewInvoice = () => {
    setSelectedInvoice();
    setShowInvoiceDlg(true);
  };

  const onEditInvoice = invoice => {
    setSelectedInvoice(invoice);
    setShowInvoiceDlg(true);
  };

  const onInvoiceDialogClosed = useCallback(
    needRefresh => {
      setShowInvoiceDlg(false);
      setSelectedInvoice();
      if (needRefresh) {
        reload();
      }
    },
    [reload]
  );

  const syncOrders = useCallback(() => {
    apiGet(
      `api/accounting/gas/syncAequosOrdersWithoutInvoice/${accounting.currentYear}`
    )
      .then(response => {
        reload();
        enqueueSnackbar(
          `Sincronizzazione completata con successo, ${response.data.data} ordini aggiunti`,
          {
            variant: 'success',
          }
        );
      })
      .catch(err => {
        enqueueSnackbar(`Errore di sincronizzazione: ${err}`, {
          variant: 'error',
        });
      });
  }, [enqueueSnackbar, accounting, reload]);

  const rows = useMemo(() => {
    return loading ? (
      <LoadingRow colSpan={7} />
    ) : (
      entries.map((e, j) => {
        const scaduta =
          !e.paid && moment().diff(moment(e.invoiceDate), 'days') > 30;
        return (
          <TableRow key={`invoice-${e.accountingCode}-${e.invoiceNumber}-${j}`}>
            <TableCell>{moment(e.invoiceDate).format('DD/MM/YYYY')}</TableCell>
            <TableCell>{e.invoiceNumber}</TableCell>
            <TableCell>{e.description}</TableCell>
            <TableCell className={classes.cellAmount}>
              {e.invoiceAmount.toFixed(2)}
            </TableCell>
            <TableCell
              className={classes.cellPaid}
              style={{ color: scaduta ? 'red' : 'inherited' }}
            >
              {e.paid ? 'PAGATA' : scaduta ? 'SCADUTA' : ''}
            </TableCell>
            <TableCell className={classes.cellDate}>
              {e.paymentDate ? moment(e.paymentDate).format('DD/MM/YYYY') : ''}
            </TableCell>
            <TableCell className={classes.cellButtons}>
              {e.accountingCode === aequosAccountingCode ? null : (
                <IconButton
                  onClick={() => onEditInvoice(e)}
                  size="small"
                  title="Modifica"
                >
                  <EditIcon fontSize="small" />
                </IconButton>
              )}
              <IconButton
                onClick={() => onPayInvoice(e)}
                size="small"
                title="Pagamento"
              >
                <PayIcon fontSize="small" />
              </IconButton>
            </TableCell>
          </TableRow>
        );
      })
    );
  }, [entries, loading, classes, aequosAccountingCode]);

  return (
    <Container maxWidth={false}>
      <PageTitle
        title={`Gestione fatture fornitori - ${accounting.currentYear}`}
      >
        <Button type="button" variant="outlined" onClick={syncOrders}>
          Sincronizza fatture Aequos
        </Button>
      </PageTitle>

      <Fab className={classes.fab} color="secondary" onClick={onNewInvoice}>
        <PlusIcon />
      </Fab>
      <TableContainer className={classes.tableContainer}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Data</TableCell>
              <TableCell>Numero</TableCell>
              <TableCell>Descrizione</TableCell>
              <TableCell className={classes.cellAmount}>Importo</TableCell>
              <TableCell className={classes.cellPaid}>Pagata</TableCell>
              <TableCell className={classes.cellDate}>Data pagamento</TableCell>
              <TableCell className={classes.cellButtons} />
            </TableRow>
          </TableHead>

          <TableBody>{rows}</TableBody>
        </Table>
      </TableContainer>

      <PaymentDialog
        invoice={selectedInvoice}
        open={showPayDlg}
        onClose={onPayDialogClosed}
      />

      <InvoiceDialog
        invoice={selectedInvoice}
        open={showInvoiceDlg}
        onClose={onInvoiceDialogClosed}
      />
    </Container>
  );
};

const mapStateToProps = state => {
  return {
    accounting: state.accounting,
  };
};

const mapDispatchToProps = {};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(withSnackbar(InvoiceManagement));
