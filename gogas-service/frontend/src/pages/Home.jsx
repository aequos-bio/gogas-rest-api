import React, { useState, useEffect, useMemo } from 'react';
import { useSelector } from 'react-redux';
import {
  Container,
  Grid,
  Card,
  CardHeader,
  CardContent,
  Avatar,
} from '@material-ui/core';
import { CheckSharp as CheckIcon } from '@material-ui/icons';
import { green } from '@material-ui/core/colors';
import Jwt from 'jsonwebtoken';
import moment from 'moment-timezone';
import _ from 'lodash';
import { makeStyles } from '@material-ui/core/styles';
import { apiGetJson } from '../utils/axios_utils';

const useStyles = makeStyles(() => ({
  ordered: {
    backgroundColor: green[500],
  },
}));

const Home = () => {
  const classes = useStyles();
  const [openOrders, setOpenOrders] = useState([]);
  const { authentication, info } = useSelector(state => state);

  const jwt = useMemo(() => {
    if (authentication.jwtToken) {
      const j = Jwt.decode(authentication.jwtToken);
      if (moment(j.exp * 1000).isBefore(moment())) {
        j.expired = true;
      }
      return j;
    }
    return null;
  }, [authentication]);

  useEffect(() => {
    if (!jwt || !jwt.id || jwt.expired) return;
    apiGetJson('/api/order/user/open').then(orders =>
      setOpenOrders(_.orderBy(orders, o => o.userOrders.length, 'desc'))
    );
  }, [jwt]);

  const items = useMemo(() => {
    return openOrders.map(o => (
      <Grid item xs={12} sm={12} md={6} lg={4} xl={3} key={`order-${o.id}`}>
        <Card>
          <CardHeader
            avatar={
              <Avatar
                className={
                  o.userOrders && o.userOrders.length ? classes.ordered : null
                }
              >
                {o.userOrders && o.userOrders.length ? <CheckIcon /> : <div />}
              </Avatar>
            }
            title={o.tipoordine}
            subheader={
              <div>
                Consegna {o.dataconsegna}
                <br />
                Chiusura {o.datachiusura} {o.orachiusura}:00
              </div>
            }
          />
          <CardContent>
            {o.userOrders && o.userOrders.length ? (
              <span>
                {o.userOrders.map(uo => (
                  <div key={`userorder-${o.id}-${uo.userId}`}>
                    {info['visualizzazione.utenti'] &&
                    info['visualizzazione.utenti'] === 'NC'
                      ? `${uo.firstname} ${uo.lastname}`
                      : `${uo.lastname} ${uo.firstname}`}
                    , {uo.itemsCount} articoli, {uo.totalAmount.toFixed(2)} €
                  </div>
                ))}
              </span>
            ) : (
              <span>Nessun ordine conpilato</span>
            )}
          </CardContent>
        </Card>
      </Grid>
    ));
  }, [openOrders, info, classes]);

  return (
    <Container maxWidth={false}>
      <Grid container spacing={3}>
        {items}
      </Grid>
    </Container>
  );
};

export default Home;
