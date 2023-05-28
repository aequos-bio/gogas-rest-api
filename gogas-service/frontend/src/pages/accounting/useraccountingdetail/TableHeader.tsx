import { TableCell, TableHead, TableRow } from "@material-ui/core";
import { makeStyles } from '@material-ui/core/styles';

const useStyles = makeStyles((theme) => ({
  header: {
    fontWeight: 700,
  },
  tdButtons: {
    minWidth: '90px',
    width: '90px',
  },
}));

interface Props {
  admin: boolean;
}

const TableHeader: React.FC<Props> = ({ admin }) => {
  const classes = useStyles();

  return (
    <TableHead>
      <TableRow>
        <TableCell className={classes.header}>Data</TableCell>
        <TableCell className={classes.header}>Descrizione</TableCell>
        <TableCell className={classes.header} align='center'>Accrediti</TableCell>
        <TableCell className={classes.header} align='center'>Addebiti</TableCell>
        <TableCell className={classes.header} align='center'>Saldo</TableCell>
        {admin ? (
          <TableCell className={classes.tdButtons} />
        ) : null}
      </TableRow>
    </TableHead>

  );
}

export default TableHeader;