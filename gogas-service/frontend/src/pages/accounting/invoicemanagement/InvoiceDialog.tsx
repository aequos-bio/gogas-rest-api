import React, { useState, useEffect, useMemo, useCallback } from 'react';
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
import moment, { Moment } from 'moment-timezone';
import MomentUtils from '@date-io/moment';
import { makeStyles } from '@material-ui/core/styles';
import { Invoice } from './types';
import { useAppSelector } from '../../../store/store';
import { useInvoicesAPI } from './useInvoicesAPI';
import { MaterialUiPickersDate } from '@material-ui/pickers/typings/date';

const useStyles = makeStyles((theme) => ({
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
    backgroundColor: theme.palette.secondary.light,
  },
}));

interface Props {
  invoice?: Invoice;
  open: boolean;
  onClose: (refresh?: boolean) => void;
}

const InvoiceDialog: React.FC<Props> = ({ open, onClose, invoice }) => {
  const classes = useStyles();
  const [date, setDate] = useState<string | undefined>();
  const [number, setNumber] = useState<string | undefined>();
  const [amount, setAmount] = useState<number | undefined>();
  const [orderIds, setOrderIds] = useState<string[]>([]);
  const [paymentDate, setPaymentDate] = useState<string | undefined>();
  const accounting = useAppSelector((state) => state.accounting);
  const { saveInvoice, ordersWithoutInvoice, reloadOrdersWithoutInvoice } = useInvoicesAPI(accounting.currentYear);

  useEffect(() => {
    if (open) reloadOrdersWithoutInvoice();

    if (invoice) {
      setNumber(invoice.invoiceNumber);
      setDate(invoice.invoiceDate);
      setAmount(invoice.invoiceAmount);
      setOrderIds(invoice.orderIds);
      setPaymentDate(invoice.paymentDate);
    } else {
      setNumber(undefined);
      setDate(undefined);
      setAmount(undefined);
      setOrderIds([]);
      setPaymentDate(undefined);
    }
  }, [invoice, open, accounting]);

  const canSave = useMemo(() => {
    return date && number && amount && orderIds.length;
  }, [date, number, amount, orderIds]);

  const save = useCallback(() => {
    saveInvoice(number as string, date as string, amount as number, orderIds, paymentDate as string).then(() => onClose(true));
  }, [number, date, amount, orderIds, paymentDate, onClose]);

  const cancel = useCallback(() => {
    onClose();
  }, [onClose]);

  const selectOrder = useCallback((id) => {
    const ids = [];
    ids.push(id);
    setOrderIds(ids);
  }, []);

  return (
    <Dialog open={open} onClose={cancel} maxWidth='sm' fullWidth>
      <DialogTitle>{invoice ? 'Modifica' : 'Nuova'} fattura</DialogTitle>

      <DialogContent>
        <Grid container spacing={1}>
          <Grid item xs={12}>
            <MuiPickersUtilsProvider
              libInstance={moment}
              utils={MomentUtils}
              locale='it'
            >
              <DatePicker
                disableToolbar
                variant='inline'
                format='DD/MM/YYYY'
                margin='dense'
                id='date-picker-inline'
                label='Data'
                value={date ? moment(date) : null}
                onChange={(date: MaterialUiPickersDate) => { setDate((date as Moment).format('YYYY-MM-DD')) }}
                autoOk
                inputVariant='outlined'
              />
            </MuiPickersUtilsProvider>
          </Grid>

          <Grid item xs={12}>
            <TextField
              label='Numero'
              value={number}
              size='small'
              onChange={(evt) => setNumber(evt.target.value)}
              variant='outlined'
            />
          </Grid>

          <Grid item xs={12}>
            <TextField
              label='Importo'
              type='number'
              size='small'
              InputProps={{
                endAdornment: (
                  <InputAdornment position='end'>
                    <EuroIcon />
                  </InputAdornment>
                ),
              }}
              value={amount}
              onChange={(evt) => setAmount(evt.target.value as unknown as number)}
              variant='outlined'
            />
          </Grid>
          <Grid item xs={12}>
            <div className={classes.tableTitle}>Ordine abbinato</div>
            {invoice ? (
              <div>{invoice.description}</div>
            ) : (
              <TableContainer className={classes.tableContainer}>
                <Table>
                  <TableBody>
                    {ordersWithoutInvoice.map((order) => (
                      <TableRow
                        key={`order-${order.id}`}
                        className={orderIds.includes(order.id) ? classes.tableRowSelected : ''}
                        onClick={() => selectOrder(order.id)}
                      >
                        <TableCell>{order.dataconsegna}</TableCell>
                        <TableCell>{order.tipoordine}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
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

export default InvoiceDialog;
