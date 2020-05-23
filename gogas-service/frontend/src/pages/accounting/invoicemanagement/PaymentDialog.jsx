import React, { useState, useEffect, useMemo, useCallback } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  IconButton,
  Grid,
} from '@material-ui/core';
import { ClearSharp as ClearIcon } from '@material-ui/icons';
import { MuiPickersUtilsProvider, DatePicker } from '@material-ui/pickers';
import moment from 'moment-timezone';
import MomentUtils from '@date-io/moment';
import { withSnackbar } from 'notistack';
import { makeStyles } from '@material-ui/core/styles';
import { apiPost } from '../../../utils/axios_utils';

const useStyles = makeStyles(theme => ({
  label: {
    fontWeight: 'lighter',
  },
  cancelIcon: {
    marginTop: theme.spacing(4),
  },
}));

const PaymentDialog = ({ invoice, open, onClose, enqueueSnackbar }) => {
  const classes = useStyles();
  const [paymentDate, setPaymentDate] = useState(invoice?.paymentDate);

  useEffect(() => {
    if (invoice) setPaymentDate(invoice.paymentDate);
  }, [invoice]);

  const canSave = useMemo(() => {
    if (!invoice || !invoice.paymentDate) {
      return paymentDate !== undefined && paymentDate !== null;
    }
    return true;
  }, [paymentDate, invoice]);

  const doPayment = useCallback(() => {
    const thenFn = () => {
      enqueueSnackbar(`Pagamento fattura salvato con successo`, {
        variant: 'success',
      });
      onClose(true);
    };

    const catchFn = err => {
      enqueueSnackbar(
        err.response?.statusText || 'Errore nel salvataggio del pagamento',
        { variant: 'error' }
      );
    };

    const promises = [];
    invoice.orderIds.forEach(orderId => {
      const params = {
        idDataOrdine: orderId,
        numeroFattura: invoice.invoiceNumber,
        importoFattura: invoice.invoiceAmount,
        dataFattura: moment(invoice.invoiceDate).format('DD/MM/YYYY'),
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
  }, [onClose, invoice, enqueueSnackbar, paymentDate]);

  return (
    <Dialog open={open} onClose={onClose} size="sm" fullWidth>
      <DialogTitle>Pagamento fattura </DialogTitle>

      <DialogContent>
        <Grid container spacing={1}>
          <Grid item xs={4} className={classes.label}>
            Fattura n.
          </Grid>
          <Grid item xs={8}>
            {invoice?.invoiceNumber}
          </Grid>
          <Grid item xs={4} className={classes.label}>
            Del
          </Grid>
          <Grid item xs={8}>
            {invoice?.invoiceDate}
          </Grid>
          <Grid item xs={4} className={classes.label}>
            Descrizione
          </Grid>
          <Grid item xs={8}>
            {invoice?.description}
          </Grid>
          <Grid item xs={4} className={classes.label} />
          <Grid item xs={8}>
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
                label="Data di pagamento"
                value={paymentDate ? moment(paymentDate) : null}
                onChange={setPaymentDate}
                autoOk
              />
              <IconButton
                className={classes.cancelIcon}
                size="small"
                onClick={() => {
                  setPaymentDate();
                }}
              >
                <ClearIcon />
              </IconButton>
            </MuiPickersUtilsProvider>
          </Grid>
        </Grid>
      </DialogContent>

      <DialogActions>
        <Button
          onClick={() => {
            onClose();
          }}
        >
          Annulla
        </Button>
        <Button onClick={doPayment} disabled={!canSave}>
          Salva
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default withSnackbar(PaymentDialog);
