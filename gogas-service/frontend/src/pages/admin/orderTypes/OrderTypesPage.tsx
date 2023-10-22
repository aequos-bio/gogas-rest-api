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
import { useSnackbar } from 'notistack';
import { makeStyles } from '@material-ui/core/styles';
import PageTitle from '../../../components/PageTitle';
import ActionDialog from '../../../components/ActionDialog';
import LoadingRow from '../../../components/LoadingRow';
import EditOrderTypeDialog from './EditOrderTypeDialog';
import OrderTypeRow from './OrderTypeRow';
import CategoriesDialog from './CategoriesDialog';
import { useOrderTypesAPI } from './useOrderTypesAPI';
import ManagersDialog from './ManagersDialog';
import { OrderType } from './typed';
import { Order } from '../../accounting/invoicemanagement/types';

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

const OrderTypes: React.FC = () => {
  const classes = useStyles();
  const { enqueueSnackbar } = useSnackbar();
  const [dialogMode, setDialogMode] = useState<false | 'new' | 'edit'>(false);
  const [selectedOrderType, setSelectedOrderType] = useState<OrderType | undefined>(undefined);
  const [deleteDlgOpen, setDeleteDlgOpen] = useState(false);
  const [categoriesDlgOpen, setCategoriesDlgOpen] = useState(false);
  const [managersDlgOpen, setManagersDlgOpen] = useState(false);
  const { orderTypes, loading, reload, deleteOrderType, syncWithAequos } = useOrderTypesAPI();


  useEffect(() => {
    reload();
  }, [reload]);

  const newOrderType = useCallback(() => {
    setSelectedOrderType(undefined);
    setDialogMode('new');
  }, []);

  const editOrderType = useCallback((_orderType: OrderType) => {
    setSelectedOrderType(_orderType);
    setDialogMode('edit');
  }, []);

  const _deleteOrderType = useCallback((_orderType: OrderType) => {
    setSelectedOrderType(_orderType);
    setDeleteDlgOpen(true);
  }, []);

  const editCategories = useCallback((_orderType: OrderType) => {
    setSelectedOrderType(_orderType);
    setCategoriesDlgOpen(true);
  }, []);

  const editManagers = useCallback((_orderType: OrderType) => {
    setSelectedOrderType(_orderType);
    setManagersDlgOpen(true);
  },
    []
  );

  const doDeleteOrderType = useCallback(() => {
    deleteOrderType((selectedOrderType as OrderType).id as string).then(() => {
      setDeleteDlgOpen(false);
    });
  }, [selectedOrderType]);

  const dialogClosed = useCallback(
    needrefresh => {
      setDialogMode(false);
      if (needrefresh) {
        reload();
      }
    },
    [reload]
  );

  const rows = useMemo(() => {
    return loading ? (
      <LoadingRow colSpan={8} />
    ) : (
      orderTypes.map(o => (
        <OrderTypeRow
          key={`ordertype-${o.id}`}
          orderType={o}
          onEdit={editOrderType}
          onDelete={_deleteOrderType}
          onEditCategories={editCategories}
          onEditManagers={editManagers}
        />
      ))
    );
  }, [
    orderTypes,
    loading,
    editOrderType,
    _deleteOrderType,
    editCategories,
    editManagers,
  ]);

  return (
    <Container maxWidth={false}>
      <PageTitle title="Tipi di ordine">
        <Button
          onClick={syncWithAequos}
          variant="outlined"
          startIcon={<SyncIcon />}
        >
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

          <TableBody>{rows}</TableBody>
        </Table>
      </TableContainer>

      <EditOrderTypeDialog
        mode={dialogMode}
        onClose={dialogClosed}
        orderType={selectedOrderType as OrderType}
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
        orderType={selectedOrderType}
        open={categoriesDlgOpen}
        onClose={() => setCategoriesDlgOpen(false)}
      />

      <ManagersDialog
        orderType={selectedOrderType}
        open={managersDlgOpen}
        onClose={() => setManagersDlgOpen(false)}
      />
    </Container>
  );
};

export default OrderTypes;
