import React, { useState, useCallback } from 'react';
import { Avatar, Card, CardContent, CardHeader, Grid, IconButton, Menu, MenuItem } from "@material-ui/core";
import { CheckSharp as CheckIcon, AddSharp as PlusIcon, EditSharp as EditIcon } from '@material-ui/icons';
import { green } from '@material-ui/core/colors';
import { makeStyles } from '@material-ui/core/styles';
import { UserOpenOrder } from "./types";
import { Friend } from "../users/types";
import { filter } from "lodash";

interface Props {
  order: UserOpenOrder;
  userNameOrder: 'NC' | 'CN';
  onOpenDetail: (orderId: string, userId: string) => void;
  users: UserSelect[];
}

const useStyles = makeStyles(() => ({
  ordered: {
    backgroundColor: green[500],
  },
  add: {
    float: 'right',
  }
}));

export const UserOpenOrderWidget: React.FC<Props> = ({ order, userNameOrder, onOpenDetail, users }) => {
  const classes = useStyles();
  const [anchorEl, setAnchorEl] = useState(null);
  const open = Boolean(anchorEl);

  const handleOpenMenu = useCallback(event => {
      setAnchorEl(event.currentTarget);
    }, []);

  const handleCloseMenu = useCallback(() => {
      setAnchorEl(null);
  }, []);

  var addUsers = filter(users, (u) => !(order.userOrders.find((o) => o.userId == u.id)));

  return (
    <Grid item xs={12} sm={12} md={6} lg={4} xl={3}>
      <Card>
        <CardHeader
          avatar={
            <Avatar
              className={
                order.userOrders && order.userOrders.length ? classes.ordered : undefined
              }
            >
              {order.userOrders && order.userOrders.length ? <CheckIcon /> : <div />}
            </Avatar>
          }
          title={order.tipoordine}
          subheader={
            <div>
              Consegna {order.dataconsegna}
              <br />
              Chiusura {order.datachiusura} {order.orachiusura}:00
            </div>
          }
        />
        <CardContent>
          {order.userOrders && order.userOrders.length ? (
            <span>
              {order.userOrders.map((suborder) => (
                <div key={`userorder-${order.id}-${suborder.userId}`}>
                  <span>
                    {userNameOrder === 'NC'
                    ? `${suborder.firstname} ${suborder.lastname}`
                    : `${suborder.lastname} ${suborder.firstname}`}
                  , {suborder.itemsCount} articoli, {suborder.totalAmount.toFixed(2)} €
                  </span>
                  <IconButton
                            onClick={() => onOpenDetail(order.id, suborder.userId)}
                            size='small'
                          >
                    <EditIcon />
                  </IconButton>
                </div>
              ))}
            </span>
          ) : (
            <span>Nessun ordine compilato</span>
          )}
          {!addUsers.length ? null :
            users.length > 1 ? (
            <span className={classes.add}>
              <IconButton onClick={handleOpenMenu} size='small'>
                <PlusIcon />
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
                  <MenuItem onClick={() => onOpenDetail(order.id, user.id)} key="{user.id}">
                    {user.description}
                  </MenuItem>
                ))}
              </Menu>
            </span>
          ) : (
            <IconButton
                      className={classes.add}
                      onClick={() => onOpenDetail(order.id, users[0].id)}
                      size='small'
                    >
              <PlusIcon />
            </IconButton>
          )}
        </CardContent>
      </Card>
    </Grid>

  )
}