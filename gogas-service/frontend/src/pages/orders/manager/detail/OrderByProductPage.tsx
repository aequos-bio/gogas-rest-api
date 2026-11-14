import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Container, Paper, Typography, IconButton, Tooltip, Link, Button, Divider, CircularProgress, Card, CardContent, Fade, Collapse, Menu, MenuItem, ListItemIcon, ListItemText, Switch, Tabs, Tab } from '@material-ui/core';
import {
  CancelSharp as CancelIcon,
  CloseSharp as CloseEditIcon,
  RestoreSharp as RestoreIcon,
  ReplySharp as UndoIcon,
  EuroSharp as PriceIcon,
  AddSharp as AddIcon,
  CallSplitSharp as DistributeIcon,
  SwapHorizSharp as ReplaceIcon,
  PeopleSharp as UsersIcon,
  ArrowBackSharp as BackIcon,
  AssessmentSharp as SmistamentoIcon,
  PublishSharp as UploadIcon,
  PrintSharp as PrintIcon,
  GetAppSharp as ExcelIcon,
  SendSharp as SendIcon,
  FileCopySharp as WeightsIcon,
  SyncSharp as SyncIcon,
  EditSharp as EditIcon,
  FolderSharp as CloseStatusIcon,
  FolderOpenSharp as ReopenIcon,
  AttachMoneySharp as AccountIcon,
  MoneyOffSharp as UndoAccountIcon,
  BlockSharp as CancelOrderIcon,
  RestoreSharp as UndoCancelIcon,
  LocalShippingSharp as SupplierQtyIcon,
  ShoppingBasketSharp as OrderedQtyIcon,
  ArchiveSharp as RemainingQtyIcon,
  FilterListSharp as FilterIcon,
} from '@material-ui/icons';
import { makeStyles, useTheme } from '@material-ui/core/styles';
import { useMediaQuery } from '@material-ui/core';
import { useHistory } from 'react-router-dom';
import queryString from 'query-string';
import useJwt from '../../../../hooks/JwtHooks';
import ToggleButton from '@material-ui/lab/ToggleButton';
import ToggleButtonGroup from '@material-ui/lab/ToggleButtonGroup';
import EditableGridTable, { EditableColumn } from '../../../../components/table/EditableGridTable';
import RowActionsMenu, { RowAction } from '../../../../components/table/RowActionsMenu';
import NumberField from '../../../../components/form/NumberField';
import ActionDialog from '../../../../components/ActionDialog';
import AddUserOrderDialog from './AddUserOrderDialog';
import ModifyPriceDialog from './ModifyPriceDialog';
import ReplaceProductDialog from './ReplaceProductDialog';
import InvoiceDataDialog from './InvoiceDataDialog';
import CloseOrderDialog from '../CloseOrderDialog';
import OrderWorkflowTimeline from './OrderWorkflowTimeline';
import OrderMetaPanel from './OrderMetaPanel';
import { useOrderByProductAPI } from './useOrderByProductAPI';
import { OrderByProduct, OrderItemByProduct, SelectItem } from './types';

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
  // View switch tabs, placed between the actions toolbar and the tables.
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
    flex: '1 1 auto',
    minWidth: 0,
    [theme.breakpoints.down('sm')]: { width: '100%' },
  },
  detail: {
    flex: '0 0 420px',
    minWidth: 0,
    [theme.breakpoints.down('sm')]: { flex: '1 1 auto', width: '100%' },
  },
  detailTitle: {
    display: 'flex', alignItems: 'center', gap: theme.spacing(1),
    padding: theme.spacing(1), fontWeight: 600,
  },
  detailLoader: {
    display: 'flex', justifyContent: 'center', alignItems: 'center',
    padding: theme.spacing(4),
  },
  // ----- mobile card / hero layout -----
  filterToggle: { display: 'flex', justifyContent: 'flex-end', marginBottom: theme.spacing(0.5) },
  cardList: { display: 'flex', flexDirection: 'column', gap: theme.spacing(1.5) },
  card: { borderLeftWidth: 4 },
  cardSelected: { boxShadow: theme.shadows[3] },
  // Remaining-qty tint: green when nothing left, red when there is a remainder.
  cardOk: {
    borderLeftColor: theme.palette.success.main,
    backgroundColor: 'rgba(76, 175, 80, 0.08)',
  },
  cardWarn: {
    borderLeftColor: theme.palette.error.main,
    backgroundColor: 'rgba(244, 67, 54, 0.08)',
  },
  // Cancelled product: neutral grey tint regardless of remaining qty.
  cardCancelled: {
    borderLeftColor: theme.palette.grey[500],
    backgroundColor: 'rgba(0, 0, 0, 0.05)',
  },
  cardHeader: {
    display: 'flex', alignItems: 'center', gap: theme.spacing(0.5),
    marginBottom: theme.spacing(1),
  },
  cardName: { flex: '1 1 auto', minWidth: 0, fontWeight: 700, fontSize: '1rem' },
  // Prominent row with the three key quantities.
  cardMetrics: {
    display: 'flex', gap: theme.spacing(1),
    padding: theme.spacing(1, 0),
    borderTop: `1px solid ${theme.palette.divider}`,
    borderBottom: `1px solid ${theme.palette.divider}`,
  },
  metric: {
    flex: '1 1 0', minWidth: 0,
    display: 'flex', flexDirection: 'column', alignItems: 'center',
    gap: 2, textAlign: 'center',
  },
  metricHead: {
    display: 'inline-flex', alignItems: 'center', gap: 4,
    color: theme.palette.text.secondary,
  },
  metricLabel: { fontSize: '0.68rem', textTransform: 'uppercase', letterSpacing: 0.3 },
  metricValue: { fontSize: '1.25rem', fontWeight: 700, lineHeight: 1.1 },
  metricIcon: { fontSize: '0.95rem' },
  // Smaller secondary info, shown as a wrapped list of chips.
  cardSecondary: {
    display: 'flex', flexWrap: 'wrap', gap: theme.spacing(0.5, 1.5),
    marginTop: theme.spacing(1),
    fontSize: '0.8rem', color: theme.palette.text.secondary,
  },
  cardSecondaryItem: { display: 'inline-flex', alignItems: 'center', gap: 4 },
  cardEdit: {
    marginTop: theme.spacing(1.5),
    display: 'flex', alignItems: 'center', gap: theme.spacing(1),
    maxWidth: 220,
  },
  // ----- per-user detail item cards -----
  itemCard: {},
  itemCardContent: {
    '&:last-child': { paddingBottom: theme.spacing(1.5) },
    paddingTop: theme.spacing(1.5),
  },
  itemCardHeader: {
    display: 'flex', alignItems: 'center', gap: theme.spacing(1),
  },
  itemUser: { flex: '1 1 auto', minWidth: 0, fontWeight: 600 },
  itemOrdered: { whiteSpace: 'nowrap', color: theme.palette.text.secondary, fontSize: '0.85rem' },
  itemWeight: {
    marginTop: theme.spacing(1),
    display: 'flex', alignItems: 'center', gap: theme.spacing(1),
    maxWidth: 280,
  },
  itemWeightLabel: { whiteSpace: 'nowrap', color: theme.palette.text.secondary, fontSize: '0.85rem' },
  green: { color: theme.palette.success.main, fontWeight: 600 },
  orange: { color: theme.palette.warning.main, fontWeight: 600 },
  red: { color: theme.palette.error.main, fontWeight: 600 },
  rowActions: { whiteSpace: 'nowrap' },
  toolbar: {
    display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: theme.spacing(1),
    padding: theme.spacing(1), marginBottom: theme.spacing(2),
  },
  toolbarSpacer: { flex: '1 1 auto' },
  invoicePanel: { padding: theme.spacing(1, 2), marginBottom: theme.spacing(2) },
  invoiceHeader: { display: 'flex', alignItems: 'center', justifyContent: 'space-between' },
  invoiceFields: {
    display: 'flex', flexWrap: 'wrap', gap: theme.spacing(0.5, 3),
    '& > span': { marginRight: theme.spacing(3) },
  },
}));

