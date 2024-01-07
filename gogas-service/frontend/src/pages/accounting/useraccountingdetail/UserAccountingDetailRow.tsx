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
        {moment(transaction.data, 'DD/MM/YYYY').format('DD/MM/YYYY')}
      </TableCell>
      <TableCell>
        {transaction.descrizione}
      </TableCell>
      <TableCell className={classes.tdAmount}>
        {transaction.importo >= 0
          ? Math.abs(transaction.importo).toFixed(2)
          : ''}
      </TableCell>
      <TableCell className={classes.tdAmount}>
        {transaction.importo < 0
          ? Math.abs(transaction.importo).toFixed(2)
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
          {transaction.orderId ? null : (
            <IconButton
              onClick={() => {
                onEditTransaction(transaction.idRiga);
              }}
            >
              <EditIcon fontSize='small' />
            </IconButton>
          )}

          {transaction.orderId ? null : (
            <IconButton
              onClick={() => {
                onDeleteTransaction(transaction.idRiga);
              }}
            >
              <DeleteIcon fontSize='small' />
            </IconButton>
          )}
        </TableCell>
      ) : <TableCell />}
    </TableRow>
  )
}

export default UserAccountingDetailRow;