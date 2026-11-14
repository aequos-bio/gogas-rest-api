import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Container, Grid, Button, Paper, IconButton, Collapse, Chip, Typography } from '@material-ui/core';
import {
  Search as SearchIcon,
  FilterList as FilterIcon,
  ExpandLess as ExpandLessIcon,
  ExpandMore as ExpandMoreIcon,
  Clear as ClearIcon,
  AddSharp as AddIcon,
  EditSharp as EditIcon,
  DeleteSharp as DeleteIcon,
  VisibilitySharp as DetailIcon,
  BuildSharp as ManageIcon,
  FolderSharp as CloseIcon,
  FolderOpenSharp as ReopenIcon,
  AttachMoneySharp as AccountIcon,
  ReplySharp as UndoAccountIcon,
  CancelSharp as CancelIcon,
  RestoreSharp as UndoCancelIcon,
  CheckSharp as CheckIcon,
} from '@material-ui/icons';
import { makeStyles } from '@material-ui/core/styles';
import { useHistory } from 'react-router-dom';
import { useSnackbar, SnackbarKey } from 'notistack';
import moment from 'moment-timezone';
import PageTitle from '../../../components/PageTitle';
import SortableTable, { Column } from '../../../components/table/SortableTable';
import RowActionsMenu, { RowAction } from '../../../components/table/RowActionsMenu';
import SelectField from '../../../components/form/SelectField';
import MultiSelectField from '../../../components/form/MultiSelectField';
import DatePickerField from '../../../components/form/DatePickerField';
import ActionDialog from '../../../components/ActionDialog';
import OrderEditDialog, { OrderPrefill } from './OrderEditDialog';
import CloseOrderDialog from './CloseOrderDialog';
import useJwt from '../../../hooks/JwtHooks';
import { useOrderManagerAPI } from './useOrderManagerAPI';
import { ManagerOrder, OrderInput, OrderSearchFilter } from './types';

/**
 * Full width of a date-range box: two 140px date fields + 16px gap + the box's
 * 16px horizontal padding on each side. Used to cap the first two dropdowns to
 * the same width.
 */
const DATE_BOX_WIDTH = 545;

const useStyles = makeStyles((theme) => ({
  filterPanel: {
    marginBottom: theme.spacing(2),
    backgroundColor: theme.palette.grey[50],
  },
  filterHeader: {
    display: 'flex',
    alignItems: 'center',
    gap: theme.spacing(1),
    padding: theme.spacing(0.5, 1, 0.5, 2),
    borderBottom: `1px solid ${theme.palette.divider}`,
  },
  filterTitle: { display: 'flex', alignItems: 'center', gap: theme.spacing(1), flexShrink: 0 },
  chips: {
    display: 'flex',
    flexWrap: 'wrap',
    gap: theme.spacing(0.5),
    flex: '1 1 auto',
  },
  filterBody: { padding: theme.spacing(1, 2, 2) },
  selectField: { maxWidth: DATE_BOX_WIDTH },
  rangeRow: { display: 'flex', flexWrap: 'wrap', gap: theme.spacing(2) },
  dateField: { minWidth: 140 },
  dateGroups: {
    display: 'flex',
    flexWrap: 'wrap',
    gap: theme.spacing(2),
    marginTop: theme.spacing(3),
  },
  dateGroup: {
    minWidth: 0,
    border: `1px solid ${theme.palette.divider}`,
    borderRadius: theme.shape.borderRadius,
    padding: theme.spacing(1.5, 2, 2),
    margin: 0,
  },
  dateLegend: {
    padding: theme.spacing(0, 0.5),
    fontSize: '0.75rem',
    color: theme.palette.text.secondary,
  },
  filterActions: {
    display: 'flex',
    justifyContent: 'flex-end',
    gap: theme.spacing(1),
    marginTop: theme.spacing(2),
  },
}));

const defaultDueDateFrom = () => moment().subtract(2, 'months').format('YYYY-MM-DD');
const toItDate = (iso?: string) => (iso ? moment(iso, 'YYYY-MM-DD').format('DD/MM/YYYY') : undefined);

const paidOptions = [
  { value: 'true', label: 'Evaso' },
  { value: 'false', label: 'Non evaso' },
];

