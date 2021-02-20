import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { connect } from 'react-redux';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Grid,
  InputAdornment,
  TableContainer,
  Table,
  TableBody,
  TableRow,
  TableCell,
} from '@material-ui/core';
import { EuroSharp as EuroIcon } from '@material-ui/icons';
import { MuiPickersUtilsProvider, DatePicker } from '@material-ui/pickers';
import moment from 'moment-timezone';
import _ from 'lodash';
import MomentUtils from '@date-io/moment';
import { withSnackbar } from 'notistack';
import { makeStyles } from '@material-ui/core/styles';
import { apiGet, apiPost } from '../../../utils/axios_utils';

const useStyles = makeStyles(theme => ({
  label: {
    fontWeight: 'lighter',
  },
  tableTitle: {
    margin: theme.spacing(2, 0, 1),
    color: theme.palette.grey[600],
  },
  tableContainer: {
    border: `1px solid ${theme.palette.grey[300]}`,
    height: '300px',
  },
  tableRowSelected: {
    backgroundColor: theme.palette.secondary[300],
  },
}));

const InvoiceDialog = ({
  open,
  onClose,
  invoice,
  enqueueSnackbar,
  accounting,
}) => {
  const classes = useStyles();
  const [orders, setOrders] = useState([]);
  const [date, setDate] = useState();
  const [number, setNumber] = useState();
  const [amount, setAmount] = useState();
  const [orderIds, setOrderIds] = useState([]);
  const [paymentDate, setPaymentDate] = useState();

  useEffect(() => {
    if (open) {
      apiGet(
        `api/accounting/gas/ordersWithoutInvoice/${accounting.currentYear}`
      ).then(oo => {
        setOrders(
          _.orderBy(
            oo.data,
            [o => moment(o.dataconsegna, 'DD/MM/YYYY').toISOString()],
            ['desc']
          )
        );
      });
    }
    if (invoice) {
      setNumber(invoice.invoiceNumber);
      setDate(invoice.invoiceDate);
      setAmount(invoice.invoiceAmount);
      setOrderIds(invoice.orderIds);
      setPaymentDate(invoice.paymentDate);
    } else {
      setNumber();
      setDate();
      setAmount();
      setOrderIds([]);
      setPaymentDate();
    }
  }, [invoice, open, accounting]);

  const canSave = useMemo(() => {
    return date && number && amount && orderIds.length;
  }, [date, number, amount, orderIds]);

  const save = useCallback(() => {
    const thenFn = () => {
      enqueueSnackbar(`Fattura salvata con successo`, {
        variant: 'success',
      });
      onClose(true);
    };

    const catchFn = err => {
      enqueueSnackbar(
        err.response?.statusText || 'Errore nel salvataggio della fattura',
        { variant: 'error' }
      );
    };

    const promises = [];
    orderIds.forEach(orderId => {
      const params = {
        idDataOrdine: orderId,
        numeroFattura: number,
        importoFattura: amount,
        dataFattura: moment(date).format('DD/MM/YYYY'),
        dataPagamento: paymentDate
          ? moment(paymentDate).format('DD/MM/YYYY')
          : null,
        pagato: paymentDate !== undefined && paymentDate !== null,
      };
      promises.push(
        apiPost(`/api/order/manage/${orderId}/invoice/data`, params)
      );
    });

    Promise.all(promises)
      .then(thenFn)
      .catch(catchFn);
  }, [number, date, amount, orderIds, paymentDate, onClose, enqueueSnackbar]);

  const cancel = useCallback(() => {
    onClose();
  }, [onClose]);

  const selectOrder = useCallback(id => {
    const ids = [];
    ids.push(id);
    setOrderIds(ids);
  }, []);

  const rows = useMemo(() => {
    return orders.map(o => (
      <TableRow
        key={`order-${o.id}`}
        className={orderIds.includes(o.id) ? classes.tableRowSelected : ''}
        onClick={() => selectOrder(o.id)}
      >
        <TableCell>{o.dataconsegna}</TableCell>
        <TableCell>{o.tipoordine}</TableCell>
      </TableRow>
    ));
  }, [orders, orderIds, classes, selectOrder]);

  return (
    <Dialog open={open} onClose={cancel} maxWidth="sm" fullWidth>
      <DialogTitle>{invoice ? 'Modifica' : 'Nuova'} fattura</DialogTitle>

      <DialogContent>
        <Grid container spacing={1}>
          <Grid item xs={12}>
            <MuiPickersUtilsProvider
              libInstance={moment}
              utils={MomentUtils}
              locale="it"
            >
              <DatePicker
                disableToolbar
                variant="inline"
                format="DD/MM/YYYY"
                margin="dense"
                id="date-picker-inline"
                label="Data"
                value={date ? moment(date) : null}
                onChange={setDate}
                autoOk
                inputVariant="outlined"
              />
            </MuiPickersUtilsProvider>
          </Grid>

          <Grid item xs={12}>
            <TextField
              label="Numero"
              value={number}
              size="small"
              onChange={evt => setNumber(evt.target.value)}
            />
          </Grid>

          <Grid item xs={12}>
            <TextField
              className={classes.field}
              label="Importo"
              type="number"
              size="small"
              InputProps={{
                endAdornment: (
                  <InputAdornment position="end">
                    <EuroIcon className={classes.icon} />
                  </InputAdornment>
                ),
              }}
              value={amount}
              onChange={evt => setAmount(evt.target.value)}
            />
          </Grid>
          <Grid item xs={12}>
            <div className={classes.tableTitle}>Ordine abbinato</div>
            {invoice ? (
              <div>{invoice.description}</div>
            ) : (
              <TableContainer className={classes.tableContainer}>
                <Table>
                  <TableBody>{rows}</TableBody>
                </Table>
              </TableContainer>
            )}
          </Grid>
        </Grid>
      </DialogContent>

      <DialogActions>
        <Button onClick={cancel}>Annulla</Button>
        <Button onClick={save} disabled={!canSave}>
          Salva
        </Button>
      </DialogActions>
    </Dialog>
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
)(withSnackbar(InvoiceDialog));
