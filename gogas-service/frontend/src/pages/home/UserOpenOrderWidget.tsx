import { Avatar, Card, CardContent, CardHeader, Grid } from "@material-ui/core";
import { CheckSharp as CheckIcon } from '@material-ui/icons';
import { green } from '@material-ui/core/colors';
import { makeStyles } from '@material-ui/core/styles';
import { UserOpenOrder } from "./types";

interface Props {
  order: UserOpenOrder;
  userNameOrder: 'NC' | 'CN'
}

const useStyles = makeStyles(() => ({
  ordered: {
    backgroundColor: green[500],
  },
}));

export const UserOpenOrderWidget: React.FC<Props> = ({ order, userNameOrder }) => {
  const classes = useStyles();

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
                  {userNameOrder === 'NC'
                    ? `${suborder.firstname} ${suborder.lastname}`
                    : `${suborder.lastname} ${suborder.firstname}`}
                  , {suborder.itemsCount} articoli, {suborder.totalAmount.toFixed(2)} €
                </div>
              ))}
            </span>
          ) : (
            <span>Nessun ordine conpilato</span>
          )}
        </CardContent>
      </Card>
    </Grid>

  )
}