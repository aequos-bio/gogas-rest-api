import React from 'react';
import { Avatar, Card, CardContent, CardHeader, Grid, Typography } from "@material-ui/core";
import {
  CheckSharp as CheckIcon, EventBusy, LocalShipping,
} from '@material-ui/icons';
import { green, grey } from '@material-ui/core/colors';
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
  unordered: {
    border: '1px solid ' + grey[500],
    backgroundColor: 'white'
  },
  dateAndIcon: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    '& *:first-child': {
      marginRight: '8px'
    }
  }
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
                order.userOrders && order.userOrders.length ? classes.ordered : classes.unordered
              }
            >
              {order.userOrders && order.userOrders.length ? <CheckIcon /> : <div />}
            </Avatar>
          }
          title={
            <Typography variant="button">{order.tipoordine}</Typography>
          }
          subheader={
            <div>
              <div className={classes.dateAndIcon}>
                <EventBusy />{' '}
                {order.datachiusura} {order.orachiusura}:00
              </div>
              <div className={classes.dateAndIcon}>
                <LocalShipping />{' '}
                {order.dataconsegna}

              </div>
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