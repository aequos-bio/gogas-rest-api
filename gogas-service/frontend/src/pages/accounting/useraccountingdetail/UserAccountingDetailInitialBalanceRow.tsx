import { TableCell, TableRow } from "@material-ui/core";
import { makeStyles } from '@material-ui/core/styles';

const useStyles = makeStyles((theme) => ({
  tdAmount: {
    textAlign: 'right',
    width: '90px',
  },
}));

interface Props {
  year: number;
  balance: number;
}

const UserAccountingDetailInitialBalanceRow: React.FC<Props> = ({ year, balance }) => {
  const classes = useStyles();

  return (
    <TableRow hover>
      <TableCell align='center'>01/01/{year}</TableCell>
      <TableCell>Saldo iniziale {year}</TableCell>
      <TableCell />
      <TableCell />
      <TableCell className={classes.tdAmount} style={{ color: balance < 0 ? 'red' : 'inherited' }}>
        {balance >= 0 ? '+ ' : ''}
        {balance.toFixed(2)}
      </TableCell>
      <TableCell />
    </TableRow>

  )
}

export default UserAccountingDetailInitialBalanceRow;