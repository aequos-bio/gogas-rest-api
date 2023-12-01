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
import moment, { Moment } from 'moment-timezone';
import MomentUtils from '@date-io/moment';
import { makeStyles } from '@material-ui/core/styles';
import { Invoice } from './types';
import { useInvoicesAPI } from './useInvoicesAPI';
import { useAppSelector } from '../../../store/store';
import { MaterialUiPickersDate } from '@material-ui/pickers/typings/date';

const useStyles = makeStyles(theme => ({
  label: {
    fontWeight: 'lighter',
  },
  cancelIcon: {
    marginTop: theme.spacing(4),
  },
}));

interface Props {
  invoice?: Invoice;
  open: boolean;
  onClose: (refresh?: boolean) => void;
}

const PaymentDialog: React.FC<Props> = ({ invoice, open, onClose }) => {
  const classes = useStyles();
  const [paymentDate, setPaymentDate] = useState<string | undefined>(invoice?.paymentDate);
  const accounting = useAppSelector((state) => state.accounting);
  const { payInvoice } = useInvoicesAPI(accounting.currentYear);

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
    if (!paymentDate || !invoice) return;
    payInvoice(invoice, paymentDate).then(() => { onClose(true) });
  }, [onClose, invoice, paymentDate]);

  return (
    <Dialog open={open} onClose={() => onClose()} maxWidth="sm" fullWidth>
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
                onChange={(date: MaterialUiPickersDate) => { setPaymentDate((date as Moment).format('YYYY-MM-DD')) }}
                autoOk
              />
              <IconButton
                className={classes.cancelIcon}
                size="small"
                onClick={() => {
                  setPaymentDate(undefined);
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

export default PaymentDialog;
