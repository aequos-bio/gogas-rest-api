import React, { useCallback, useEffect, useState, useMemo } from 'react';
import {
  Button,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
} from '@material-ui/core';
import {
  AddSharp as PlusIcon,
  SaveAltSharp as SaveIcon,
} from '@material-ui/icons';
import { makeStyles } from '@material-ui/core/styles';
import LoadingRow from '../../components/LoadingRow';
import { useHistory } from 'react-router';
import InDeliveryOrdersRow from './InDeliveryOrdersRow';

interface Props {
  orders: UserDeliveryOrder[];
  onOpenDetail: (orderId: string, userId: string) => void;
}

const useStyles = makeStyles((theme) => ({
  header: {
    fontWeight: 700,
  },
}));

const InDeliveryOrdersTable: React.FC<Props> = ({ orders, onOpenDetail }) => {
  const history = useHistory();
  const classes = useStyles();

  return (
      <TableContainer>
        <Table size='small'>
          <TableHead>
            <TableRow>
              <TableCell className={classes.header}>Tipo ordine</TableCell>
              <TableCell className={classes.header}>Data Consegna</TableCell>
              <TableCell className={classes.header}>Num. Articoli</TableCell>
              <TableCell className={classes.header}>Importo</TableCell>
              <TableCell/>
            </TableRow>
          </TableHead>

          <TableBody>
            {orders.map((order) => (
                <InDeliveryOrdersRow key={order.id} order={order} onOpenDetail={onOpenDetail} />
              ))
            }
          </TableBody>
        </Table>
      </TableContainer>
  );
}

export default InDeliveryOrdersTable;