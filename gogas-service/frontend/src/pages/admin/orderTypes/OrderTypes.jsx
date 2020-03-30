import React, { useEffect, useCallback, useState, useMemo } from 'react';
import {
  Container,
  Fab,
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
  SyncSharp as SyncIcon,
} from '@material-ui/icons';
import { withSnackbar } from 'notistack';
import { connect } from 'react-redux';
import { makeStyles } from '@material-ui/core/styles';
import { apiGetJson, apiPut, apiDelete } from '../../../utils/axios_utils';
import PageTitle from '../../../components/PageTitle';
import ActionDialog from '../../../components/ActionDialog';
import LoadingRow from '../../../components/LoadingRow';
import EditOrderTypeDialog from './EditOrderTypeDialog';
import OrderTypeItem from './OrderTypeItem';
import CategoriesDialog from './CategoriesDialog';

const useStyles = makeStyles(theme => ({
  fab: {
    position: 'fixed',
    bottom: theme.spacing(2),
    right: theme.spacing(2),
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
    verticalAlign: 'bottom',
  },
  cellHeaderFlag: {
    maxWidth: '60px',
    width: '60px',
    textAlign: 'center',
    height: '90px',
    whiteSpace: 'nowrap',
    '& > div': {
      transform: 'translate(15px,-5px) rotate(270deg)',
      width: '30px',
    },
  },
}));

const OrderTypes = ({ enqueueSnackbar }) => {
  const classes = useStyles();
  const [loading, setLoading] = useState(false);
  const [orderTypes, setOrderTypes] = useState([]);
  const [dialogMode, setDialogMode] = useState(false);
  const [selectedId, setSelectedId] = useState();
  const [deleteDlgOpen, setDeleteDlgOpen] = useState(false);
  const [categoriesDlgOpen, setCategoriesDlgOpen] = useState(false);
  const [managersDlgOpen, setManagersDlgOpen] = useState(false);

  const reload = useCallback(() => {
    setLoading(true);
    apiGetJson('/api/ordertype/list', {}).then(oo => {
      setLoading(false);
      if (oo.error) {
        enqueueSnackbar(oo.errorMessage, { variant: 'error' });
      } else {
        setOrderTypes(oo);
      }
    });
  }, [enqueueSnackbar]);

  useEffect(() => {
    reload();
  }, [reload]);

  const newOrderType = useCallback(() => {
    setSelectedId();
    setDialogMode('new');
  }, []);

  const editOrderType = useCallback(id => {
    setSelectedId(id);
    setDialogMode('edit');
  }, []);

  const deleteOrderType = useCallback(id => {
    setSelectedId(id);
    setDeleteDlgOpen(true);
  }, []);

  const editCategories = useCallback(id => {
    setSelectedId(id);
    setCategoriesDlgOpen(true);
  }, []);

  const editManagers = useCallback(
    id => {
      setSelectedId(id);
      setManagersDlgOpen(true);
      enqueueSnackbar('Non implementato!', { variant: 'error' });
    },
    [enqueueSnackbar]
  );

  const doDeleteOrderType = useCallback(() => {
    apiDelete(`/api/ordertype/${selectedId}`)
      .then(() => {
        setDeleteDlgOpen(false);
        reload();
        enqueueSnackbar('Tipo ordine eliminato', { variant: 'success' });
      })
      .catch(err => {
        enqueueSnackbar(
          err.response?.statusText ||
            "Errore nell'eliminazione del tipo ordine",
          { variant: 'error' }
        );
      });
  }, [enqueueSnackbar, selectedId, reload]);

  const dialogClosed = useCallback(
    needrefresh => {
      setDialogMode(false);
      if (needrefresh) {
        reload();
      }
    },
    [reload]
  );

  const syncWithAequos = useCallback(() => {
    apiPut('/api/ordertype/aequos/sync')
      .then(() => {
        enqueueSnackbar('Sincronizzazione completata con successo', {
          variant: 'success',
        });
        reload();
      })
      .catch(err => {
        enqueueSnackbar(`Errore nella sincronizzazione: ${err}`, {
          variant: 'error',
        });
      });
  }, [reload, enqueueSnackbar]);

  const rows = useMemo(() => {
    return loading ? (
      <LoadingRow colSpan={8} />
    ) : (
      orderTypes.map(o => (
        <OrderTypeItem
          key={`ordertype-${o.id}`}
          orderType={o}
          onEdit={editOrderType}
          onDelete={deleteOrderType}
          onEditCategories={editCategories}
          onEditManagers={editManagers}
        />
      ))
    );
  }, [
    orderTypes,
    loading,
    editOrderType,
    deleteOrderType,
    editCategories,
    editManagers,
  ]);

  return (
    <Container maxWidth={false}>
      <PageTitle title="Tipi di ordine">
        <Button onClick={syncWithAequos} startIcon={<SyncIcon />}>
          Sincronizza
        </Button>
      </PageTitle>

      <Fab className={classes.fab} color="secondary" onClick={newOrderType}>
        <PlusIcon />
      </Fab>

      <TableContainer>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell className={classes.cellHeader}>Descrizione</TableCell>
              <TableCell
                className={`${classes.cellHeader} ${classes.cellHeaderFlag}`}
              >
                <div>Raggr. amici</div>
              </TableCell>
              <TableCell
                className={`${classes.cellHeader} ${classes.cellHeaderFlag}`}
              >
                <div>Tot. calcolato</div>
              </TableCell>
              <TableCell
                className={`${classes.cellHeader} ${classes.cellHeaderFlag}`}
              >
                <div>Prevede turni</div>
              </TableCell>
              <TableCell
                className={`${classes.cellHeader} ${classes.cellHeaderFlag}`}
              >
                <div>Mostra prev.</div>
              </TableCell>
              <TableCell
                className={`${classes.cellHeader} ${classes.cellHeaderFlag}`}
              >
                <div>Mostra c. colli</div>
              </TableCell>
              <TableCell
                className={`${classes.cellHeader} ${classes.cellHeaderFlag}`}
              >
                <div>Ord. esterno</div>
              </TableCell>
              <TableCell className={classes.tdButtons} />
            </TableRow>
          </TableHead>

          <TableBody className={classes.tableBody}>{rows}</TableBody>
        </Table>
      </TableContainer>

      <EditOrderTypeDialog
        mode={dialogMode}
        onClose={dialogClosed}
        orderTypeId={selectedId}
      />

      <ActionDialog
        open={deleteDlgOpen}
        onCancel={() => setDeleteDlgOpen(false)}
        actions={['Ok']}
        onAction={doDeleteOrderType}
        title="Conferma eliminazione"
        message="Sei sicuro di voler eliminare il tipo ordine selezionato?"
      />

      <CategoriesDialog
        orderTypeId={selectedId}
        open={categoriesDlgOpen}
        onClose={() => setCategoriesDlgOpen(false)}
      />
    </Container>
  );
};

const mapStateToProps = state => {
  return {
    authentication: state.authentication,
    info: state.info,
  };
};

const mapDispatchToProps = {};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(withSnackbar(OrderTypes));