const euro = (n?: number) =>
  n === undefined || n === null
    ? ''
    : new Intl.NumberFormat('it-IT', { style: 'currency', currency: 'EUR' }).format(n);
const num = (n?: number, d = 2) => (n === undefined || n === null ? '' : n.toFixed(d));

/** Semaphore colour for remaining qty, mirroring the legacy logic. */
const remainingClass = (p: OrderByProduct, classes: ReturnType<typeof useStyles>): string => {
  // Nothing ordered at all (no boxes and no user quantity): remaining is fine, show green.
  if (!p.colliOrdinati && !p.qta) return classes.green;
  if (!p.qta) return classes.red;
  if (Number(p.qtaOrdinata?.toFixed(3)) === Number(p.qta?.toFixed(3))) return classes.green;
  const deviation = Math.abs((p.qtaOrdinata - p.qta) / p.qta);
  if (p.colliOrdinati === 0 || deviation > 0.15) return classes.red;
  return classes.orange;
};

/**
 * Status-transition actions shown in the toolbar (right side). Keyed by the
 * action code the backend emits in OrderDetails.actions; `code` is the value
 * posted to action/{code}. `chiudi` is handled separately (needs roundType).
 */
const STATUS_ACTIONS: Record<
  string,
  { code: string; label: string; confirm?: string }
> = {
  riapri: { code: 'reopen', label: 'Riapri', confirm: "L'ordine verrà riaperto, tutte le modifiche sulle quantità andranno perse. Continuare?" },
  contabilizza: { code: 'contabilizza', label: 'Contabilizza', confirm: "L'ordine verrà contabilizzato e gli importi saranno addebitati ai gasisti. Continuare?" },
  storna: { code: 'tornachiuso', label: 'Storna', confirm: "L'ordine tornerà in stato chiuso, gli importi addebitati saranno stornati. Continuare?" },
  cancel: { code: 'cancel', label: 'Annulla ordine', confirm: "L'ordine verrà annullato. Continuare?" },
  undocancel: { code: 'undocancel', label: 'Ripristina ordine', confirm: "L'ordine verrà ripristinato in stato aperto. Continuare?" },
};

const STATUS_ICONS: Record<string, React.ReactNode> = {
  chiudi: <CloseStatusIcon fontSize="small" />,
  riapri: <ReopenIcon fontSize="small" />,
  contabilizza: <AccountIcon fontSize="small" />,
  storna: <UndoAccountIcon fontSize="small" />,
  cancel: <CancelOrderIcon fontSize="small" />,
  undocancel: <UndoCancelIcon fontSize="small" />,
};

interface ProductCardProps {
  product: OrderByProduct;
  classes: ReturnType<typeof useStyles>;
  /** Order-level flags controlling which fields are shown / editable. */
  synced: boolean;
  isOpen: boolean;
  contabilizzato: boolean;
  colliEditable: boolean;
  /** Whether this card is the one currently being edited (hero mode). */
  editing: boolean;
  onEdit: () => void;
  onExit: () => void;
  /** Commit a new "colli da ordinare" value for this product. */
  onCommitColli: (value: number) => Promise<unknown>;
  /** Per-product actions for the kebab (annulla/ripristina/prezzo). */
  actions: RowAction[];
}

/**
 * Mobile card representation of a by-product summary row. Shows the key fields
 * as label/value pairs; a pencil enters hero edit mode (parent then shows the
 * per-user detail below), and an X returns to the list.
 */
