import React, { useEffect, useMemo, useState, useCallback } from 'react';
import { Grid, TextField, FormControlLabel, Checkbox } from '@material-ui/core';
import FormDialog from '../../../components/form/FormDialog';
import SelectField from '../../../components/form/SelectField';
import NumberField from '../../../components/form/NumberField';
import { Product, ProductInput, SelectItem } from './types';

interface Props {
  open: boolean;
  product?: Product;
  suppliers: SelectItem[];
  orderTypes: SelectItem[];
  loadCategories: (typeId: string) => Promise<SelectItem[]>;
  onCancel: () => void;
  onSave: (values: ProductInput) => Promise<boolean>;
}

/**
 * Add/edit dialog for a product. Mirrors the legacy products.html popup:
 *  - type -> category dropdowns cascade (category options depend on the type);
 *  - "acquistabile solo a collo" (boxOnly) requires the box UM;
 *  - Aequos products lock the core fields (not editable).
 */
const ProductEditDialog: React.FC<Props> = ({
  open,
  product,
  suppliers,
  orderTypes,
  loadCategories,
  onCancel,
  onSave,
}) => {
  const isEdit = !!product;
  const aequosLocked = !!product?.ordineaequos;

  const [descrizione, setDescrizione] = useState('');
  const [codiceEsterno, setCodiceEsterno] = useState('');
  const [um, setUm] = useState('');
  const [prezzo, setPrezzo] = useState<number | undefined>(undefined);
  const [umCollo, setUmCollo] = useState('');
  const [pesoCassa, setPesoCassa] = useState<number | undefined>(undefined);
  const [multiplo, setMultiplo] = useState<number | undefined>(undefined);
  const [typeId, setTypeId] = useState<string | undefined>(undefined);
  const [categoryId, setCategoryId] = useState<string | undefined>(undefined);
  const [supplierId, setSupplierId] = useState<string | undefined>(undefined);
  const [cadenza, setCadenza] = useState('');
  const [note, setNote] = useState('');
  const [boxOnly, setBoxOnly] = useState(false);
  const [categories, setCategories] = useState<SelectItem[]>([]);
  const [saving, setSaving] = useState(false);

  // Reset the form when opening.
  useEffect(() => {
    if (!open) return;
    setDescrizione(product?.descrizione ?? '');
    setCodiceEsterno(product?.codiceesterno ?? '');
    setUm(product?.unitamisura ?? '');
    setPrezzo(product?.prezzounitario);
    setUmCollo(product?.unitamisuracollo ?? '');
    setPesoCassa(product?.pesocassa);
    setMultiplo(product?.multiplo ?? undefined);
    setTypeId(product?.idtipo);
    setCategoryId(product?.idcategoria);
    setSupplierId(product?.idproduttore);
    setCadenza(product?.cadenza ?? '');
    setNote(product?.note ?? '');
    setBoxOnly(product?.solocollointero ?? false);
    setSaving(false);
  }, [open, product]);

  // Load categories whenever the selected type changes.
  useEffect(() => {
    if (!open || !typeId) {
      setCategories([]);
      return;
    }
    let active = true;
    loadCategories(typeId).then((cats) => {
      if (active) setCategories(cats);
    });
    return () => {
      active = false;
    };
  }, [open, typeId, loadCategories]);

  const onTypeChange = useCallback((value?: string) => {
    setTypeId(value);
    setCategoryId(undefined); // reset category when the type changes
  }, []);

  const supplierOptions = useMemo(
    () => suppliers.map((s) => ({ value: s.id, label: s.description })),
    [suppliers],
  );
  const typeOptions = useMemo(
    () => orderTypes.map((t) => ({ value: t.id, label: t.description })),
    [orderTypes],
  );
  const categoryOptions = useMemo(
    () => categories.map((c) => ({ value: c.id, label: c.description })),
    [categories],
  );

  const canSave = useMemo(() => {
    return (
      descrizione.trim().length > 0 &&
      um.trim().length > 0 &&
      prezzo !== undefined &&
      prezzo > 0 &&
      pesoCassa !== undefined &&
      pesoCassa > 0 &&
      !!supplierId &&
      !!typeId &&
      !!categoryId &&
      (!boxOnly || umCollo.trim().length > 0) &&
      (multiplo === undefined || multiplo > 0)
    );
  }, [descrizione, um, prezzo, pesoCassa, supplierId, typeId, categoryId, boxOnly, umCollo, multiplo]);

  const save = useCallback(async () => {
    if (!typeId || !categoryId || !supplierId || prezzo === undefined || pesoCassa === undefined) return;
    setSaving(true);
    const values: ProductInput = {
      descrizione: descrizione.trim(),
      unitamisura: um.trim(),
      prezzounitario: prezzo,
      unitamisuracollo: umCollo.trim() || undefined,
      pesocassa: pesoCassa,
      cadenza: cadenza.trim() || undefined,
      codiceesterno: codiceEsterno.trim() || undefined,
      note: note.trim() || undefined,
      idproduttore: supplierId,
      idtipo: typeId,
      idcategoria: categoryId,
      ordinabile: product ? product.ordinabile : true,
      annullato: product ? product.annullato : false,
      solocollointero: boxOnly,
      multiplo: multiplo ?? null,
    };
    const ok = await onSave(values);
    setSaving(false);
    if (ok) onCancel();
  }, [
    typeId, categoryId, supplierId, prezzo, pesoCassa, descrizione, um, umCollo, cadenza,
    codiceEsterno, note, boxOnly, multiplo, product, onSave, onCancel,
  ]);

  return (
    <FormDialog
      open={open}
      title={isEdit ? 'Modifica prodotto' : 'Nuovo prodotto'}
      canSave={canSave}
      saving={saving}
      maxWidth="sm"
      onCancel={onCancel}
      onSave={save}
    >
      <Grid container spacing={2}>
        <Grid item xs={12} sm={8}>
          <TextField fullWidth size="small" label="Descrizione" value={descrizione} disabled={aequosLocked}
            onChange={(e) => setDescrizione(e.target.value)} />
        </Grid>
        <Grid item xs={12} sm={4}>
          <TextField fullWidth size="small" label="Codice (Ext)" value={codiceEsterno} disabled={aequosLocked}
            onChange={(e) => setCodiceEsterno(e.target.value)} />
        </Grid>

        <Grid item xs={6} sm={3}>
          <TextField fullWidth size="small" label="Unità di misura" value={um} disabled={aequosLocked}
            onChange={(e) => setUm(e.target.value)} />
        </Grid>
        <Grid item xs={6} sm={3}>
          <NumberField label="Prezzo unitario" value={prezzo} decimals={2} min={0} disabled={aequosLocked}
            endAdornment="€" onChange={setPrezzo} />
        </Grid>
        <Grid item xs={6} sm={3}>
          <TextField fullWidth size="small" label="U.M. collo" value={umCollo} disabled={aequosLocked}
            onChange={(e) => setUmCollo(e.target.value)} />
        </Grid>
        <Grid item xs={6} sm={3}>
          <NumberField label="Peso collo" value={pesoCassa} decimals={2} min={0} disabled={aequosLocked}
            onChange={setPesoCassa} />
        </Grid>

        <Grid item xs={12} sm={6}>
          <SelectField label="Tipo ordine" options={typeOptions} value={typeId} isDisabled={aequosLocked}
            onChange={onTypeChange} />
        </Grid>
        <Grid item xs={12} sm={6}>
          <SelectField label="Categoria prodotto" options={categoryOptions} value={categoryId}
            isDisabled={aequosLocked || !typeId} onChange={setCategoryId} />
        </Grid>

        <Grid item xs={12} sm={6}>
          <SelectField label="Produttore" options={supplierOptions} value={supplierId} isDisabled={aequosLocked}
            onChange={setSupplierId} />
        </Grid>
        <Grid item xs={6} sm={3}>
          <TextField fullWidth size="small" label="Cadenza" value={cadenza} disabled={aequosLocked}
            onChange={(e) => setCadenza(e.target.value)} />
        </Grid>
        <Grid item xs={6} sm={3}>
          <NumberField label="Multiplo di" value={multiplo} decimals={2} min={0} onChange={setMultiplo} />
        </Grid>

        <Grid item xs={12}>
          <TextField fullWidth size="small" label="Note" value={note}
            onChange={(e) => setNote(e.target.value)} />
        </Grid>
        <Grid item xs={12}>
          <FormControlLabel
            control={
              <Checkbox
                checked={boxOnly}
                disabled={aequosLocked}
                onChange={(e) => setBoxOnly(e.target.checked)}
                color="primary"
              />
            }
            label="Acquistabile solo a collo"
          />
        </Grid>
      </Grid>
    </FormDialog>
  );
};

export default ProductEditDialog;
