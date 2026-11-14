import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Container, Paper, Typography, IconButton, Tooltip, Button, Divider, CircularProgress, Tabs, Tab, useMediaQuery } from '@material-ui/core';
import {
  WarningSharp as WarningIcon,
  DeleteSharp as DeleteIcon,
  PersonAddSharp as AddUserIcon,
  CheckSharp as ApplyIcon,
  ShoppingBasketSharp as ProductsIcon,
  PeopleSharp as UsersTabIcon,
  ArrowBackSharp as BackIcon,
} from '@material-ui/icons';
import { makeStyles, useTheme } from '@material-ui/core/styles';
import ToggleButton from '@material-ui/lab/ToggleButton';
import ToggleButtonGroup from '@material-ui/lab/ToggleButtonGroup';
import { useHistory } from 'react-router-dom';
import queryString from 'query-string';
import EditableGridTable, { EditableColumn } from '../../../../components/table/EditableGridTable';
import RowActionsMenu from '../../../../components/table/RowActionsMenu';
import SortableTable, { Column } from '../../../../components/table/SortableTable';
import NumberField from '../../../../components/form/NumberField';
import ActionDialog from '../../../../components/ActionDialog';
import AddMovementDialog from './AddMovementDialog';
import OrderWorkflowTimeline from './OrderWorkflowTimeline';
import OrderMetaPanel from './OrderMetaPanel';
import { useOrderByUserAPI } from './useOrderByUserAPI';
import { OrderByUser, OrderItemByUser, SelectItem } from './types';

const useStyles = makeStyles((theme) => ({
  header: { marginBottom: theme.spacing(2), padding: theme.spacing(1, 2) },
  headerTopRow: {
    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
    gap: theme.spacing(1), marginBottom: theme.spacing(0.5),
  },
  toggleIcon: { marginRight: theme.spacing(0.5) },
  toggleButton: {
    padding: theme.spacing(0.75, 2.5),
    textTransform: 'none',
    '&.Mui-selected': {
      color: theme.palette.success.dark,
      borderColor: theme.palette.success.dark,
      backgroundColor: 'rgba(76, 175, 80, 0.12)',
      '&:hover': { backgroundColor: 'rgba(76, 175, 80, 0.2)' },
    },
  },
  // View switch tabs (mobile only), placed between the actions toolbar and the tables.
  viewTabs: {
    marginTop: theme.spacing(2),
    marginBottom: theme.spacing(2),
    borderBottom: `1px solid ${theme.palette.divider}`,
  },
  grids: {
    display: 'flex',
    gap: theme.spacing(2),
    alignItems: 'flex-start',
    // On phones the two tables cannot sit side by side: stack them vertically
    // and let each one scroll horizontally within its own container.
    [theme.breakpoints.down('sm')]: { flexDirection: 'column' },
  },
  summary: {
    flex: '1 1 0',
    minWidth: 0,
    [theme.breakpoints.down('sm')]: { width: '100%', flex: '1 1 auto' },
  },
  detail: {
    flex: '1 1 0',
    minWidth: 0,
    [theme.breakpoints.down('sm')]: { width: '100%', flex: '1 1 auto' },
  },
  detailTitle: { padding: theme.spacing(1), fontWeight: 600 },
  detailLoader: {
    display: 'flex', justifyContent: 'center', alignItems: 'center',
    padding: theme.spacing(4),
  },
  functions: {
    display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: theme.spacing(1),
    padding: theme.spacing(1), marginBottom: theme.spacing(1),
  },
  functionsSpacer: { flex: '1 1 auto' },
  warn: { color: theme.palette.error.main },
}));

const euro = (n?: number) =>
  n === undefined || n === null
    ? ''
    : new Intl.NumberFormat('it-IT', { style: 'currency', currency: 'EUR' }).format(n);
const num = (n?: number, d = 3) => (n === undefined || n === null ? '' : n.toFixed(d));

