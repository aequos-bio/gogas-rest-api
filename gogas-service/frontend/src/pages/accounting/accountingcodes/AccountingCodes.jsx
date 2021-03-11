import React, { useEffect, useState, useCallback, useMemo } from 'react';
import {
  Container,
  TableContainer,
  Table,
  TableHead,
  TableBody,
  TableRow,
  TableCell,
  IconButton,
} from '@material-ui/core';
import { EditSharp as EditIcon } from '@material-ui/icons';
import { withSnackbar } from 'notistack';
import { makeStyles } from '@material-ui/core/styles';
import _ from 'lodash';
import { apiGetJson } from '../../../utils/axios_utils';
import PageTitle from '../../../components/PageTitle';
import LoadingRow from '../../../components/LoadingRow';
import EditAccountingCodeDialog from './EditAccountingCodeDialog';

const useStyles = makeStyles(theme => ({
  tdButtons: {
    textAlign: 'center',
    minWidth: '44px',
    width: '44px',
  },
  cellHeader: {
    paddingLeft: theme.spacing(1),
    paddingRight: theme.spacing(1),
    verticalAlign: 'bottom',
  },
}));

const AccountingCodes = ({ enqueueSnackbar }) => {
  const classes = useStyles();
  const [orderTypes, setOrderTypes] = useState([]);
  const [loading, setLoading] = useState(false);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [selectedOrderType, setSelectedOrderType] = useState();

  const reload = useCallback(() => {
    setLoading(true);
    apiGetJson('/api/ordertype/accounting', {}).then(oo => {
      setLoading(false);
      if (oo.error) {
        enqueueSnackbar(oo.errorMessage, { variant: 'error' });
      } else {
        setOrderTypes(
          _.orderBy(oo, [
            o => (o.id === 'aequos' ? 'A' : 'Z'),
            o => o.description,
          ])
        );
      }
    });
  }, [enqueueSnackbar]);

  useEffect(() => {
    reload();
  }, [reload]);

  const dialogClosed = useCallback(
    needrefresh => {
      setDialogOpen(false);
      if (needrefresh) {
        reload();
      }
    },
    [reload]
  );

  const editAccountingCode = useCallback(orderType => {
    setSelectedOrderType(orderType);
    setDialogOpen(true);
  }, []);

  const rows = useMemo(() => {
    return loading ? (
      <LoadingRow colSpan={8} />
    ) : (
      orderTypes.map(o => (
        <TableRow>
          <TableCell>{o.description}</TableCell>
          <TableCell>{o.accountingCode}</TableCell>
          <TableCell>
            <IconButton onClick={() => editAccountingCode(o)}>
              <EditIcon />
            </IconButton>
          </TableCell>
        </TableRow>
      ))
    );
  }, [loading, orderTypes, editAccountingCode]);

  return (
    <Container maxWidth={false}>
      <PageTitle title="Gestione dei codici contabili" />

      <TableContainer>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Tipo ordine</TableCell>
              <TableCell style={{ width: '10%' }}>Codice contabile</TableCell>
              <TableCell className={classes.tdButtons} />
            </TableRow>
          </TableHead>

          <TableBody>{rows}</TableBody>
        </Table>
      </TableContainer>
      <EditAccountingCodeDialog
        open={dialogOpen}
        onClose={dialogClosed}
        code={selectedOrderType?.accountingCode}
        orderTypeId={selectedOrderType?.id}
      />
    </Container>
  );
};

export default withSnackbar(AccountingCodes);
