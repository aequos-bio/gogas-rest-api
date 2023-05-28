/* eslint-disable no-nested-ternary */
import { useState, useCallback, useEffect } from 'react';
import {
  Container,
  Fab,
  Button,
  TableContainer,
  Table,
  TableBody,
} from '@material-ui/core';
import { AddSharp as PlusIcon } from '@material-ui/icons';
import { makeStyles } from '@material-ui/core/styles';
import PageTitle from '../../../components/PageTitle';
import LoadingRow from '../../../components/LoadingRow';
import PaymentDialog from './PaymentDialog';
import InvoiceDialog from './InvoiceDialog';
import InvoiceRow from './InvoiceRow';
import { useAccountingCodesAPI } from '../accountingcodes/useAccountingCodesAPI';
import { useInvoicesAPI } from './useInvoicesAPI';
import { useAppSelector } from '../../../store/store';
import { Invoice } from './types';
import TableHeader from './TableHeader';

const useStyles = makeStyles((theme) => ({
  tableContainer: {
    marginBottom: theme.spacing(2),
  },
  fab: {
    position: 'fixed',
    bottom: theme.spacing(2),
    right: theme.spacing(2),
    zIndex: 1000,
  },
}));

const InvoiceManagement = () => {
  const classes = useStyles();
  const [selectedInvoice, setSelectedInvoice] = useState<Invoice | undefined>(undefined);
  const [showPayDlg, setShowPayDlg] = useState(false);
  const [showInvoiceDlg, setShowInvoiceDlg] = useState(false);
  const accounting = useAppSelector((state) => state.accounting);
  const { aequosAccountingCode, reload: reloadAccountingCodes, loading: loadingAccountingCodes } = useAccountingCodesAPI();
  const { loading: loadingInvoices, invoices, reload: reloadInvoices, syncWithOrders } = useInvoicesAPI(accounting.currentYear);

  useEffect(() => {
    reloadAccountingCodes();
    reloadInvoices();
  }, [reloadAccountingCodes, reloadInvoices]);

  const onPayInvoice = (invoice: Invoice) => {
    setSelectedInvoice(invoice);
    setShowPayDlg(true);
  };

  const onPayDialogClosed = useCallback(
    (needrefresh) => {
      setShowPayDlg(false);
      setSelectedInvoice(undefined);
      if (needrefresh) {
        reloadInvoices();
      }
    },
    [reloadInvoices],
  );

  const onNewInvoice = () => {
    setSelectedInvoice(undefined);
    setShowInvoiceDlg(true);
  };

  const onEditInvoice = (invoice: Invoice) => {
    setSelectedInvoice(invoice);
    setShowInvoiceDlg(true);
  };

  const onInvoiceDialogClosed = useCallback(
    (needRefresh) => {
      setShowInvoiceDlg(false);
      setSelectedInvoice(undefined);
      if (needRefresh) {
        reloadInvoices();
      }
    },
    [reloadInvoices],
  );

  const syncOrders = useCallback(() => {
    syncWithOrders();
  }, [syncWithOrders]);

  return (
    <Container maxWidth={false}>
      <PageTitle
        title={`Gestione fatture fornitori - ${accounting.currentYear}`}
      >
        <Button type='button' variant='outlined' onClick={reloadInvoices}>
          Ricarica
        </Button>
        <Button type='button' variant='outlined' onClick={syncOrders}>
          Sincronizza fatture Aequos
        </Button>
      </PageTitle>

      <Fab className={classes.fab} color='secondary' onClick={onNewInvoice}>
        <PlusIcon />
      </Fab>
      <TableContainer className={classes.tableContainer}>
        <Table size='small'>
          <TableHeader />
          <TableBody>
            {loadingAccountingCodes || loadingInvoices ? (
              <LoadingRow colSpan={7} />
            ) : (
              invoices.map((invoice, j) => (
                <InvoiceRow
                  key={`invoice-${invoice.accountingCode}-${invoice.invoiceNumber}-${j}`}
                  invoice={invoice}
                  aequosAccountingCode={aequosAccountingCode}
                  onEdit={onEditInvoice}
                  onPay={onPayInvoice}
                />
              ))
            )}
          </TableBody>
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

export default InvoiceManagement;