const OrderByUserPage: React.FC = () => {
  const classes = useStyles();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));
  const history = useHistory();
  const orderId = (queryString.parse(window.location.search).orderId as string) || '';

  const {
    details, users, loadingUsers,
    loadDetails, loadUsers, loadUserItems,
    updateNetAmount, addMovement, deleteMovement, updateShippingCost, loadAvailableUsers,
  } = useOrderByUserAPI(orderId);

  const [selectedUserId, setSelectedUserId] = useState<string | undefined>(undefined);
  const [items, setItems] = useState<OrderItemByUser[]>([]);
  const [detailLoading, setDetailLoading] = useState(false);
  const [shipping, setShipping] = useState<number | undefined>(undefined);
  const selectedUser = useMemo(
    () => users.find((u) => u.IdUtente === selectedUserId),
    [users, selectedUserId],
  );

  const [addOpen, setAddOpen] = useState(false);
  const [availableUsers, setAvailableUsers] = useState<SelectItem[]>([]);
  const [deleteTarget, setDeleteTarget] = useState<OrderByUser | undefined>(undefined);

  const editable = !!details?.editable;
  const computed = !!details?.totaleCalcolato;
  const external = !!details?.external;
  // Net amount is editable only for non-computed (external) orders.
  const nettoEditable = editable && !computed;
  // Add/delete movements available for editable external orders.
  const canManageMovements = editable && external;
  // Show the per-user detail (ordered products) whenever the order is not
  // external: always for computed orders, and also for non-computed internal
  // orders (totaleCalcolato=false but not external).
  const showDetail = !external;

  useEffect(() => {
    if (!orderId) return;
    loadDetails().then((d) => {
      if (d) setShipping(d.speseTrasporto);
    });
    loadUsers();
  }, [orderId, loadDetails, loadUsers]);

  const selectUser = useCallback(
    (user: OrderByUser) => {
      setSelectedUserId(user.IdUtente);
      if (showDetail) {
        setItems([]);
        setDetailLoading(true);
        loadUserItems(user.IdUtente)
          .then(setItems)
          .finally(() => setDetailLoading(false));
      }
    },
    [showDetail, loadUserItems],
  );

  const reloadDetail = useCallback(() => {
    if (showDetail && selectedUserId) loadUserItems(selectedUserId).then(setItems);
  }, [showDetail, selectedUserId, loadUserItems]);

  const applyShipping = useCallback(
    (value: number | undefined, notify: boolean) => {
      if (value === undefined) return;
      updateShippingCost(value).then((updated) => {
        if (updated) {
          loadUsers();
          if (notify) {
            // success toast handled implicitly; keep silent otherwise
          }
        }
      });
    },
    [updateShippingCost, loadUsers],
  );

  // ----- by-user grid -----
  const summaryColumns: EditableColumn<OrderByUser>[] = useMemo(() => {
    const cols: EditableColumn<OrderByUser>[] = [
      { id: 'NomeUtente', label: 'Utente', align: 'left', render: (u) => u.NomeUtente },
      { id: 'ArticoliOrdinati', label: 'N. Art.', align: 'right', width: 60, render: (u) => `${u.ArticoliOrdinati}` },
      {
        id: 'TotaleNetto', label: 'Costo Netto', align: 'right', width: 95,
        editable: nettoEditable,
        decimals: 2,
        getValue: (u) => u.TotaleNetto,
        canEdit: () => nettoEditable,
        render: (u) => euro(u.TotaleNetto),
        footer: (rs) => euro(rs.reduce((s, u) => s + (u.TotaleNetto || 0), 0)),
      },
      { id: 'CostoTrasporto', label: 'Trasporto', align: 'right', width: 90, render: (u) => euro(u.CostoTrasporto) },
      {
        id: 'TotaleOrdine', label: 'Totale', align: 'right', width: 95,
        render: (u) => euro(u.TotaleOrdine),
        footer: (rs) => euro(rs.reduce((s, u) => s + (u.TotaleOrdine || 0), 0)),
      },
      {
        id: '_rosso', label: '', align: 'center', width: 40,
        render: (u) =>
          u.InRosso ? (
            <Tooltip title="Il gasista ha il conto in rosso">
              <WarningIcon fontSize="small" className={classes.warn} />
            </Tooltip>
          ) : null,
      },
    ];

    if (canManageMovements) {
      cols.push({
        id: '_actions', label: '', align: 'center', width: 48,
        render: (u) => (
          <RowActionsMenu
            actions={[
              {
                key: 'delete',
                label: "Elimina l'addebito",
                icon: <DeleteIcon fontSize="small" />,
                onClick: () => setDeleteTarget(u),
              },
            ]}
          />
        ),
      });
    }

    return cols;
  }, [nettoEditable, canManageMovements, classes.warn]);

  const onNettoCommit = useCallback(
    async (user: OrderByUser, _col: string, value: number) => {
      const ok = await updateNetAmount(user.IdUtente, value);
      if (ok) {
        applyShipping(shipping, false);
        reloadDetail();
      }
      return ok;
    },
    [updateNetAmount, applyShipping, shipping, reloadDetail],
  );

  // ----- per-user detail grid (computed orders only) -----
  const detailColumns: Column[] = useMemo(
    () => [
      { label: 'Prodotto', type: 'String', alignment: 'Left', property: 'name' },
      { label: 'Qta Rit.', type: 'Number', alignment: 'Right', property: 'qtaRitirata' },
      { label: 'U.M.', type: 'String', alignment: 'Center', property: 'um' },
      { label: 'Prezzo', type: 'Amount', alignment: 'Right', property: 'tot' },
    ],
    [],
  );

  const openAddDialog = useCallback(() => {
    loadAvailableUsers().then((u) => {
      setAvailableUsers(u);
      setAddOpen(true);
    });
  }, [loadAvailableUsers]);

  const onAddSave = useCallback(
    async (userId: string, amount: number, _keepOpen: boolean) => {
      const ok = await addMovement(userId, amount);
      if (ok) {
        loadUsers();
        applyShipping(shipping, false);
        reloadDetail();
      }
      return ok;
    },
    [addMovement, loadUsers, applyShipping, shipping],
  );

  const runDelete = useCallback(() => {
    if (!deleteTarget) return;
    deleteMovement(deleteTarget.IdUtente).then((ok) => {
      setDeleteTarget(undefined);
      if (ok) {
        loadUsers();
        applyShipping(shipping, false);
        reloadDetail();
      }
    });
  }, [deleteTarget, deleteMovement, loadUsers, applyShipping, shipping, reloadDetail]);

  const detailRows = useMemo(() => items.map((value) => ({ value })), [items]);

  return (
    <Container maxWidth={false}>
      <Paper className={classes.header} variant="outlined">
        <div className={classes.headerTopRow}>
          <Button
            size="small"
            startIcon={<BackIcon />}
            onClick={() => history.push('/legacy/orderslist')}
          >
            Ordini
          </Button>
          {/* Desktop: compact segmented switch between the two views. */}
          {!isMobile ? (
            <ToggleButtonGroup
              size="small"
              exclusive
              value="user"
              onChange={(_e, v) => {
                if (v === 'product' && computed) history.push(`/legacy/orderdetails?orderId=${orderId}`);
              }}
            >
              <ToggleButton value="product" disabled={!computed} className={classes.toggleButton}>
                <ProductsIcon fontSize="small" className={classes.toggleIcon} />
                Per prodotto
              </ToggleButton>
              <ToggleButton value="user" className={classes.toggleButton}>
                <UsersTabIcon fontSize="small" className={classes.toggleIcon} />
                Per utente
              </ToggleButton>
            </ToggleButtonGroup>
          ) : null}
        </div>
      </Paper>

      <OrderMetaPanel details={details} />

      <OrderWorkflowTimeline details={details} />

      {editable ? (
        <Paper className={classes.functions} variant="outlined">
          <Typography variant="body2">Costo di trasporto:</Typography>
          <div style={{ width: 140 }}>
            <NumberField
              value={shipping}
              decimals={2}
              min={0}
              endAdornment="€"
              onChange={(v) => setShipping(v)}
            />
          </div>
          <Tooltip title="Applica: il costo viene suddiviso sugli ordinanti in proporzione all'importo">
            <Button size="small" startIcon={<ApplyIcon />} onClick={() => applyShipping(shipping, true)}>
              Applica
            </Button>
          </Tooltip>

          {canManageMovements ? (
            <>
              <div className={classes.functionsSpacer} />
              <Divider orientation="vertical" flexItem />
              <Button
                size="small"
                variant="outlined"
                color="primary"
                startIcon={<AddUserIcon />}
                onClick={openAddDialog}
              >
                Aggiungi utente
              </Button>
            </>
          ) : null}
        </Paper>
      ) : null}

      {isMobile ? (
        <Tabs
          className={classes.viewTabs}
          value="user"
          indicatorColor="primary"
          textColor="primary"
          variant="fullWidth"
          onChange={(_e, v) => {
            if (v === 'product' && computed) history.push(`/legacy/orderdetails?orderId=${orderId}`);
          }}
        >
          <Tab
            value="product"
            icon={<ProductsIcon fontSize="small" />}
            label="Per prodotto"
            disabled={!computed}
          />
          <Tab value="user" icon={<UsersTabIcon fontSize="small" />} label="Per utente" />
        </Tabs>
      ) : null}

      <div className={classes.grids}>
        <Paper className={classes.summary} variant="outlined">
          <EditableGridTable
            columns={summaryColumns}
            rows={users}
            rowId={(u) => u.IdUtente}
            onCommit={onNettoCommit}
            onRowClick={showDetail ? selectUser : undefined}
            selectedRowId={selectedUserId}
            emptyMessage={loadingUsers ? 'Caricamento...' : 'Nessun utente'}
          />
        </Paper>

        {showDetail ? (
          <Paper className={classes.detail} variant="outlined">
            <div className={classes.detailTitle}>
              {selectedUser ? selectedUser.NomeUtente : 'Seleziona un utente'}
            </div>
            {selectedUser ? (
              detailLoading ? (
                <div className={classes.detailLoader}>
                  <CircularProgress size={28} />
                </div>
              ) : (
                <SortableTable
                  columns={detailColumns}
                  rows={detailRows}
                  settings={{
                    canAdd: false, canEdit: false, canDelete: false,
                    showHeader: true, showFooter: false, pagination: false,
                  }}
                />
              )
            ) : null}
          </Paper>
        ) : null}
      </div>

      <AddMovementDialog
        open={addOpen}
        availableUsers={availableUsers}
        onCancel={() => setAddOpen(false)}
        onSave={onAddSave}
      />

      <ActionDialog
        open={!!deleteTarget}
        onCancel={() => setDeleteTarget(undefined)}
        actions={['Ok']}
        onAction={runDelete}
        title="Conferma eliminazione"
        message={`L'addebito per l'utente ${deleteTarget?.NomeUtente ?? ''} verrà eliminato, continuare?`}
      />
    </Container>
  );
};

export default OrderByUserPage;
