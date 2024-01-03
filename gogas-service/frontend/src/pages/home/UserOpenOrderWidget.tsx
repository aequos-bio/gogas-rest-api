import React from 'react';
import { Avatar, Card, CardContent, CardHeader, Grid } from "@material-ui/core";
import {
  CheckSharp as CheckIcon,
} from '@material-ui/icons';
import { green } from '@material-ui/core/colors';
import { makeStyles } from '@material-ui/core/styles';
import { UserOpenOrder, UserSelect } from "./types";
import { UserOpenOrderStatus } from './UserOpenOrderStatus';

interface Props {
  order: UserOpenOrder;
  users: UserSelect[];
}

const useStyles = makeStyles(() => ({
  header: {
    paddingBottom: '8px',
  },
  content: {
    display: 'flex',
    flexDirection: 'column',
    paddingTop: '16px',
    paddingBottom: '16px !important'
  },
  ordered: {
    backgroundColor: green[500],
  },
}));

export const UserOpenOrderWidget: React.FC<Props> = ({ order, users }) => {
  const classes = useStyles();

  return (
    <Grid item xs={12} sm={6} md={4} lg={3} xl={3}>
      <Card>
        <CardHeader className={classes.header}
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
        <CardContent className={classes.content}>
          <UserOpenOrderStatus order={order} users={users} />
        </CardContent>
      </Card>
    </Grid>

  )
}