/** Status-change actions that just need a confirmation + POST action/{code}. */
const CONFIRM_ACTIONS: Record<string, { code: string; title: string; message: string }> = {
  cancel: {
    code: 'cancel',
    title: 'Conferma annullamento',
    message:
      "L'ordine selezionato verrà annullato, gli ordini inseriti dagli utenti non saranno rimossi ma solo annullati. Continuare?",
  },
  undocancel: {
    code: 'undocancel',
    title: 'Conferma ripristino',
    message:
      "L'ordine selezionato verrà ripristinato in stato aperto e gli ordini inseriti dagli utenti saranno ripristinati. Continuare?",
  },
  riapri: {
    code: 'reopen',
    title: 'Conferma riapertura',
    message:
      "L'ordine selezionato verrà riaperto, tutte le modifiche effettuate sulle quantità andranno perse. Continuare?",
  },
  contabilizza: {
    code: 'contabilizza',
    title: 'Conferma contabilizzazione',
    message:
      "L'ordine selezionato verrà contabilizzato e gli importi saranno addebitati ai gasisti. Continuare?",
  },
  storna: {
    code: 'tornachiuso',
    title: 'Conferma storno',
    message:
      "L'ordine selezionato tornerà in stato chiuso, gli importi addebitati ai gasisti saranno stornati. Continuare?",
  },
};

/** Label + icon for each workflow action, shown in the row kebab menu. */
const ACTION_META: Record<string, { label: string; icon: React.ReactNode }> = {
  modifica: { label: 'Modifica', icon: <EditIcon fontSize="small" /> },
  elimina: { label: 'Elimina', icon: <DeleteIcon fontSize="small" /> },
  dettaglio: { label: 'Dettaglio', icon: <DetailIcon fontSize="small" /> },
  gestisci: { label: 'Gestisci', icon: <ManageIcon fontSize="small" /> },
  chiudi: { label: 'Chiudi', icon: <CloseIcon fontSize="small" /> },
  riapri: { label: 'Riapri', icon: <ReopenIcon fontSize="small" /> },
  contabilizza: { label: 'Contabilizza', icon: <AccountIcon fontSize="small" /> },
  storna: { label: 'Storna', icon: <UndoAccountIcon fontSize="small" /> },
  cancel: { label: 'Annulla', icon: <CancelIcon fontSize="small" /> },
  undocancel: { label: 'Ripristina', icon: <UndoCancelIcon fontSize="small" /> },
};

