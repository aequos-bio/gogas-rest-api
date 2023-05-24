import { IconButton, TableCell, TableRow } from "@material-ui/core";
import {
  EditSharp as EditIcon,
  DeleteSharp as DeleteIcon,
} from '@material-ui/icons';
import { makeStyles } from '@material-ui/core/styles';
import moment from "moment-timezone";
import useJwt from "../../../hooks/JwtHooks";
import { UserTransaction } from "./types";

const useStyles = makeStyles((theme) => ({
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
}));

interface Props {
  transaction: UserTransaction;
  onEditTransaction: (id: string) => void;
  onDeleteTransaction: (id: string) => void;
  yearIsClosed?: boolean;
  admin: boolean;
}

const UserAccountingDetailRow: React.FC<Props> = ({ transaction, onEditTransaction, onDeleteTransaction, yearIsClosed = true, admin = false }) => {
  const classes = useStyles();

  return (
    <TableRow hover>
      <TableCell align='center'>
        {moment(transaction.date).format('DD/MM/YYYY')}
      </TableCell>
      <TableCell>
        {transaction.reason ? `${transaction.reason} - ` : ''}
        {transaction.friend ? `(${transaction.friend}) ` : ''}
        {transaction.description}
      </TableCell>
      <TableCell className={classes.tdAmount}>
        {transaction.sign === '+' || transaction.amount < 0
          ? Math.abs(transaction.amount).toFixed(2)
          : ''}
      </TableCell>
      <TableCell className={classes.tdAmount}>
        {transaction.sign === '-' && transaction.amount >= 0
          ? Math.abs(transaction.amount).toFixed(2)
          : ''}
      </TableCell>
      <TableCell
        className={classes.tdAmount}
        style={{ color: transaction.saldo < 0 ? 'red' : 'inherited' }}
      >
        {transaction.saldo >= 0 ? '+ ' : ''}
        {transaction.saldo.toFixed(2)}
      </TableCell>
      {admin && !yearIsClosed ? (
        <TableCell>
          {transaction.type === 'M' ? (
            <IconButton
              onClick={() => {
                onEditTransaction(transaction.id);
              }}
            >
              <EditIcon fontSize='small' />
            </IconButton>
          ) : null}

          {transaction.type === 'M' ? (
            <IconButton
              onClick={() => {
                onDeleteTransaction(transaction.id);
              }}
            >
              <DeleteIcon fontSize='small' />
            </IconButton>
          ) : null}
        </TableCell>
      ) : <TableCell />}
    </TableRow>
  )
}

export default UserAccountingDetailRow;