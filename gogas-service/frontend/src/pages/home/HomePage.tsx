import React from 'react';
import { makeStyles } from '@material-ui/core/styles';
import {
  Container,
  Grid,
  Typography
} from '@material-ui/core';
import { useUserOrdersAPI } from './useUserOrdersAPI';
import { UserOpenOrderWidget } from './UserOpenOrderWidget';
import InDeliveryOrdersTable from './InDeliveryOrdersTable';
import { ArrowForwardIosSharp as EditIcon } from '@material-ui/icons';

const useStyles = makeStyles((theme) => ({
  title: {
    paddingTop: '10px',
    paddingBottom: '30px',
    display: 'inline-block'
  },
  subtitle: {
    display: 'inline-block',
    float: 'right',
    paddingTop: '15px'
  },
  subtitleLink: {
    textDecoration: 'none',
    color: '#337ab7',
    fontSize: '1.1rem',
  },
  open: {
    paddingBottom: '60px'
  },
  arrow: {
    fontSize: '0.9rem',
    color: '#337ab7',
  },
}));

const Home: React.FC = () => {
  const { openOrders, userSelect, deliveryOrders } = useUserOrdersAPI();
  const classes = useStyles();

  return (
    <Container maxWidth={false}>
      <Typography className={classes.title} component="h5" variant="h5">
        Ordini aperti
      </Typography>
      {openOrders.length > 0 ? (
        <Grid container spacing={2} className={classes.open}>
          {openOrders.map((o) => (
            <UserOpenOrderWidget key={`order-${o.id}`} order={o} users={userSelect} />
          ))}
        </Grid>
      ) : (
        <div className={classes.open}>Nessun ordine aperto.</div>
      )}
      <div>
        <Typography className={classes.title} component="h5" variant="h5">
          Ordini in consegna
        </Typography>
        <div className={classes.subtitle}>
          <a href="legacy/ordershistory" className={classes.subtitleLink}>Vai allo storico <EditIcon className={classes.arrow} /></a>
        </div>
      </div>
      {deliveryOrders.length > 0 ? (
        <InDeliveryOrdersTable orders={deliveryOrders} />
      ) : (
        <div>Nessun ordine in consegna.</div>
      )}
    </Container>
  );
};

export default Home;