const ProductCard: React.FC<ProductCardProps> = ({
  product: p,
  classes,
  synced,
  isOpen,
  contabilizzato,
  colliEditable,
  editing,
  onEdit,
  onExit,
  onCommitColli,
  actions,
}) => {
  // Local draft of the editable "colli" value, committed via the Apply button.
  const [draftColli, setDraftColli] = useState<number | undefined>(p.colliOrdinati);
  const [committing, setCommitting] = useState(false);
  // Keep the draft in sync when the underlying value changes (e.g. after a reload).
  useEffect(() => setDraftColli(p.colliOrdinati), [p.colliOrdinati]);

  const commitColli = () => {
    if (draftColli === undefined) return;
    setCommitting(true);
    Promise.resolve(onCommitColli(draftColli)).finally(() => setCommitting(false));
  };

  const showColli = !synced && !isOpen;
  // Cancelled products always show the remaining number in green.
  const semaphore = p.annullato ? classes.green : remainingClass(p, classes);

  // The three key quantities, highlighted. Before the order is synced only the
  // user-ordered quantity is known, so the supplier/remaining metrics are hidden.
  const metrics: { label: string; value: string; icon: React.ReactNode; valueCls?: string }[] = [];
  if (synced) {
    metrics.push({ label: 'Ord. prod.', value: num(p.qtaOrdinata), icon: <SupplierQtyIcon className={classes.metricIcon} /> });
    metrics.push({ label: 'Consegn.', value: num(p.qta), icon: <OrderedQtyIcon className={classes.metricIcon} /> });
    metrics.push({ label: 'Riman.', value: num(p.rimanenze), icon: <RemainingQtyIcon className={classes.metricIcon} />, valueCls: semaphore });
  } else {
    metrics.push({ label: 'Ordinata', value: num(p.qta), icon: <OrderedQtyIcon className={classes.metricIcon} /> });
  }

  // Smaller, secondary info.
  const secondary: { label: string; value: React.ReactNode }[] = [
    { label: 'Prezzo', value: euro(p.prezzoKg) },
    { label: 'Peso collo', value: num(p.pesoCollo) },
    { label: 'Pers.', value: `${p.numeroOrdinanti}` },
  ];
  if (synced) {
    secondary.push({ label: 'Colli risult.', value: <span className={semaphore}>{num(p.colliRisultanti)}</span> });
  }
  if (!isOpen) {
    secondary.push({ label: 'Tot. colli', value: euro(p.totaleColli) });
  }
  if (contabilizzato) {
    secondary.push({ label: 'Tot. cons.', value: euro(p.totaleConsegnato) });
    secondary.push({ label: 'Tot. diff.', value: euro(p.totaleDiff) });
  }

  // Card tint follows the remaining-qty number's semaphore: light red only when
  // the number is red; green otherwise (including the yellow/orange tolerance
  // case). Cancelled products stay neutral grey.
  const tintCls = p.annullato
    ? classes.cardCancelled
    : synced
      ? semaphore === classes.red ? classes.cardWarn : classes.cardOk
      : '';

  return (
    <Card
      variant="outlined"
      className={`${classes.card} ${tintCls} ${editing ? classes.cardSelected : ''} ${p.annullato ? 'barrato' : ''}`}
    >
      <CardContent>
        <div className={classes.cardHeader}>
          <Typography className={classes.cardName} noWrap>
            {p.nomeProdotto}
          </Typography>
          {editing && actions.length ? <RowActionsMenu actions={actions} ariaLabel="Azioni prodotto" /> : null}
          {editing ? (
            <Tooltip title="Torna alla lista prodotti">
              <IconButton size="small" onClick={onExit}>
                <CloseEditIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          ) : (
            <Tooltip title="Modifica prodotto">
              <IconButton size="small" onClick={onEdit}>
                <EditIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          )}
        </div>

        <div className={classes.cardMetrics}>
          {metrics.map((m) => (
            <div key={m.label} className={classes.metric}>
              <span className={classes.metricHead}>
                {m.icon}
                <span className={classes.metricLabel}>{m.label}</span>
              </span>
              <span className={`${classes.metricValue} ${m.valueCls ?? ''}`}>{m.value}</span>
            </div>
          ))}
        </div>

        <div className={classes.cardSecondary}>
          {secondary.map((s) => (
            <span key={s.label} className={classes.cardSecondaryItem}>
              {s.label}: <b>{s.value}</b>
            </span>
          ))}
        </div>

        {showColli ? (
          <div className={classes.cardEdit}>
            <NumberField
              label="Colli da ordinare"
              value={draftColli}
              decimals={0}
              min={0}
              disabled={!colliEditable || p.annullato}
              onChange={setDraftColli}
            />
            {colliEditable && !p.annullato && draftColli !== p.colliOrdinati ? (
              <Button
                size="small"
                variant="contained"
                color="primary"
                disabled={committing}
                startIcon={committing ? <CircularProgress size={14} color="inherit" /> : undefined}
                onClick={commitColli}
              >
                Applica
              </Button>
            ) : null}
          </div>
        ) : null}
      </CardContent>
    </Card>
  );
};

interface UserItemCardProps {
  item: OrderItemByProduct;
  classes: ReturnType<typeof useStyles>;
  /** Whether the delivered-weight field is shown and editable. */
  showWeight: boolean;
  weightEditable: boolean;
  onCommitWeight: (value: number) => Promise<unknown>;
  actions: RowAction[];
}

/**
 * Mobile card for a single per-user order line (detail of a product). The user
 * is the title; the ordered qty is shown as info and the delivered weight is
 * the only editable field. The product name is omitted (shown by the hero card).
 */
const UserItemCard: React.FC<UserItemCardProps> = ({
  item: i,
  classes,
  showWeight,
  weightEditable,
  onCommitWeight,
  actions,
}) => {
  const [draft, setDraft] = useState<number | undefined>(i.qtaRitirata);
  const [committing, setCommitting] = useState(false);
  useEffect(() => setDraft(i.qtaRitirata), [i.qtaRitirata]);

  const commitWeight = () => {
    if (draft === undefined) return;
    setCommitting(true);
    Promise.resolve(onCommitWeight(draft)).finally(() => setCommitting(false));
  };

  return (
    <Card
      variant="outlined"
      className={`${classes.itemCard} ${i.annullato ? 'barrato' : ''}`}
    >
      <CardContent className={classes.itemCardContent}>
        <div className={classes.itemCardHeader}>
          <Typography className={classes.itemUser} noWrap>{i.utente}</Typography>
          <span className={classes.itemOrdered}>
            Ord. <b>{num(i.qta)}</b> {i.um}
          </span>
          {actions.length ? <RowActionsMenu actions={actions} ariaLabel="Azioni ordine" /> : null}
        </div>

        {showWeight ? (
          <div className={classes.itemWeight}>
            <span className={classes.itemWeightLabel}>Qta ritirata</span>
            <NumberField
              value={draft}
              decimals={3}
              min={0}
              disabled={!weightEditable || i.annullato}
              onChange={setDraft}
            />
            {weightEditable && !i.annullato && draft !== i.qtaRitirata ? (
              <Button
                size="small"
                variant="contained"
                color="primary"
                disabled={committing}
                startIcon={committing ? <CircularProgress size={14} color="inherit" /> : undefined}
                onClick={commitWeight}
              >
                Applica
              </Button>
            ) : null}
          </div>
        ) : null}
      </CardContent>
    </Card>
  );
};

