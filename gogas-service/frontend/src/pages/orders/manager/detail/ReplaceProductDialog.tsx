import React, { useEffect, useMemo, useState, useCallback } from 'react';
import { Grid, TextField, Typography } from '@material-ui/core';
import FormDialog from '../../../../components/form/FormDialog';
import SelectField from '../../../../components/form/SelectField';
import { OrderByProduct, OrderItemByProduct } from './types';

interface Props {
  open: boolean;
  item?: OrderItemByProduct;
  products: OrderByProduct[];
  currentProductId?: string;
  onCancel: () => void;
  onSave: (targetProductId: string) => Promise<boolean>;
}

/**
 * Replace the product of a user's order item with another product from the
 * order. Replaces the legacy drag-and-drop with an explicit product picker
 * (same `PUT .../item/{itemId}/replace` endpoint).
 */
const ReplaceProductDialog: React.FC<Props> = ({
  open,
  item,
  products,
  currentProductId,
  onCancel,
  onSave,
}) => {
  const [targetId, setTargetId] = useState<string | undefined>(undefined);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) return;
    setTargetId(undefined);
    setSaving(false);
  }, [open]);

  const productOptions = useMemo(
    () =>
      products
        .filter((p) => p.idProdotto !== currentProductId && !p.annullato)
        .map((p) => ({ value: p.idProdotto, label: p.nomeProdotto })),
    [products, currentProductId],
  );

  const canSave = !!targetId;

  const save = useCallback(async () => {
    if (!targetId) return;
    setSaving(true);
    const ok = await onSave(targetId);
    setSaving(false);
    if (ok) onCancel();
  }, [targetId, onSave, onCancel]);

  return (
    <FormDialog
      open={open}
      title="Sostituisci prodotto"
      canSave={canSave}
      saving={saving}
      maxWidth="xs"
      onCancel={onCancel}
      onSave={save}
    >
      <Grid container spacing={2}>
        <Grid item xs={12}>
          <TextField fullWidth size="small" label="Utente" value={item?.utente ?? ''} disabled />
        </Grid>
        <Grid item xs={12}>
          <SelectField label="Nuovo prodotto" options={productOptions} value={targetId} onChange={setTargetId} />
        </Grid>
        <Grid item xs={12}>
          <Typography variant="caption" color="textSecondary">
            L'ordine dell'utente verrà spostato sul prodotto selezionato.
          </Typography>
        </Grid>
      </Grid>
    </FormDialog>
  );
};

export default ReplaceProductDialog;
