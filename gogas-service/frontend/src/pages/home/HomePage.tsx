import React, { useCallback } from 'react';
import { useSelector } from 'react-redux';
import { useHistory } from 'react-router';
import { makeStyles } from '@material-ui/core/styles';
import {
  Container,
  Grid,
  Typography
} from '@material-ui/core';
import { RootState } from '../../store/store';
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
  const info = useSelector((state: RootState) => state.info);
  const { openOrders, userSelect, deliveryOrders } = useUserOrdersAPI();
  const classes = useStyles();
  const history = useHistory();

  const onOpenDetail = useCallback((orderId: string, userId: string) => {
      history.push(`/legacy/ordersdetails?orderId=${orderId}&userId=${userId}`)
  }, [history]);

  return (
    <Container maxWidth={false}>
      <Typography className={classes.title} component="h5" variant="h5">
        Ordini aperti
      </Typography>
      <Grid container spacing={3} className={classes.open}>
        {openOrders.map((o) => (
          <UserOpenOrderWidget key={`order-${o.id}`} order={o} userNameOrder={info['visualizzazione.utenti']} onOpenDetail={onOpenDetail} users={userSelect} />
        ))}
      </Grid>
      <div>
          <Typography className={classes.title} component="h5" variant="h5">
            Ordini in consegna
          </Typography>
          <div className={classes.subtitle}>
            <a href="legacy/ordershistory"className={classes.subtitleLink}>Vai allo storico <EditIcon className={classes.arrow} /></a>
          </div>
      </div>
      <InDeliveryOrdersTable orders={deliveryOrders} onOpenDetail={onOpenDetail} />
    </Container>
  );
};

export default Home;
