import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Container, Grid, Button, Typography, Checkbox } from '@material-ui/core';
import {
  Search as SearchIcon,
  Sync as SyncIcon,
  Publish as ImportIcon,
  GetApp as ExportIcon,
  AddSharp as AddIcon,
} from '@material-ui/icons';
import { makeStyles } from '@material-ui/core/styles';
import moment from 'moment-timezone';
import PageTitle from '../../../components/PageTitle';
import SortableTable, { Column } from '../../../components/table/SortableTable';
import SelectField from '../../../components/form/SelectField';
import ActionDialog from '../../../components/ActionDialog';
import ProductEditDialog from './ProductEditDialog';
import { useProductsAPI } from './useProductsAPI';
import { Product, ProductInput, SelectItem } from './types';

const useStyles = makeStyles((theme) => ({
  filter: { marginBottom: theme.spacing(2) },
  toolbar: {
    display: 'flex',
    flexWrap: 'wrap',
    alignItems: 'center',
    gap: theme.spacing(1),
    justifyContent: 'flex-end',
    marginBottom: theme.spacing(2),
  },
  lastSync: { color: theme.palette.text.secondary, marginRight: theme.spacing(1) },
}));

const availableOptions = [
  { value: '', label: 'Tutti' },
  { value: 'true', label: 'Ordinabili' },
  { value: 'false', label: 'Non ordinabili' },
];
const cancelledOptions = [
  { value: '', label: 'Tutti' },
  { value: 'true', label: 'Sì' },
  { value: 'false', label: 'No' },
];

const baseColumns: Column[] = [
  { label: 'Descrizione', type: 'String', alignment: 'Left', property: 'descrizione' },
  { label: 'U.M.', type: 'String', alignment: 'Center', property: 'unitamisura' },
  { label: 'Prezzo unità', type: 'Amount', alignment: 'Right', property: 'prezzounitario' },
  { label: 'U.M. Collo', type: 'String', alignment: 'Center', property: 'unitamisuracollo' },
  { label: 'Peso Collo', type: 'Number', alignment: 'Right', property: 'pesocassa' },
  { label: 'Categoria', type: 'String', alignment: 'Left', property: 'nomecategoria' },
  { label: 'Produttore', type: 'String', alignment: 'Left', property: 'nomeproduttore' },
  { label: 'Cadenza', type: 'String', alignment: 'Center', property: 'cadenza' },
  { label: 'Solo collo', type: 'Boolean', alignment: 'Center', property: 'solocollointero', sortable: false },
];

const formatSync = (iso?: string) =>
  iso ? moment(iso).format('DD/MM/YYYY HH:mm') : undefined;

