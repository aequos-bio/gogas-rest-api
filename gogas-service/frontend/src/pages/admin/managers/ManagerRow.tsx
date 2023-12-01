import { IconButton, TableCell, TableRow } from "@material-ui/core";
import { makeStyles } from '@material-ui/core/styles';
import { OrderTypeManager } from "./types";
import { User } from "../users/types";
import { Edit } from "@material-ui/icons";

const useStyles = makeStyles((theme) => ({
  tableCell: {
    paddingLeft: theme.spacing(1),
    paddingRight: theme.spacing(1),
    verticalAlign: 'top',
    width: '25%',
  },
  inactiveLabel: {
    fontSize: '80%',
    marginLeft: theme.spacing(2),
    padding: theme.spacing(0, 0.5),
    borderRadius: '3px',
    color: theme.palette.getContrastText(theme.palette.error.main),
    backgroundColor: theme.palette.error.main,
  },
}));

interface Props {
  user: User;
  orderTypes: OrderTypeManager[];
  onEditManager: (user: User) => void;
}

const ManagerRow: React.FC<Props> = ({ user, orderTypes, onEditManager }) => {
  const classes = useStyles();
  const sliceSize = Math.max(7, orderTypes.length / 3 + 1);

  return (
    <TableRow hover>
      <TableCell className={classes.tableCell}>
        {user.nome} {user.cognome}{' '}
        {user.attivo ? (
          ''
        ) : (
          <span className={classes.inactiveLabel}>INATTIVO</span>
        )}
      </TableCell>
      <TableCell className={classes.tableCell}>
        {orderTypes.length === 0 ? (
          <span>- nessun ordine assegnato -</span>
        ) : (
          <ul>
            {orderTypes.slice(0, sliceSize).map((item) => (
              <li key={item.id}>{item.orderTypeName}</li>
            ))}
          </ul>
        )}
      </TableCell>
      <TableCell className={classes.tableCell}>
        <ul>
          {orderTypes.slice(sliceSize, sliceSize * 2).map((item) => (
            <li key={item.id}>{item.orderTypeName}</li>
          ))}
        </ul>
      </TableCell>
      <TableCell className={classes.tableCell}>
        <ul>
          {orderTypes.slice(sliceSize * 2, 100).map((item) => (
            <li key={item.id}>{item.orderTypeName}</li>
          ))}
        </ul>
      </TableCell>
      <TableCell className={classes.tableCell}>
        <IconButton
          onClick={() => {
            onEditManager(user);
          }}
        >
          <Edit fontSize='small' />
        </IconButton>
      </TableCell>
    </TableRow>

  );
}

export default ManagerRow;