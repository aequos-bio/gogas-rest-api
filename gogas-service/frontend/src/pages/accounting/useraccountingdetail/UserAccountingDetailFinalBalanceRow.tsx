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
  balancePlus: number;
  balanceMinus: number;
}

const UserAccountingDetailFinalBalanceRow: React.FC<Props> = ({ year, balancePlus, balanceMinus }) => {
  const classes = useStyles();

  return (
    <TableRow hover>
      <TableCell colSpan={2} align='right'>
        <strong>Totale anno {year}</strong>
      </TableCell>
      <TableCell className={classes.tdAmount}>
        <strong>{Math.abs(balancePlus).toFixed(2)}</strong>
      </TableCell>
      <TableCell className={classes.tdAmount}>
        <strong>{Math.abs(balanceMinus).toFixed(2)}</strong>
      </TableCell>
      <TableCell />
      <TableCell />
    </TableRow>

  )
}

export default UserAccountingDetailFinalBalanceRow;