const ManagerOrdersListPage: React.FC = () => {
  const classes = useStyles();
  const history = useHistory();
  const jwt = useJwt();
  const { enqueueSnackbar, closeSnackbar } = useSnackbar();
  const {
    orders, orderTypes, statuses, loading,
    loadFilters, search, createOrder, updateOrder, deleteOrder, changeStatus, loadAequosAvailable,
  } = useOrderManagerAPI();

  const isAdmin = jwt?.role === 'A';

  const [orderType, setOrderType] = useState<string | undefined>(undefined);
  const [dueDateFrom, setDueDateFrom] = useState<string | undefined>(defaultDueDateFrom());
  const [dueDateTo, setDueDateTo] = useState<string | undefined>(undefined);
  const [deliveryDateFrom, setDeliveryDateFrom] = useState<string | undefined>(undefined);
  const [deliveryDateTo, setDeliveryDateTo] = useState<string | undefined>(undefined);
  const [selectedStatuses, setSelectedStatuses] = useState<string[]>(isAdmin ? ['0', '1', '2'] : ['0', '1']);
  const [paid, setPaid] = useState<string | undefined>('');

  const [editOpen, setEditOpen] = useState(false);
  const [closeOpen, setCloseOpen] = useState(false);
  const [confirm, setConfirm] = useState<{ action: string; order: ManagerOrder } | undefined>(undefined);
  const [deleteTarget, setDeleteTarget] = useState<ManagerOrder | undefined>(undefined);
  const [selected, setSelected] = useState<ManagerOrder | undefined>(undefined);
  const [prefill, setPrefill] = useState<OrderPrefill | undefined>(undefined);
  const [filtersOpen, setFiltersOpen] = useState(false);

  const initialised = useRef(false);

  const defaultStatuses = useMemo(() => (isAdmin ? ['0', '1', '2'] : ['0', '1']), [isAdmin]);

  const resetFilters = useCallback(() => {
    setOrderType(undefined);
    setDueDateFrom(defaultDueDateFrom());
    setDueDateTo(undefined);
    setDeliveryDateFrom(undefined);
    setDeliveryDateTo(undefined);
    setSelectedStatuses(defaultStatuses);
    setPaid('');
  }, [defaultStatuses]);

  const buildFilter = useCallback(
    (): OrderSearchFilter => ({
      orderType,
      dueDateFrom: toItDate(dueDateFrom),
      dueDateTo: toItDate(dueDateTo),
      deliveryDateFrom: toItDate(deliveryDateFrom),
      deliveryDateTo: toItDate(deliveryDateTo),
      status: selectedStatuses.map((s) => parseInt(s, 10)),
      paid: paid === '' || paid === undefined ? undefined : paid === 'true',
    }),
    [orderType, dueDateFrom, dueDateTo, deliveryDateFrom, deliveryDateTo, selectedStatuses, paid],
  );

  const doSearch = useCallback(() => search(buildFilter()), [search, buildFilter]);

  useEffect(() => {
    loadFilters();
  }, [loadFilters]);

  const openFromAequos = useCallback((prefillValues: OrderPrefill) => {
    setSelected(undefined);
    setPrefill(prefillValues);
    setEditOpen(true);
  }, []);

  // Initial search + Aequos availability check (once).
  useEffect(() => {
    if (initialised.current) return;
    initialised.current = true;
    doSearch();
    loadAequosAvailable().then((available) => {
      available.forEach((o) => {
        const toIso = (it?: string) => (it ? moment(it, 'DD/MM/YYYY').format('YYYY-MM-DD') : undefined);
        enqueueSnackbar(
          `È disponibile un nuovo ordine di ${o.tipoordine} (consegna ${o.dataconsegna})`,
          {
            variant: 'info',
            persist: true,
            action: (key: SnackbarKey) => (
              <>
                <Button
                  color="inherit"
                  size="small"
                  onClick={() => {
                    closeSnackbar(key);
                    openFromAequos({
                      idtipoordine: o.idtipoordine,
                      dataapertura: toIso(o.dataapertura),
                      datachiusura: toIso(o.datachiusura),
                      dataconsegna: toIso(o.dataconsegna),
                    });
                  }}
                >
                  Aprilo ora
                </Button>
                <Button color="inherit" size="small" onClick={() => closeSnackbar(key)}>
                  Chiudi
                </Button>
              </>
            ),
          },
        );
      });
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const orderTypeOptions = useMemo(
    () => orderTypes.map((t) => ({ value: t.id, label: t.description })),
    [orderTypes],
  );
  const statusOptions = useMemo(
    () => statuses.map((s) => ({ value: s.id, label: s.description })),
    [statuses],
  );

  // Chips summarizing the active filters (shown when the panel is collapsed).
  const activeFilterChips = useMemo(() => {
    const chips: { key: string; label: string; onDelete: () => void }[] = [];

    if (orderType) {
      const name = orderTypes.find((t) => t.id === orderType)?.description ?? orderType;
      chips.push({ key: 'type', label: `Tipologia: ${name}`, onDelete: () => setOrderType(undefined) });
    }
    if (selectedStatuses.length) {
      const names = selectedStatuses
        .map((s) => statuses.find((st) => st.id === s)?.description ?? s)
        .join(', ');
      chips.push({ key: 'status', label: `Stato: ${names}`, onDelete: () => setSelectedStatuses([]) });
    }
    if (dueDateFrom) chips.push({ key: 'ddf', label: `Chiusura dal: ${toItDate(dueDateFrom)}`, onDelete: () => setDueDateFrom(undefined) });
    if (dueDateTo) chips.push({ key: 'ddt', label: `Chiusura al: ${toItDate(dueDateTo)}`, onDelete: () => setDueDateTo(undefined) });
    if (deliveryDateFrom) chips.push({ key: 'cdf', label: `Consegna dal: ${toItDate(deliveryDateFrom)}`, onDelete: () => setDeliveryDateFrom(undefined) });
    if (deliveryDateTo) chips.push({ key: 'cdt', label: `Consegna al: ${toItDate(deliveryDateTo)}`, onDelete: () => setDeliveryDateTo(undefined) });
    if (paid) {
      const name = paidOptions.find((p) => p.value === paid)?.label ?? paid;
      chips.push({ key: 'paid', label: `Evaso: ${name}`, onDelete: () => setPaid('') });
    }
    return chips;
  }, [orderType, orderTypes, selectedStatuses, statuses, dueDateFrom, dueDateTo, deliveryDateFrom, deliveryDateTo, paid]);

  const goToDetails = useCallback(
    (order: ManagerOrder) => history.push(`/legacy/orderdetails?orderId=${order.id}`),
    [history],
  );

  const onNew = useCallback(() => {
    setSelected(undefined);
    setPrefill(undefined);
    setEditOpen(true);
  }, []);

  const closeEdit = useCallback(() => {
    setEditOpen(false);
    setPrefill(undefined);
  }, []);

  const onEdit = useCallback((order: ManagerOrder) => {
    setSelected(order);
    setPrefill(undefined);
    setEditOpen(true);
  }, []);

  const saveOrder = useCallback(
    async (values: OrderInput) => {
      const ok = selected ? await updateOrder(selected.id, values) : await createOrder(values);
      if (ok) doSearch();
      return ok;
    },
    [selected, updateOrder, createOrder, doSearch],
  );

  const runConfirmAction = useCallback(() => {
    if (!confirm) return;
    const mapping = CONFIRM_ACTIONS[confirm.action];
    changeStatus(confirm.order.id, mapping.code).then((ok) => {
      setConfirm(undefined);
      if (ok) doSearch();
    });
  }, [confirm, changeStatus, doSearch]);

  const runDelete = useCallback(() => {
    if (!deleteTarget) return;
    deleteOrder(deleteTarget.id).then((ok) => {
      setDeleteTarget(undefined);
      if (ok) doSearch();
    });
  }, [deleteTarget, deleteOrder, doSearch]);

  const runClose = useCallback(
    (roundType: number) => {
      if (!selected) return;
      changeStatus(selected.id, 'close', roundType).then((ok) => {
        setCloseOpen(false);
        setSelected(undefined);
        if (ok) doSearch();
      });
    },
    [selected, changeStatus, doSearch],
  );

  const triggerAction = useCallback(
    (action: string, order: ManagerOrder) => {
      switch (action) {
        case 'modifica':
          onEdit(order);
          break;
        case 'elimina':
          setDeleteTarget(order);
          break;
        case 'dettaglio':
        case 'gestisci':
          goToDetails(order);
          break;
        case 'chiudi':
          setSelected(order);
          setCloseOpen(true);
          break;
        case 'cancel':
        case 'undocancel':
        case 'riapri':
        case 'contabilizza':
        case 'storna':
          setConfirm({ action, order });
          break;
        default:
          break;
      }
    },
    [onEdit, goToDetails],
  );

  const buildRowActions = useCallback(
    (order: ManagerOrder): RowAction[] => {
      const codes = (order.actions ? order.actions.split(',') : []).map((a) => a.trim()).filter(Boolean);
      return codes
        .filter((a) => ACTION_META[a])
        .map((a) => ({
          key: a,
          label: ACTION_META[a].label,
          icon: ACTION_META[a].icon,
          onClick: () => triggerAction(a, order),
        }));
    },
    [triggerAction],
  );

  const columns: Column[] = useMemo(
    () => [
      { label: 'Tipo Ordine', type: 'String', alignment: 'Left', property: 'tipoordine' },
      { label: 'Data Apertura', type: 'Date', alignment: 'Center', property: 'dataapertura' },
      { label: 'Data Chiusura', type: 'Date', alignment: 'Center', property: 'datachiusura' },
      { label: 'Ora', type: 'Number', alignment: 'Center', property: 'orachiusura' },
      { label: 'Data Consegna', type: 'Date', alignment: 'Center', property: 'dataconsegna' },
      { label: 'Stato', type: 'String', alignment: 'Center', property: 'stato' },
      { label: 'Totale Ordine', type: 'Amount', alignment: 'Right', property: 'totaleordine' },
      { label: 'Totale Fattura', type: 'Amount', alignment: 'Right', property: 'totalefattura' },
      {
        label: 'Inviato',
        type: 'Boolean',
        alignment: 'Center',
        property: 'inviato',
        sortable: false,
        render: (order: ManagerOrder) =>
          order.inviato ? <CheckIcon fontSize="small" color="primary" /> : <span>&ndash;</span>,
      },
      {
        label: 'Pagato',
        type: 'Boolean',
        alignment: 'Center',
        property: 'evaso',
        sortable: false,
        render: (order: ManagerOrder) =>
          order.evaso ? <CheckIcon fontSize="small" color="primary" /> : <span>&ndash;</span>,
      },
      {
        label: 'Azioni',
        type: 'String',
        alignment: 'Right',
        property: 'actions',
        sortable: false,
        render: (order: ManagerOrder) => <RowActionsMenu actions={buildRowActions(order)} />,
      },
    ],
    [buildRowActions],
  );

  const rows = useMemo(() => orders.map((value) => ({ value })), [orders]);

  return (
    <Container maxWidth={false}>
      <PageTitle title="Gestione ordini">
        <Button variant="contained" color="secondary" startIcon={<AddIcon />} onClick={onNew}>
          Nuovo ordine
        </Button>
      </PageTitle>

      <Paper variant="outlined" className={classes.filterPanel}>
        <div className={classes.filterHeader}>
          <div className={classes.filterTitle}>
            <FilterIcon fontSize="small" color="action" />
            <Typography variant="subtitle2">Filtri</Typography>
          </div>
          <div className={classes.chips}>
            {!filtersOpen
              ? activeFilterChips.map((chip) => (
                  <Chip key={chip.key} size="small" label={chip.label} onDelete={chip.onDelete} />
                ))
              : null}
          </div>
          <IconButton size="small" onClick={() => setFiltersOpen((o) => !o)}
            aria-label={filtersOpen ? 'Comprimi filtri' : 'Espandi filtri'}>
            {filtersOpen ? <ExpandLessIcon /> : <ExpandMoreIcon />}
          </IconButton>
        </div>

        <Collapse in={filtersOpen}>
          <div className={classes.filterBody}>
            <Grid container spacing={2} alignItems="flex-end">
              <Grid item xs={12} sm={6} md={4} className={classes.selectField}>
                <Typography variant="caption" color="textSecondary">Tipologia ordine</Typography>
                <SelectField options={orderTypeOptions} value={orderType}
                  onChange={setOrderType} placeholder="Tutte le tipologie..." isClearable />
              </Grid>
              <Grid item xs={12} sm={6} md={4} className={classes.selectField}>
                <Typography variant="caption" color="textSecondary">Stato</Typography>
                <MultiSelectField options={statusOptions} value={selectedStatuses}
                  onChange={setSelectedStatuses} placeholder="Seleziona uno stato..." />
              </Grid>
              <Grid item xs={6} sm={3} md={2}>
                <Typography variant="caption" color="textSecondary">Evaso</Typography>
                <SelectField options={paidOptions} value={paid}
                  placeholder="Tutti" isClearable onChange={(v) => setPaid(v ?? '')} />
              </Grid>
            </Grid>

            {/* Dates on a dedicated row; each dal/al pair wrapped in a box. */}
            <div className={classes.dateGroups}>
              <fieldset className={classes.dateGroup}>
                <legend className={classes.dateLegend}>Data chiusura</legend>
                <div className={classes.rangeRow}>
                  <div className={classes.dateField}>
                    <DatePickerField label="Dal" value={dueDateFrom} onChange={setDueDateFrom} fullWidth={false} />
                  </div>
                  <div className={classes.dateField}>
                    <DatePickerField label="Al" value={dueDateTo} onChange={setDueDateTo} fullWidth={false} />
                  </div>
                </div>
              </fieldset>
              <fieldset className={classes.dateGroup}>
                <legend className={classes.dateLegend}>Data consegna</legend>
                <div className={classes.rangeRow}>
                  <div className={classes.dateField}>
                    <DatePickerField label="Dal" value={deliveryDateFrom} onChange={setDeliveryDateFrom} fullWidth={false} />
                  </div>
                  <div className={classes.dateField}>
                    <DatePickerField label="Al" value={deliveryDateTo} onChange={setDeliveryDateTo} fullWidth={false} />
                  </div>
                </div>
              </fieldset>
            </div>

            <div className={classes.filterActions}>
              <Button startIcon={<ClearIcon />} onClick={resetFilters}>
                Pulisci filtri
              </Button>
              <Button variant="contained" color="primary" startIcon={<SearchIcon />} onClick={doSearch}>
                Ricerca
              </Button>
            </div>
          </div>
        </Collapse>
      </Paper>

      <SortableTable
        columns={columns}
        rows={rows}
        loading={loading}
        settings={{
          canAdd: false,
          canEdit: false,
          canDelete: false,
          showHeader: true,
          showFooter: false,
          pagination: true,
          rowsPerPage: 25,
        }}
      />

      <OrderEditDialog
        open={editOpen}
        order={selected}
        prefill={prefill}
        orderTypes={orderTypes}
        onCancel={closeEdit}
        onSave={saveOrder}
      />

      <CloseOrderDialog
        open={closeOpen}
        onCancel={() => { setCloseOpen(false); setSelected(undefined); }}
        onConfirm={runClose}
      />

      <ActionDialog
        open={!!confirm}
        onCancel={() => setConfirm(undefined)}
        actions={['Ok']}
        onAction={runConfirmAction}
        title={confirm ? CONFIRM_ACTIONS[confirm.action].title : ''}
        message={confirm ? CONFIRM_ACTIONS[confirm.action].message : ''}
      />

      <ActionDialog
        open={!!deleteTarget}
        onCancel={() => setDeleteTarget(undefined)}
        actions={['Ok']}
        onAction={runDelete}
        title="Conferma eliminazione"
        message={`L'ordine ${deleteTarget?.tipoordine ?? ''} verrà cancellato, continuare?`}
      />
    </Container>
  );
};

export default ManagerOrdersListPage;
