import { IconButton, TableCell, TableRow } from "@material-ui/core";
import {
  EditSharp as EditIcon,
  EuroSharp as PayIcon,
} from '@material-ui/icons';
import { makeStyles } from '@material-ui/core/styles';
import moment from "moment-timezone";
import { Invoice } from "./types";

const useStyles = makeStyles((theme) => ({
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
}));

interface Props {
  invoice: Invoice;
  aequosAccountingCode: string;
  onEdit: (invoice: Invoice) => void;
  onPay: (invoice: Invoice) => void;
}

const InvoiceRow: React.FC<Props> = ({ invoice, aequosAccountingCode, onEdit, onPay }) => {
  const classes = useStyles();
  const scaduta =
    !invoice.paid && moment().diff(moment(invoice.invoiceDate), 'days') > 30;

  return (
    <TableRow>
      <TableCell>{moment(invoice.invoiceDate).format('DD/MM/YYYY')}</TableCell>
      <TableCell>{invoice.invoiceNumber}</TableCell>
      <TableCell>{invoice.description}</TableCell>
      <TableCell className={classes.cellAmount}>
        {invoice.invoiceAmount.toFixed(2)}
      </TableCell>
      <TableCell
        className={classes.cellPaid}
        style={{ color: scaduta ? 'red' : 'inherited' }}
      >
        {invoice.paid ? 'PAGATA' : scaduta ? 'SCADUTA' : ''}
      </TableCell>
      <TableCell className={classes.cellDate}>
        {invoice.paymentDate ? moment(invoice.paymentDate).format('DD/MM/YYYY') : ''}
      </TableCell>
      <TableCell className={classes.cellButtons}>
        {invoice.accountingCode === aequosAccountingCode ? null : (
          <IconButton
            onClick={() => onEdit(invoice)}
            size='small'
            title='Modifica'
          >
            <EditIcon fontSize='small' />
          </IconButton>
        )}
        <IconButton
          onClick={() => onPay(invoice)}
          size='small'
          title='Pagamento'
        >
          <PayIcon fontSize='small' />
        </IconButton>
      </TableCell>
    </TableRow>

  )
}

export default InvoiceRow;