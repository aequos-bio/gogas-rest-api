import { TableCell, TableHead, TableRow } from "@material-ui/core";
import { makeStyles } from '@material-ui/core/styles';

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

const TableHeader: React.FC = () => {
  const classes = useStyles();

  return (
    <TableHead>
      <TableRow>
        <TableCell>Data</TableCell>
        <TableCell>Numero</TableCell>
        <TableCell>Descrizione</TableCell>
        <TableCell className={classes.cellAmount}>Importo</TableCell>
        <TableCell className={classes.cellPaid}>Pagata</TableCell>
        <TableCell className={classes.cellDate}>Data pagamento</TableCell>
        <TableCell className={classes.cellButtons} />
      </TableRow>
    </TableHead>

  );
}

export default TableHeader;