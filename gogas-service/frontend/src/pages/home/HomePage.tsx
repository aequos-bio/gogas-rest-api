import React from 'react';
import { useSelector } from 'react-redux';
import {
  Container,
  Grid,
} from '@material-ui/core';
import { RootState } from '../../store/store';
import { useUserOrdersAPI } from './useUserOrdersAPI';
import { UserOpenOrderWidget } from './UserOpenOrderWidget';

const Home: React.FC = () => {
  const info = useSelector((state: RootState) => state.info);
  const { openOrders } = useUserOrdersAPI();

  return (
    <Container maxWidth={false}>
      <Grid container spacing={3}>
        {openOrders.map((o) => (
          <UserOpenOrderWidget key={`order-${o.id}`} order={o} userNameOrder={info['visualizzazione.utenti']} />
        ))}
      </Grid>
    </Container>
  );
};

export default Home;