const ProductsPage: React.FC = () => {
  const classes = useStyles();
  const {
    products, orderTypes, suppliers, loading,
    loadOrderTypes, loadSuppliers, loadCategories, getSynchroInfo,
    search, createProduct, updateProduct, deleteProduct, toggleAvailability,
    syncPriceList, importPriceList, exportPriceList,
  } = useProductsAPI();

  const [orderType, setOrderType] = useState<string | undefined>(undefined);
  const [category, setCategory] = useState<string | undefined>(undefined);
  const [available, setAvailable] = useState<string | undefined>('');
  const [cancelled, setCancelled] = useState<string | undefined>('');
  const [categories, setCategories] = useState<SelectItem[]>([]);
  const [aequosLinked, setAequosLinked] = useState(false);
  const [lastSync, setLastSync] = useState<string | undefined>(undefined);

  const [editOpen, setEditOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [toggleOpen, setToggleOpen] = useState(false);
  const [selected, setSelected] = useState<Product | undefined>(undefined);

  const fileInputRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    loadOrderTypes();
    loadSuppliers();
  }, [loadOrderTypes, loadSuppliers]);

  // When the filter order type changes, reload its categories and sync info.
  useEffect(() => {
    setCategory(undefined);
    if (!orderType) {
      setCategories([]);
      setAequosLinked(false);
      setLastSync(undefined);
      return;
    }
    loadCategories(orderType).then(setCategories);
    getSynchroInfo(orderType).then((info) => {
      setAequosLinked(!!info && info.aequosOrderId != null);
      setLastSync(formatSync(info?.lastSynchro));
    });
  }, [orderType, loadCategories, getSynchroInfo]);

  const doSearch = useCallback(() => {
    if (!orderType) return;
    search(orderType, category, available, cancelled);
  }, [orderType, category, available, cancelled, search]);

  const orderTypeOptions = useMemo(
    () => orderTypes.map((t) => ({ value: t.id, label: t.description })),
    [orderTypes],
  );
  const categoryOptions = useMemo(
    () => [{ value: '', label: 'Tutte' }, ...categories.map((c) => ({ value: c.id, label: c.description }))],
    [categories],
  );

  const onAdd = useCallback(() => {
    setSelected(undefined);
    setEditOpen(true);
  }, []);

  const onEdit = useCallback((product: Product) => {
    setSelected(product);
    setEditOpen(true);
  }, []);

  const onDelete = useCallback((product: Product) => {
    setSelected(product);
    setDeleteOpen(true);
  }, []);

  const save = useCallback(
    async (values: ProductInput) => {
      const ok = selected ? await updateProduct(selected.id, values) : await createProduct(values);
      if (ok) doSearch();
      return ok;
    },
    [selected, updateProduct, createProduct, doSearch],
  );

  const doDelete = useCallback(() => {
    if (!selected) return;
    deleteProduct(selected.id).then((ok) => {
      setDeleteOpen(false);
      setSelected(undefined);
      if (ok) doSearch();
    });
  }, [selected, deleteProduct, doSearch]);

  const onSync = useCallback(() => {
    if (!orderType) return;
    syncPriceList(orderType).then((info) => {
      if (info) {
        setLastSync(formatSync(info.lastSynchro));
        doSearch();
      }
    });
  }, [orderType, syncPriceList, doSearch]);

  const onExport = useCallback(() => {
    if (orderType) exportPriceList(orderType);
  }, [orderType, exportPriceList]);

  const onImportClick = useCallback(() => {
    fileInputRef.current?.click();
  }, []);

  const onFileSelected = useCallback(
    (event: React.ChangeEvent<HTMLInputElement>) => {
      const file = event.target.files?.[0];
      event.target.value = ''; // allow re-selecting the same file
      if (!file || !orderType) return;
      importPriceList(orderType, file).then((ok) => {
        if (ok) doSearch();
      });
    },
    [orderType, importPriceList, doSearch],
  );

  const onToggleAvailability = useCallback((product: Product) => {
    setSelected(product);
    setToggleOpen(true);
  }, []);

  const doToggleAvailability = useCallback(() => {
    if (!selected) return;
    toggleAvailability(selected).then((ok) => {
      setToggleOpen(false);
      setSelected(undefined);
      if (ok) doSearch();
    });
  }, [selected, toggleAvailability, doSearch]);

  const columns: Column[] = useMemo(
    () => [
      {
        label: 'A listino',
        type: 'Boolean',
        alignment: 'Center',
        property: 'ordinabile',
        sortable: false,
        render: (product: Product) => (
          <Checkbox
            checked={product.ordinabile}
            color="primary"
            size="small"
            onChange={() => onToggleAvailability(product)}
            inputProps={{ 'aria-label': 'Disponibilità a listino' }}
          />
        ),
      },
      ...baseColumns,
    ],
    [onToggleAvailability],
  );

  const rows = useMemo(() => products.map((value) => ({ value })), [products]);

  return (
    <Container maxWidth={false}>
      <PageTitle title="Gestione prodotti" />

      <Grid container spacing={2} className={classes.filter} alignItems="flex-end">
        <Grid item xs={12} sm={6} md={4}>
          <SelectField label="Tipologia prodotto" options={orderTypeOptions} value={orderType}
            onChange={setOrderType} placeholder="Selezionare una tipologia..." isClearable
            helperText="Tipo di ordine da gestire" />
        </Grid>
        <Grid item xs={12} sm={6} md={4}>
          <SelectField label="Categoria prodotto" options={categoryOptions} value={category ?? ''}
            onChange={(v) => setCategory(v || undefined)} isDisabled={!orderType}
            helperText="Filtra per categoria" />
        </Grid>
        <Grid item xs={6} sm={3} md={2}>
          <SelectField label="Stato" options={availableOptions} value={available}
            onChange={(v) => setAvailable(v ?? '')}
            helperText="Disponibilità a listino" />
        </Grid>
        <Grid item xs={6} sm={3} md={2}>
          <SelectField label="Annullati" options={cancelledOptions} value={cancelled}
            onChange={(v) => setCancelled(v ?? '')}
            helperText="Prodotti annullati" />
        </Grid>
      </Grid>

      <div className={classes.toolbar}>
        {aequosLinked ? (
          <>
            {lastSync ? (
              <Typography variant="body2" className={classes.lastSync}>
                Ultima sincronizzazione: {lastSync}
              </Typography>
            ) : null}
            <Button startIcon={<SyncIcon />} onClick={onSync} disabled={!orderType}>
              Sincronizza listino
            </Button>
          </>
        ) : (
          <>
            <Button startIcon={<ImportIcon />} onClick={onImportClick} disabled={!orderType}>
              Importa listino
            </Button>
            <Button startIcon={<ExportIcon />} onClick={onExport} disabled={!orderType}>
              Esporta listino
            </Button>
            <Button startIcon={<AddIcon />} color="secondary" onClick={onAdd} disabled={!orderType}>
              Nuovo prodotto
            </Button>
          </>
        )}
        <Button variant="contained" color="primary" startIcon={<SearchIcon />} onClick={doSearch}
          disabled={!orderType}>
          Ricerca
        </Button>
        <input ref={fileInputRef} type="file" accept=".xls,.xlsx" hidden onChange={onFileSelected} />
      </div>

      <SortableTable
        columns={columns}
        rows={rows}
        loading={loading}
        settings={{
          canAdd: false,
          canEdit: true,
          canDelete: true,
          showHeader: true,
          showFooter: false,
          pagination: true,
          rowsPerPage: 50,
        }}
        canDeleteRow={(p: Product) => !p.ordineaequos}
        onEdit={onEdit}
        onDelete={onDelete}
      />

      <ProductEditDialog
        open={editOpen}
        product={selected}
        suppliers={suppliers}
        orderTypes={orderTypes}
        loadCategories={loadCategories}
        onCancel={() => setEditOpen(false)}
        onSave={save}
      />

      <ActionDialog
        open={deleteOpen}
        onCancel={() => setDeleteOpen(false)}
        actions={['Ok']}
        onAction={doDelete}
        title="Conferma eliminazione"
        message={`Il prodotto ${selected?.descrizione ?? ''} verrà cancellato, continuare?`}
      />

      <ActionDialog
        open={toggleOpen}
        onCancel={() => setToggleOpen(false)}
        actions={['Ok']}
        onAction={doToggleAvailability}
        title="Conferma disponibilità"
        message={
          selected?.ordinabile
            ? `Il prodotto ${selected?.descrizione ?? ''} verrà rimosso dal listino, continuare?`
            : `Il prodotto ${selected?.descrizione ?? ''} verrà aggiunto al listino, continuare?`
        }
      />
    </Container>
  );
};

export default ProductsPage;