const OrderByProductPage: React.FC = () => {
  const classes = useStyles();
  const theme = useTheme();
  const history = useHistory();
  const jwt = useJwt();
  const orderId = (queryString.parse(window.location.search).orderId as string) || '';

  const api = useOrderByProductAPI(orderId);
  const {
    details, products, loadingProducts,
    loadDetails, loadProducts, loadProductItems,
    updateOrderedBoxes, updateDeliveredQty, itemAction,
    addUserOrder, updatePrice, distributeRemaining, replaceProduct, loadAvailableUsers,
    exportSmistamentoFile, exportExcel, printUserSheets,
    uploadDeliveryFile, uploadInvoiceAttachment, downloadInvoiceAttachment,
    aequosAction, saveInvoiceData, changeStatus,
  } = api;

  const [selectedProductId, setSelectedProductId] = useState<string | undefined>(undefined);
  const [items, setItems] = useState<OrderItemByProduct[]>([]);
  const [detailLoading, setDetailLoading] = useState(false);
  // Always derive the selected product from the live products array so it never
  // goes stale after a reload.
  const selectedProduct = useMemo(
    () => products.find((p) => p.idProdotto === selectedProductId),
    [products, selectedProductId],
  );

  // dialogs
  const [addOpen, setAddOpen] = useState(false);
  const [availableUsers, setAvailableUsers] = useState<SelectItem[]>([]);
  const [priceOpen, setPriceOpen] = useState(false);
  const [priceProduct, setPriceProduct] = useState<OrderByProduct | undefined>(undefined);
  const [replaceOpen, setReplaceOpen] = useState(false);
  const [replaceItem, setReplaceItem] = useState<OrderItemByProduct | undefined>(undefined);
  const [invoiceOpen, setInvoiceOpen] = useState(false);
  const [closeOpen, setCloseOpen] = useState(false);
  // Anchor for the mobile filter menu (opened by the filter icon).
  const [filterMenuEl, setFilterMenuEl] = useState<HTMLElement | null>(null);
  const [confirm, setConfirm] = useState<{ title: string; message: string; run: () => void } | undefined>(undefined);

  const deliveryFileRef = useRef<HTMLInputElement | null>(null);
  const invoiceFileRef = useRef<HTMLInputElement | null>(null);

  const editable = !!details?.editable && !!details?.totaleCalcolato;
  // An order that is neither Closed (editable) nor Accounted (contabilizzato)
  // is still Open: the ordered-boxes / delivered-qty columns are not meaningful.
  const isOpen = !!details && !details.editable && !details.contabilizzato;
  const synced = details?.sincronizzato != null;

  // "Show only incomplete" filter: an incomplete product is one whose remaining
  // quantity is in the red state (meaningful only once synced). The filter is
  // only offered when the order is synced.
  const [showOnlyIncomplete, setShowOnlyIncomplete] = useState(false);
  const canFilterIncomplete = synced;
  const isIncomplete = useCallback(
    (p: OrderByProduct) => !p.annullato && remainingClass(p, classes) === classes.red,
    [classes],
  );
  const visibleProducts = useMemo(
    () => (canFilterIncomplete && showOnlyIncomplete ? products.filter(isIncomplete) : products),
    [products, canFilterIncomplete, showOnlyIncomplete, isIncomplete],
  );

  useEffect(() => {
    if (!orderId) return;
    loadDetails().then((d) => {
      // For orders whose total is NOT computed (amounts entered manually per
      // user), the by-product view is not meaningful: show the by-user page.
      if (d && !d.totaleCalcolato) {
        history.replace(`/legacy/orderdetailsbyuser?orderId=${orderId}`);
        return;
      }
      loadProducts();
    });
  }, [orderId, loadDetails, loadProducts, history]);

  // Mirror the selected id in a ref so refresh helpers always read the current
  // selection regardless of closure timing.
  const selectedIdRef = useRef<string | undefined>(undefined);
  useEffect(() => {
    selectedIdRef.current = selectedProductId;
  }, [selectedProductId]);

  // Scroll position of the product list when entering hero edit mode, and a
  // flag asking to restore it once we're back on the list.
  const listScrollRef = useRef(0);
  const restoreScrollRef = useRef(false);
  useEffect(() => {
    if (!selectedProductId && restoreScrollRef.current) {
      restoreScrollRef.current = false;
      // Wait for the list to paint before scrolling back.
      requestAnimationFrame(() => window.scrollTo({ top: listScrollRef.current }));
    }
  }, [selectedProductId]);

  const reloadDetail = useCallback(() => {
    const id = selectedIdRef.current;
    if (id) {
      loadProductItems(id).then(setItems);
    }
  }, [loadProductItems]);

  // Reload both the summary rows and the currently-open detail items.
  const refreshProducts = useCallback(() => {
    loadProducts();
    reloadDetail();
  }, [loadProducts, reloadDetail]);

  const selectProduct = useCallback(
    (product: OrderByProduct) => {
      // Remember the list scroll position so we can restore it when the user
      // leaves hero edit mode (mobile), but only when entering from the list.
      if (!selectedIdRef.current) {
        listScrollRef.current = window.scrollY;
      }
      setSelectedProductId(product.idProdotto);
      selectedIdRef.current = product.idProdotto;
      // Clear and show a loader so the previous product's rows aren't shown
      // morphing into the new ones while loading.
      setItems([]);
      setDetailLoading(true);
      loadProductItems(product.idProdotto)
        .then(setItems)
        .finally(() => setDetailLoading(false));
    },
    [loadProductItems],
  );

  // Clear the current selection (used by the mobile "X" to leave hero edit mode).
  const clearSelection = useCallback(() => {
    setSelectedProductId(undefined);
    selectedIdRef.current = undefined;
    setItems([]);
    // Restore the list scroll position after the list has re-rendered.
    restoreScrollRef.current = true;
  }, []);

  // ----- summary grid (by product) -----
  const summaryColumns: EditableColumn<OrderByProduct>[] = useMemo(() => {
    const synced = details?.sincronizzato != null;
    const cols: EditableColumn<OrderByProduct>[] = [
      { id: 'nomeProdotto', label: 'Prodotto', align: 'left', render: (p) => p.nomeProdotto },
      { id: 'prezzoKg', label: 'Prezzo', align: 'right', render: (p) => euro(p.prezzoKg) },
      { id: 'pesoCollo', label: 'Peso Collo', align: 'right', render: (p) => num(p.pesoCollo) },
    ];

    if (synced) {
      cols.push({ id: 'qtaOrdinata', label: 'Qta ord. produtt.', align: 'right', render: (p) => num(p.qtaOrdinata), footer: (rs) => num(rs.reduce((s, p) => s + p.qtaOrdinata, 0)) });
    } else if (!isOpen) {
      // "Colli da ordinare" / "Colli risultanti" only make sense once the order
      // is closed (supplier order built); hide them while the order is open.
      cols.push({
        id: 'colliOrdinati',
        label: 'Colli da ordinare',
        align: 'right',
        width: 120,
        editable: true,
        decimals: 0,
        getValue: (p) => p.colliOrdinati,
        canEdit: (p) => editable && !p.annullato,
        footer: (rs) => num(rs.reduce((s, p) => s + (p.colliOrdinati || 0), 0), 0),
      });
      cols.push({ id: 'colliRisultanti', label: 'Colli Risult.', align: 'right', render: (p) => (
        <span className={remainingClass(p, classes)}>{num(p.colliRisultanti)}</span>
      ) });
    }

    cols.push({ id: 'qta', label: synced ? 'Qta cons.' : 'Qta ord.', align: 'right', render: (p) => num(p.qta) });
    cols.push({
      id: 'rimanenze', label: 'Qta Rim.', align: 'right',
      render: (p) => <span className={synced ? remainingClass(p, classes) : undefined}>{num(p.rimanenze)}</span>,
    });
    cols.push({ id: 'numeroOrdinanti', label: 'Pers.', align: 'right', render: (p) => `${p.numeroOrdinanti}` });
    if (!isOpen) {
      cols.push({ id: 'totaleColli', label: 'Totale Colli', align: 'right', render: (p) => euro(p.totaleColli), footer: (rs) => euro(rs.reduce((s, p) => s + p.totaleColli, 0)) });
    }

    if (details?.contabilizzato) {
      cols.push({ id: 'totaleConsegnato', label: 'Totale Cons.', align: 'right', render: (p) => euro(p.totaleConsegnato), footer: (rs) => euro(rs.reduce((s, p) => s + p.totaleConsegnato, 0)) });
      cols.push({ id: 'totaleDiff', label: 'Totale Diff.', align: 'right', render: (p) => euro(p.totaleDiff), footer: (rs) => euro(rs.reduce((s, p) => s + p.totaleDiff, 0)) });
    }

    // Filter toggle shown in the trailing (actions) column header on desktop.
    const filterHeader = canFilterIncomplete ? (
      <Tooltip title={showOnlyIncomplete ? 'Mostra tutti i prodotti' : 'Mostra solo i prodotti da completare'}>
        <IconButton
          size="small"
          onClick={() => setShowOnlyIncomplete((v) => !v)}
          style={showOnlyIncomplete ? { color: theme.palette.error.main } : undefined}
        >
          <FilterIcon fontSize="small" />
        </IconButton>
      </Tooltip>
    ) : '';

    if (editable) {
      cols.push({
        id: '_actions', label: filterHeader, align: 'right', width: 48,
        render: (p) => (
          <RowActionsMenu
            actions={[
              {
                key: 'toggle',
                label: p.annullato ? 'Ripristina ordine prodotto' : 'Annulla ordine prodotto',
                icon: p.annullato ? <RestoreIcon fontSize="small" /> : <CancelIcon fontSize="small" />,
                onClick: () => onProductAction(p, p.annullato ? 'restore' : 'cancel'),
              },
              {
                key: 'undo',
                label: 'Ripristina quantità originali',
                icon: <UndoIcon fontSize="small" />,
                onClick: () => onProductAction(p, 'restore'),
              },
              {
                key: 'price',
                label: 'Modifica prezzo',
                icon: <PriceIcon fontSize="small" />,
                onClick: () => { setPriceProduct(p); setPriceOpen(true); },
              },
            ]}
          />
        ),
      });
    } else if (canFilterIncomplete) {
      // Read-only order: still offer the filter toggle in a trailing header-only column.
      cols.push({ id: '_filter', label: filterHeader, align: 'right', width: 48, render: () => null });
    }

    return cols;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [details, editable, classes, theme, canFilterIncomplete, showOnlyIncomplete]);

  // ----- detail grid (per product) -----
  const detailColumns: EditableColumn<OrderItemByProduct>[] = useMemo(() => {
    const cols: EditableColumn<OrderItemByProduct>[] = [
      { id: 'utente', label: 'Utente', align: 'left', render: (i) => i.utente },
      { id: 'um', label: 'U.M.', align: 'center', render: (i) => i.um },
      { id: 'qta', label: 'Qta Ord.', align: 'right', render: (i) => num(i.qta) },
    ];
    // "Qta Rit." (delivered) is only meaningful once the order is closed.
    if (!isOpen) {
      cols.push({
        id: 'qtaRitirata', label: 'Qta Rit.', align: 'right', width: 90,
        editable: true, decimals: 3,
        getValue: (i) => i.qtaRitirata,
        canEdit: (i) => editable && !i.annullato,
      });
    }
    if (editable) {
      cols.push({
        id: '_actions', label: '', align: 'right', width: 48,
        render: (i) => (
          <RowActionsMenu
            actions={[
              {
                key: 'toggle',
                label: i.annullato ? "Ripristina l'ordine" : "Annulla l'ordine",
                icon: i.annullato ? <RestoreIcon fontSize="small" /> : <CancelIcon fontSize="small" />,
                onClick: () => onItemAction(i, i.annullato ? 'restore' : 'cancel'),
              },
              {
                key: 'undo',
                label: 'Ripristina quantità originale',
                icon: <UndoIcon fontSize="small" />,
                onClick: () => onItemAction(i, 'restore'),
              },
              {
                key: 'replace',
                label: 'Sostituisci prodotto',
                icon: <ReplaceIcon fontSize="small" />,
                onClick: () => { setReplaceItem(i); setReplaceOpen(true); },
              },
            ]}
          />
        ),
      });
    }
    return cols;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [editable, isOpen, classes]);

  // ----- commit handlers for editable cells -----
  const onSummaryCommit = useCallback(
    async (product: OrderByProduct, _col: string, value: number) => {
      const ok = await updateOrderedBoxes(product.idProdotto, value);
      if (ok) refreshProducts();
      return ok;
    },
    [updateOrderedBoxes, refreshProducts],
  );

  const onDetailCommit = useCallback(
    async (item: OrderItemByProduct, _col: string, value: number) => {
      const ok = await updateDeliveredQty(item.idOrdine, value);
      if (ok) refreshProducts();
      return ok;
    },
    [updateDeliveredQty, refreshProducts],
  );

  // ----- actions -----
  const onProductAction = useCallback(
    (product: OrderByProduct, action: 'cancel' | 'restore') => {
      const msg =
        action === 'cancel'
          ? `Annullare l'ordine del prodotto ${product.nomeProdotto}?`
          : `Ripristinare l'ordine del prodotto ${product.nomeProdotto}?`;
      setConfirm({
        title: 'Conferma',
        message: msg,
        run: () => {
          itemAction('product', product.idProdotto, action).then((ok) => {
            setConfirm(undefined);
            if (ok) refreshProducts();
          });
        },
      });
    },
    [itemAction, refreshProducts],
  );

  // Per-product kebab actions shared by the desktop table row and the mobile
  // card (annulla/ripristina ordine, ripristina quantità, modifica prezzo).
  const cardActionsFor = useCallback(
    (p: OrderByProduct): RowAction[] => [
      {
        key: 'toggle',
        label: p.annullato ? 'Ripristina ordine prodotto' : 'Annulla ordine prodotto',
        icon: p.annullato ? <RestoreIcon fontSize="small" /> : <CancelIcon fontSize="small" />,
        onClick: () => onProductAction(p, p.annullato ? 'restore' : 'cancel'),
      },
      {
        key: 'undo',
        label: 'Ripristina quantità originali',
        icon: <UndoIcon fontSize="small" />,
        onClick: () => onProductAction(p, 'restore'),
      },
      {
        key: 'price',
        label: 'Modifica prezzo',
        icon: <PriceIcon fontSize="small" />,
        onClick: () => { setPriceProduct(p); setPriceOpen(true); },
      },
    ],
    [onProductAction],
  );

  const onItemAction = useCallback(
    (item: OrderItemByProduct, action: 'cancel' | 'restore') => {
      const msg =
        action === 'cancel'
          ? `Annullare l'ordine di ${item.utente}?`
          : `Ripristinare l'ordine di ${item.utente}?`;
      setConfirm({
        title: 'Conferma',
        message: msg,
        run: () => {
          itemAction('item', item.idOrdine, action).then((ok) => {
            setConfirm(undefined);
            if (ok) refreshProducts();
          });
        },
      });
    },
    [itemAction, refreshProducts],
  );

  // Per-user-item kebab actions (cancel/restore order, undo qty, replace product).
  const itemActionsFor = useCallback(
    (i: OrderItemByProduct): RowAction[] => [
      {
        key: 'toggle',
        label: i.annullato ? "Ripristina l'ordine" : "Annulla l'ordine",
        icon: i.annullato ? <RestoreIcon fontSize="small" /> : <CancelIcon fontSize="small" />,
        onClick: () => onItemAction(i, i.annullato ? 'restore' : 'cancel'),
      },
      {
        key: 'undo',
        label: 'Ripristina quantità originale',
        icon: <UndoIcon fontSize="small" />,
        onClick: () => onItemAction(i, 'restore'),
      },
      {
        key: 'replace',
        label: 'Sostituisci prodotto',
        icon: <ReplaceIcon fontSize="small" />,
        onClick: () => { setReplaceItem(i); setReplaceOpen(true); },
      },
    ],
    [onItemAction],
  );

  const openAddDialog = useCallback(() => {
    if (!selectedProduct) return;
    loadAvailableUsers(selectedProduct.idProdotto).then((users) => {
      setAvailableUsers(users);
      setAddOpen(true);
    });
  }, [selectedProduct, loadAvailableUsers]);

  const onAddSave = useCallback(
    async (userId: string, qta: number) => {
      if (!selectedProduct) return false;
      const ok = await addUserOrder(selectedProduct.idProdotto, userId, selectedProduct.umProdotto, qta);
      if (ok) refreshProducts();
      return ok;
    },
    [selectedProduct, addUserOrder, refreshProducts],
  );

  const onPriceSave = useCallback(
    async (price: number) => {
      if (!priceProduct) return false;
      const ok = await updatePrice(priceProduct.idProdotto, price);
      if (ok) refreshProducts();
      return ok;
    },
    [priceProduct, updatePrice, refreshProducts],
  );

  const onReplaceSave = useCallback(
    async (targetProductId: string) => {
      if (!replaceItem) return false;
      const ok = await replaceProduct(replaceItem.idOrdine, targetProductId);
      if (ok) refreshProducts();
      return ok;
    },
    [replaceItem, replaceProduct, refreshProducts],
  );

  const onDistribute = useCallback(() => {
    if (!selectedProduct) return;
    setConfirm({
      title: 'Conferma',
      message: 'Le quantità verranno ridistribuite per raggiungere il numero di colli, continuare?',
      run: () => {
        distributeRemaining(selectedProduct.idProdotto).then((ok) => {
          setConfirm(undefined);
          if (ok) refreshProducts();
        });
      },
    });
  }, [selectedProduct, distributeRemaining, refreshProducts]);

  // ----- toolbar / invoice handlers -----
  const onDeliveryFileSelected = useCallback(
    (event: React.ChangeEvent<HTMLInputElement>) => {
      const file = event.target.files?.[0];
      event.target.value = '';
      if (!file) return;
      uploadDeliveryFile(file).then((ok) => {
        if (ok) refreshProducts();
      });
    },
    [uploadDeliveryFile, refreshProducts],
  );

  const onInvoiceFileSelected = useCallback(
    (event: React.ChangeEvent<HTMLInputElement>) => {
      const file = event.target.files?.[0];
      event.target.value = '';
      if (!file) return;
      uploadInvoiceAttachment(file).then((ok) => {
        if (ok) loadDetails();
      });
    },
    [uploadInvoiceAttachment, loadDetails],
  );

  const confirmAequos = useCallback(
    (action: 'send' | 'synch' | 'weights', message: string) => {
      setConfirm({
        title: 'Conferma',
        message,
        run: () => {
          aequosAction(action).then((ok) => {
            setConfirm(undefined);
            if (ok) {
              loadDetails();
              refreshProducts();
            }
          });
        },
      });
    },
    [aequosAction, loadDetails, refreshProducts],
  );

  const onSaveInvoice = useCallback(
    async (data: Parameters<typeof saveInvoiceData>[0]) => {
      const ok = await saveInvoiceData(data);
      if (ok) loadDetails();
      return ok;
    },
    [saveInvoiceData, loadDetails],
  );

  // Status transitions permitted for this order (from the backend), limited to
  // the workflow actions that make sense on the detail page.
  const statusActions = useMemo(() => {
    const all = (details?.actions ? details.actions.split(',') : []).map((a) => a.trim());
    return all.filter((a) => a === 'chiudi' || !!STATUS_ACTIONS[a]);
  }, [details]);

  const afterStatusChange = useCallback(
    (ok: boolean) => {
      if (ok) {
        loadDetails();
        refreshProducts();
      }
    },
    [loadDetails, refreshProducts],
  );

  const onStatusAction = useCallback(
    (action: string) => {
      if (action === 'chiudi') {
        setCloseOpen(true);
        return;
      }
      const mapping = STATUS_ACTIONS[action];
      if (!mapping) return;
      setConfirm({
        title: 'Conferma',
        message: mapping.confirm ?? 'Confermare l\'operazione?',
        run: () => {
          changeStatus(mapping.code).then((ok) => {
            setConfirm(undefined);
            afterStatusChange(ok);
          });
        },
      });
    },
    [changeStatus, afterStatusChange],
  );

  const runClose = useCallback(
    (roundType: number) => {
      changeStatus('close', roundType).then((ok) => {
        setCloseOpen(false);
        afterStatusChange(ok);
      });
    },
    [changeStatus, afterStatusChange],
  );

  const isAdmin = jwt?.role === 'A';
  const canSendAequos = editable && details?.invioOrdineConsentito;
  const canSendWeights = !!details?.invioPesiRichiesto && !!details?.invioPesiConsentito;
  const canSync =
    (editable || !!details?.contabilizzato) &&
    details?.idaequos != null &&
    !!details?.inviato &&
    details?.numeroOrdineEsterno != null;

  // On phones the toolbar's non-status actions don't fit as a button row, so
  // they collapse into a kebab menu; the status-transition buttons stay visible.
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));
  const toolbarActions = useMemo<RowAction[]>(() => {
    const list: RowAction[] = [];
    if (editable) {
      list.push(
        { key: 'smistamento', label: 'Esporta per smistamento', icon: <SmistamentoIcon fontSize="small" />, onClick: exportSmistamentoFile },
        { key: 'updateQty', label: 'Aggiorna quantità', icon: <UploadIcon fontSize="small" />, onClick: () => deliveryFileRef.current?.click() },
        { key: 'sheetsPdf', label: 'Schede utenti', icon: <PrintIcon fontSize="small" />, onClick: () => printUserSheets('pdf') },
        { key: 'sheetsXls', label: 'Schede utenti XLS', icon: <PrintIcon fontSize="small" />, onClick: () => printUserSheets('excel') },
        { key: 'excel', label: 'Export in excel', icon: <ExcelIcon fontSize="small" />, onClick: exportExcel },
      );
      if (canSendAequos) {
        list.push({ key: 'send', label: 'Invia ad aequos', icon: <SendIcon fontSize="small" />, onClick: () => confirmAequos('send', "L'ordine verrà automaticamente inviato ad Aequos, continuare?") });
      }
    }
    if (canSendWeights) {
      list.push({ key: 'weights', label: 'Invia pesi ad aequos', icon: <WeightsIcon fontSize="small" />, onClick: () => confirmAequos('weights', "I pesi dell'ordine verranno inviati ad Aequos, continuare?") });
    }
    if (canSync) {
      list.push({ key: 'sync', label: 'Sincronizza ordine aequos', icon: <SyncIcon fontSize="small" />, onClick: () => confirmAequos('synch', 'Le quantità dei colli ordinati ad Aequos verranno sincronizzate, continuare?') });
    }
    return list;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [editable, canSendAequos, canSendWeights, canSync]);

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
              value="product"
              onChange={(_e, v) => {
                if (v === 'user') history.push(`/legacy/orderdetailsbyuser?orderId=${orderId}`);
              }}
            >
              <ToggleButton value="product" className={classes.toggleButton}>
                <OrderedQtyIcon fontSize="small" className={classes.toggleIcon} />
                Per prodotto
              </ToggleButton>
              <ToggleButton value="user" className={classes.toggleButton}>
                <UsersIcon fontSize="small" className={classes.toggleIcon} />
                Per utente
              </ToggleButton>
            </ToggleButtonGroup>
          ) : null}
        </div>
      </Paper>

      <OrderMetaPanel details={details} />

      <OrderWorkflowTimeline details={details} />

      <Paper className={classes.toolbar} variant="outlined">
        {isMobile ? null : (
          <>
            {editable ? (
              <>
                <Button size="small" startIcon={<SmistamentoIcon />} onClick={exportSmistamentoFile}>
                  Esporta per smistamento
                </Button>
                <Button size="small" startIcon={<UploadIcon />} onClick={() => deliveryFileRef.current?.click()}>
                  Aggiorna quantità
                </Button>
                <Button size="small" startIcon={<PrintIcon />} onClick={() => printUserSheets('pdf')}>
                  Schede utenti
                </Button>
                <Button size="small" startIcon={<PrintIcon />} onClick={() => printUserSheets('excel')}>
                  Schede utenti XLS
                </Button>
                <Button size="small" startIcon={<ExcelIcon />} onClick={exportExcel}>
                  Export in excel
                </Button>
                {canSendAequos ? (
                  <Button
                    size="small"
                    startIcon={<SendIcon />}
                    onClick={() =>
                      confirmAequos('send', "L'ordine verrà automaticamente inviato ad Aequos, continuare?")
                    }
                  >
                    Invia ad aequos
                  </Button>
                ) : null}
              </>
            ) : null}

            {canSendWeights ? (
              <Button
                size="small"
                startIcon={<WeightsIcon />}
                onClick={() =>
                  confirmAequos('weights', "I pesi dell'ordine verranno inviati ad Aequos, continuare?")
                }
              >
                Invia pesi ad aequos
              </Button>
            ) : null}

            {canSync ? (
              <Button
                size="small"
                startIcon={<SyncIcon />}
                onClick={() =>
                  confirmAequos('synch', 'Le quantità dei colli ordinati ad Aequos verranno sincronizzate, continuare?')
                }
              >
                Sincronizza ordine aequos
              </Button>
            ) : null}
          </>
        )}
        {statusActions.length || (isMobile && toolbarActions.length) ? (
          <>
            {/* On desktop push the status group to the right; on mobile keep it
                left-aligned next to the rest. */}
            {isMobile ? null : <div className={classes.toolbarSpacer} />}
            {statusActions.length ? (
              <>
                {isMobile ? null : <Divider orientation="vertical" flexItem />}
                {statusActions.map((a) => (
                  <Button
                    key={a}
                    size="small"
                    variant="outlined"
                    color="primary"
                    startIcon={STATUS_ICONS[a]}
                    onClick={() => onStatusAction(a)}
                  >
                    {a === 'chiudi' ? 'Chiudi' : STATUS_ACTIONS[a].label}
                  </Button>
                ))}
              </>
            ) : null}
            {/* On mobile the non-status actions live in a kebab to the right of
                the status buttons. */}
            {isMobile ? (
              <RowActionsMenu actions={toolbarActions} ariaLabel="Altre azioni" />
            ) : null}
          </>
        ) : null}
      </Paper>

      {details?.contabilizzato ? (
        <Paper className={classes.invoicePanel} variant="outlined">
          <div className={classes.invoiceHeader}>
            <Typography variant="subtitle2">Dati contabili</Typography>
            {isAdmin ? (
              <Tooltip title="Modifica i dati contabili">
                <IconButton size="small" onClick={() => setInvoiceOpen(true)}>
                  <EditIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            ) : null}
          </div>
          <div className={classes.invoiceFields}>
            <span><b>Numero fattura:</b> {details.numerofattura ?? '-'}</span>
            <span><b>Importo fattura:</b> {details.totalefattura != null ? euro(details.totalefattura) : '-'}</span>
            <span><b>Data fattura:</b> {details.datafattura ?? '-'}</span>
            <span><b>Pagato:</b> {details.evaso ? 'Sì' : 'No'}</span>
            <span><b>Data pagamento:</b> {details.datapagamento ?? '-'}</span>
            <span>
              <b>Allegato:</b>{' '}
              {details.hasAttachment ? (
                <Link component="button" variant="body2" onClick={() => downloadInvoiceAttachment()}>Sì</Link>
              ) : (
                <>
                  No{' '}
                  <Tooltip title="Carica l'allegato della fattura">
                    <IconButton size="small" onClick={() => invoiceFileRef.current?.click()}>
                      <UploadIcon fontSize="small" />
                    </IconButton>
                  </Tooltip>
                </>
              )}
            </span>
          </div>
        </Paper>
      ) : null}

      <input ref={deliveryFileRef} type="file" accept=".xls,.xlsx" hidden onChange={onDeliveryFileSelected} />
      <input ref={invoiceFileRef} type="file" hidden onChange={onInvoiceFileSelected} />

      {isMobile ? (
        <Tabs
          className={classes.viewTabs}
          value="product"
          indicatorColor="primary"
          textColor="primary"
          variant="fullWidth"
          onChange={(_e, v) => {
            if (v === 'user') history.push(`/legacy/orderdetailsbyuser?orderId=${orderId}`);
          }}
        >
          <Tab value="product" icon={<OrderedQtyIcon fontSize="small" />} label="Per prodotto" />
          <Tab value="user" icon={<UsersIcon fontSize="small" />} label="Per utente" />
        </Tabs>
      ) : null}

      {isMobile ? (
        <>
          {/* Filter icon opening a menu with the "show only incomplete" option
              (hidden in hero mode and when the order isn't synced). */}
          {canFilterIncomplete && !selectedProduct ? (
            <div className={classes.filterToggle}>
              <Button
                size="small"
                startIcon={<FilterIcon fontSize="small" />}
                onClick={(e) => setFilterMenuEl(e.currentTarget)}
                style={showOnlyIncomplete ? { color: theme.palette.error.main } : undefined}
              >
                Filtri
              </Button>
              <Menu
                anchorEl={filterMenuEl}
                open={Boolean(filterMenuEl)}
                onClose={() => setFilterMenuEl(null)}
              >
                <MenuItem onClick={() => setShowOnlyIncomplete((v) => !v)}>
                  <ListItemIcon style={{ minWidth: 36 }}>
                    <Switch
                      edge="start"
                      size="small"
                      color="primary"
                      checked={showOnlyIncomplete}
                      onClick={(e) => e.stopPropagation()}
                      onChange={(e) => setShowOnlyIncomplete(e.target.checked)}
                    />
                  </ListItemIcon>
                  <ListItemText>Solo prodotti da completare</ListItemText>
                </MenuItem>
              </Menu>
            </div>
          ) : null}

          {/* Card list. In hero mode (a product selected) only the chosen card
              is kept; otherwise the full list is shown and the detail is hidden.
              Keyed + faded so switching between list and hero cross-fades. */}
          <Fade in key={selectedProductId ?? 'all'} timeout={200}>
          <div className={classes.cardList}>
            {loadingProducts ? (
              <Typography variant="body2" color="textSecondary">Caricamento...</Typography>
            ) : visibleProducts.length === 0 ? (
              <Typography variant="body2" color="textSecondary">
                {showOnlyIncomplete ? 'Nessun prodotto da completare' : 'Nessun prodotto'}
              </Typography>
            ) : (
              (selectedProduct ? [selectedProduct] : visibleProducts).map((p) => (
                <ProductCard
                  key={p.idProdotto}
                  product={p}
                  classes={classes}
                  synced={details?.sincronizzato != null}
                  isOpen={isOpen}
                  contabilizzato={!!details?.contabilizzato}
                  colliEditable={editable}
                  editing={selectedProductId === p.idProdotto}
                  onEdit={() => selectProduct(p)}
                  onExit={clearSelection}
                  onCommitColli={(value) => onSummaryCommit(p, 'colliOrdinati', value)}
                  actions={
                    !editable
                      ? []
                      : selectedProductId === p.idProdotto
                        // On the selected (hero) card, the detail-level actions
                        // (add user / distribute remaining) join the kebab.
                        ? [
                            ...cardActionsFor(p),
                            { key: 'addUser', label: 'Aggiungi ordine utente', icon: <AddIcon fontSize="small" />, onClick: openAddDialog },
                            { key: 'distribute', label: 'Distribuisci quantità rimanenti', icon: <DistributeIcon fontSize="small" />, onClick: onDistribute },
                          ]
                        : cardActionsFor(p)
                  }
                />
              ))
            )}
          </div>
          </Fade>

          <Collapse in={!!selectedProduct} timeout={250} unmountOnExit>
            <div className={classes.cardList} style={{ marginTop: 16 }}>
              {detailLoading ? (
                <div className={classes.detailLoader}>
                  <CircularProgress size={28} />
                </div>
              ) : items.length === 0 ? (
                <Typography variant="body2" color="textSecondary">Nessun ordine</Typography>
              ) : (
                items.map((i) => (
                  <UserItemCard
                    key={i.idOrdine}
                    item={i}
                    classes={classes}
                    showWeight={!isOpen}
                    weightEditable={editable}
                    onCommitWeight={(value) => onDetailCommit(i, 'qtaRitirata', value)}
                    actions={editable ? itemActionsFor(i) : []}
                  />
                ))
              )}
            </div>
          </Collapse>
        </>
      ) : (
        <div className={classes.grids}>
          <Paper className={classes.summary} variant="outlined">
            <EditableGridTable
              columns={summaryColumns}
              rows={visibleProducts}
              rowId={(p) => p.idProdotto}
              onCommit={onSummaryCommit}
              rowClassName={(p) => (p.annullato ? 'barrato' : undefined)}
              onRowClick={selectProduct}
              selectedRowId={selectedProductId}
              emptyMessage={
                loadingProducts
                  ? 'Caricamento...'
                  : showOnlyIncomplete
                    ? 'Nessun prodotto da completare'
                    : 'Nessun prodotto'
              }
            />
          </Paper>

          <Paper className={classes.detail} variant="outlined">
            <div className={classes.detailTitle}>
              <span>{selectedProduct ? selectedProduct.nomeProdotto : 'Seleziona un prodotto'}</span>
              {editable && selectedProduct ? (
                <>
                  <Tooltip title="Aggiungi ordine utente">
                    <IconButton size="small" onClick={openAddDialog}><AddIcon fontSize="small" /></IconButton>
                  </Tooltip>
                  <Tooltip title="Distribuisci le quantità rimanenti">
                    <IconButton size="small" onClick={onDistribute}><DistributeIcon fontSize="small" /></IconButton>
                  </Tooltip>
                </>
              ) : null}
            </div>
            {selectedProduct ? (
              detailLoading ? (
                <div className={classes.detailLoader}>
                  <CircularProgress size={28} />
                </div>
              ) : (
                <EditableGridTable
                  columns={detailColumns}
                  rows={items}
                  rowId={(i) => i.idOrdine}
                  onCommit={onDetailCommit}
                  rowClassName={(i) => (i.annullato ? 'barrato' : undefined)}
                  emptyMessage="Nessun ordine"
                />
              )
            ) : null}
          </Paper>
        </div>
      )}

      <AddUserOrderDialog
        open={addOpen}
        product={selectedProduct}
        availableUsers={availableUsers}
        onCancel={() => setAddOpen(false)}
        onSave={onAddSave}
      />
      <ModifyPriceDialog
        open={priceOpen}
        product={priceProduct}
        onCancel={() => setPriceOpen(false)}
        onSave={onPriceSave}
      />
      <ReplaceProductDialog
        open={replaceOpen}
        item={replaceItem}
        products={products}
        currentProductId={selectedProduct?.idProdotto}
        onCancel={() => setReplaceOpen(false)}
        onSave={onReplaceSave}
      />
      <InvoiceDataDialog
        open={invoiceOpen}
        details={details}
        aequosLocked={details?.idaequos != null}
        onCancel={() => setInvoiceOpen(false)}
        onSave={onSaveInvoice}
      />
      <CloseOrderDialog
        open={closeOpen}
        onCancel={() => setCloseOpen(false)}
        onConfirm={runClose}
      />
      <ActionDialog
        open={!!confirm}
        onCancel={() => setConfirm(undefined)}
        actions={['Ok']}
        onAction={() => confirm?.run()}
        title={confirm?.title ?? ''}
        message={confirm?.message ?? ''}
      />
    </Container>
  );
};

export default OrderByProductPage;
