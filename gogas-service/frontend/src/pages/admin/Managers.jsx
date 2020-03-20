import React, { useEffect, useCallback, useState, useMemo } from 'react';
import {
  Container,
  Fab,
  IconButton,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
} from '@material-ui/core';
import {
  AddSharp as PlusIcon,
  EditSharp as EditIcon,
} from '@material-ui/icons';
import { withSnackbar } from 'notistack';
import { makeStyles } from '@material-ui/core/styles';
import _ from 'lodash';
import { apiGetJson } from '../../utils/axios_utils';
import PageTitle from '../../components/PageTitle';
import LoadingRow from '../../components/LoadingRow';

const useStyles = makeStyles(theme => ({
  fab: {
    position: 'fixed',
    bottom: theme.spacing(2),
    right: theme.spacing(2),
  },
  tableCell: {
    paddingLeft: theme.spacing(1),
    paddingRight: theme.spacing(1),
    verticalAlign: 'top',
    width: '25%',
  },
  tdButtons: {
    fontSize: '130%',
    textAlign: 'center',
    minWidth: '44px',
    width: '44px',
  },
  cellHeader: {
    paddingLeft: theme.spacing(1),
    paddingRight: theme.spacing(1),
  },
}));

const Managers = ({ enqueueSnackbar }) => {
  const classes = useStyles();
  const [managers, setManagers] = useState([]);
  const [loading, setLoading] = useState(false);

  const reload = useCallback(() => {
    setLoading(true);
    apiGetJson('/api/ordertype/manager/list', {}).then(mm => {
      setLoading(false);
      if (mm.error) {
        enqueueSnackbar(mm.errorMessage, { variant: 'error' });
      } else {
        const _managers = {};
        mm.forEach(m => {
          let list;
          if (_managers[m.userName]) {
            list = _managers[m.userName];
          } else {
            list = [];
            _managers[m.userName] = list;
          }
          list.push(m);
        });
        setManagers(_managers);
      }
    });
  }, [enqueueSnackbar]);

  useEffect(() => {
    reload();
  }, [reload]);

  const newManager = useCallback(() => {
    enqueueSnackbar('Funzione non implementata!', { variant: 'error' });
  }, [enqueueSnackbar]);

  const editManager = useCallback(
    id => {
      enqueueSnackbar('Funzione non implementata!', { variant: 'error' });
    },
    [enqueueSnackbar]
  );

  const rows = useMemo(() => {
    const sorted = _.sortBy(Object.keys(managers));
    return loading ? (
      <LoadingRow colSpan={3} />
    ) : (
      sorted.map(m => {
        const orderTypes = _.sortBy(managers[m], i =>
          i.orderTypeName.toUpperCase()
        );
        const sliceSize = Math.max(
          7,
          Number.parseInt(orderTypes.length / 3, 10) + 1
        );
        return (
          <TableRow key={`user-${m}`} hover>
            <TableCell className={classes.tableCell}>{m}</TableCell>
            <TableCell className={classes.tableCell}>
              <ul>
                {orderTypes.slice(0, sliceSize).map(item => (
                  <li key={item.id}>{item.orderTypeName}</li>
                ))}
              </ul>
            </TableCell>
            <TableCell className={classes.tableCell}>
              <ul>
                {orderTypes.slice(sliceSize, sliceSize * 2).map(item => (
                  <li key={item.id}>{item.orderTypeName}</li>
                ))}
              </ul>
            </TableCell>
            <TableCell className={classes.tableCell}>
              <ul>
                {orderTypes.slice(sliceSize * 2, 100).map(item => (
                  <li key={item.id}>{item.orderTypeName}</li>
                ))}
              </ul>
            </TableCell>
            <TableCell className={classes.tableCell}>
              <IconButton
                onClick={() => {
                  editManager(m.id);
                }}
              >
                <EditIcon fontSize="small" />
              </IconButton>
            </TableCell>
          </TableRow>
        );
      })
    );
  }, [classes, managers, editManager, loading]);

  return (
    <Container maxWidth={false}>
      <PageTitle title="Referenti" />

      <Fab className={classes.fab} color="secondary" onClick={newManager}>
        <PlusIcon />
      </Fab>

      <TableContainer>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell className={classes.cellHeader}>Referente</TableCell>
              <TableCell className={classes.cellHeader} colSpan={3}>
                Ordini
              </TableCell>
              <TableCell className={classes.tdButtons} />
            </TableRow>
          </TableHead>

          <TableBody className={classes.tableBody}>{rows}</TableBody>
        </Table>
      </TableContainer>
    </Container>
  );
};

export default withSnackbar(Managers);
