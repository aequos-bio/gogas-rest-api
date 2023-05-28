import { TableCell, TableFooter, TableHead, TableRow } from "@material-ui/core";
import { makeStyles } from '@material-ui/core/styles';

const useStyles = makeStyles((theme) => ({
  tdAmount: {
    textAlign: 'right',
    width: '90px',
  },
  footercell: {
    '& td': {
      fontSize: '.875rem',
    },
  },
}));

interface Props {
  admin: boolean;
  totals: {
    accrediti: number;
    addebiti: number;
  }
}

const TableFoot: React.FC<Props> = ({ admin, totals }) => {
  const classes = useStyles();

  return (
    <TableFooter className={classes.footercell}>
      <TableRow>
        <TableCell />
        <TableCell>
          <strong>TOTALE</strong>
        </TableCell>
        <TableCell className={classes.tdAmount}>
          <strong>
            {totals && !Number.isNaN(totals.accrediti)
              ? totals.accrediti.toFixed(2)
              : ''}
          </strong>
        </TableCell>
        <TableCell className={classes.tdAmount}>
          <strong>
            {totals && !Number.isNaN(totals.addebiti)
              ? totals.addebiti.toFixed(2)
              : ''}
          </strong>
        </TableCell>
        <TableCell />
        {admin ? <TableCell /> : null}
      </TableRow>
    </TableFooter>
  );
}

export default TableFoot;