import { IconButton, TableCell, TableRow } from "@material-ui/core";
import { makeStyles } from '@material-ui/core/styles';
import {
    ArrowForwardIosSharp as DetailsIcon,
    LinkSharp as LinkIcon,
    LocalGroceryStoreSharp as FriendsIcon
} from '@material-ui/icons';
import useJwt from '../../hooks/JwtHooks';
import { UserDeliveryOrder } from "./types";

const useStyles = makeStyles((theme) => ({
  tdAmount: {
    textAlign: 'right',
    width: '90px',
  },
  tdLink: {
    textAlign: 'center',
    width: '70px',
  },
  link: {
    verticalAlign: 'middle',
    color: '#337ab7',
    textDecoration:'none',
  }
}));

interface Props {
  order: UserDeliveryOrder;
  onOpenDetail: (orderId: string, userId: string) => void;
  onFriendAccounting: (orderId: string, userId: string) => void;
}

const InDeliveryOrdersRow: React.FC<Props> = ({ order, onOpenDetail, onFriendAccounting }) => {
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
        {order.external ? (
            <a href={order.externallink} target='blank' className={classes.link}><LinkIcon fontSize='small' /></a>
        ) : (
            <IconButton
              onClick={() => { if (jwt) { onOpenDetail(order.id, jwt.id); }}}
              size='small'
            >
              <DetailsIcon fontSize='small' />
            </IconButton>
        )}
      </TableCell>
      <TableCell className={classes.tdLink}>
        {order.amici && order.contabilizzabile ? (
            <IconButton
              onClick={() => { if (jwt) { onFriendAccounting(order.id, jwt.id); }}}
              size='small'
            >
              <FriendsIcon fontSize='small' />
            </IconButton>
        ) : null }
      </TableCell>
    </TableRow>

  )
}

export default InDeliveryOrdersRow;