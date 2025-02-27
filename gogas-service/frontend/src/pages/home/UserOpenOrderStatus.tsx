import { useCallback, useState } from "react";
import { IconButton, Menu, MenuItem } from "@material-ui/core";
import { AddSharp, EditSharp, LinkSharp } from '@material-ui/icons';
import { makeStyles } from '@material-ui/core/styles';
import { green } from "@material-ui/core/colors";
import { filter } from "lodash";
import { UserOpenOrder, UserSelect, UserSubOrder } from "./types"
import { useAppSelector } from "../../store/store";
import { useHistory } from "react-router-dom";

interface Props {
  order: UserOpenOrder;
  users: UserSelect[];
}

const useStyles = makeStyles(() => ({
  orderDetail: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    '& *:first-child': {
      flexGrow: 1
    }
  },
  ordered: {
    backgroundColor: green[500],
  },
  link: {
    verticalAlign: 'middle',
    color: '#337ab7',
    textDecoration: 'none',
  }
}));

const ExternalOrder = ({ order }: { order: UserOpenOrder }) => {
  const classes = useStyles();

  return (
    <span>Per compilare l'ordine fare click <a className={classes.link} href={order.externallink} target='blank'>qui <LinkSharp fontSize="small" className={classes.link} /></a></span>
  )
}

const UserSuborderDescription = ({ order, suborder, onOpenDetail }: { order: UserOpenOrder, suborder: UserSubOrder, onOpenDetail: (orderId: string, userId: string) => void }) => {
  const classes = useStyles();
  const info = useAppSelector((state) => state.info);
  const userNameOrder = info['visualizzazione.utenti'];
  const amount = suborder.totalAmount != null ? suborder.totalAmount.toFixed(2) + ' €' : '-';

  return (
    <div className={classes.orderDetail}>
      <span >
        {userNameOrder === 'NC'
          ? `${suborder.firstname} ${suborder.lastname}`
          : `${suborder.lastname} ${suborder.firstname}`}
        , {suborder.itemsCount} articoli, {amount}
      </span>
      <IconButton
        onClick={() => onOpenDetail(order.id, suborder.userId)}
        size='small'
      >
        <EditSharp />
      </IconButton>
    </div>

  )
}

const AddOrder = ({ order, users, onEditOrder }: { order: UserOpenOrder; users: UserSelect[]; onEditOrder: (orderId: string, userId: string) => void }) => {
  const classes = useStyles();
  const [anchorEl, setAnchorEl] = useState(null);
  const open = Boolean(anchorEl);
  var addUsers = filter(users, (u) => !(order.userOrders.find((o) => o.userId == u.id)));

  const handleOpenMenu = useCallback(event => {
    setAnchorEl(event.currentTarget);
  }, []);

  const handleCloseMenu = useCallback(() => {
    setAnchorEl(null);
  }, []);

  return (
    <div className={classes.orderDetail}>
      <div>
        {order.userOrders.length ? null :
          <span>Nessun ordine compilato</span>
        }
      </div>

      <div>
        {addUsers.length ? (
          <span>
            {users.length > 1 ? (
              <>
                <IconButton
                  onClick={handleOpenMenu}
                  size='small'
                >
                  <AddSharp />
                </IconButton>
                <Menu
                  anchorEl={anchorEl}
                  keepMounted
                  open={open}
                  onClose={handleCloseMenu}
                  PaperProps={{
                    style: {
                      width: 300,
                    },
                  }}
                >
                  {addUsers.map((user) => (
                    <MenuItem onClick={() => onEditOrder(order.id, user.id)} key={`useradd-${order.id}-${user.id}`}>
                      {user.description}
                    </MenuItem>
                  ))}
                </Menu>
              </>
            ) : (
              <IconButton
                onClick={() => onEditOrder(order.id, users[0].id)}
              // size='small'
              >
                <AddSharp />
              </IconButton>
            )}
          </span>
        )
          : <></>}
      </div>

    </div>
  )
}

export const UserOpenOrderStatus: React.FC<Props> = ({ order, users }) => {
  const history = useHistory();

  const onOpenDetail = useCallback((orderId: string, userId: string) => {
    history.push(`/legacy/ordersdetails?orderId=${orderId}&userId=${userId}`)
  }, [history]);


  if (order.external) return <ExternalOrder order={order} />

  return (
    <>
      {order.userOrders.length ? (
        order.userOrders.map((suborder) => (
          <UserSuborderDescription
            key={`userorder-${order.id}-${suborder.userId}`}
            order={order}
            suborder={suborder}
            onOpenDetail={onOpenDetail}
          />
        ))

      ) : null}

      <AddOrder order={order} onEditOrder={onOpenDetail} users={users} />

    </>
  )

}

