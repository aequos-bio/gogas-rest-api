import { IconButton, TableCell, TableRow } from "@material-ui/core";
import { makeStyles } from '@material-ui/core/styles';
import { ArrowForwardIosSharp as EditIcon } from '@material-ui/icons';
import { UserAccountingTotal } from "./types";
import useJwt from '../../hooks/JwtHooks';

const useStyles = makeStyles((theme) => ({
  tdAmount: {
    textAlign: 'right',
    width: '90px',
  },
  tdLink: {
    textAlign: 'center',
    width: '70px',
  },
}));

interface Props {
  order: UserDeliveryOrder;
  onOpenDetail: (orderId: string, userId: string) => void;
}

const InDeliveryOrdersRow: React.FC<Props> = ({ order, onOpenDetail }) => {
  const classes = useStyles();
  const jwt = useJwt();

  return (
    <TableRow hover>
      <TableCell>{order.tipoordine}</TableCell>
      <TableCell>{order.dataconsegna}</TableCell>
      <TableCell className={classes.tdAmount}>
        {order.numarticoli}
      </TableCell>
      <TableCell className={classes.tdAmount}>
        {order.totaleordine.toFixed(2)} €
      </TableCell>
      <TableCell className={classes.tdLink}>
        <IconButton
          onClick={() => onOpenDetail(order.id, jwt.id)}
          size='small'
        >
          <EditIcon fontSize='small' />
        </IconButton>
      </TableCell>
    </TableRow>

  )
}

export default InDeliveryOrdersRow;