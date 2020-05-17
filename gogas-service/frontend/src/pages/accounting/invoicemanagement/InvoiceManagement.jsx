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
import { apiGetJson } from '../../../utils/axios_utils';
import PageTitle from '../../../components/PageTitle';
import LoadingRow from '../../../components/LoadingRow';
import PaymentDialog from './PaymentDialog';

const useStyles = makeStyles(theme => ({
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
  },
}));

const InvoiceManagement = ({ enqueueSnackbar }) => {
  const classes = useStyles();
  const [year, setYear] = useState(moment().format('YYYY'));
  const [loading, setLoading] = useState(false);
  const [entries, setEntries] = useState([]);
  const [showDlg, setShowDlg] = useState(false);
  const [selectedInvoice, setSelectedInvoice] = useState();
  const [showPayDlg, setShowPayDlg] = useState(false);

  const reload = useCallback(() => {
    setLoading(true);
    apiGetJson(`/api/accounting/gas/invoices/${year}`, {}).then(tt => {
      setLoading(false);
      if (tt.error) {
        enqueueSnackbar(tt.errorMessage, { variant: 'error' });
      } else {
        setEntries(_.orderBy(tt, ['invoiceDate']));
      }
    });
  }, [enqueueSnackbar, year]);

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

  const rows = useMemo(() => {
    return loading ? (
      <LoadingRow colSpan={7} />
    ) : (
      entries.map(e => {
        const scaduta =
          !e.paid && moment().diff(moment(e.invoiceDate), 'days') > 30;
        return (
          <TableRow key={`invoice-${e.accountingCode}-${e.invoiceNumber}`}>
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
              <IconButton
                onClick={() => {
                  enqueueSnackbar('Non implementato', { variant: 'error' });
                }}
                size="small"
                title="Modifica"
              >
                <EditIcon fontSize="small" />
              </IconButton>
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
  }, [entries, loading, enqueueSnackbar, classes]);

  return (
    <Container maxWidth={false}>
      <PageTitle title="Gestione fatture fornitori" />
      <Fab
        className={classes.fab}
        color="secondary"
        onClick={() => {
          enqueueSnackbar('Non implementato', { variant: 'error' });
        }}
      >
        <PlusIcon />
      </Fab>
      <TableContainer>
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
    </Container>
  );
};

const mapStateToProps = state => {
  return {
    authentication: state.authentication,
  };
};

const mapDispatchToProps = {};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(withSnackbar(InvoiceManagement));
