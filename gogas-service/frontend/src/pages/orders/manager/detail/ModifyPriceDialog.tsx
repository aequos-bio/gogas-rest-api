import React, { useEffect, useState, useCallback } from 'react';
import { Grid, TextField, Typography } from '@material-ui/core';
import FormDialog from '../../../../components/form/FormDialog';
import NumberField from '../../../../components/form/NumberField';
import { OrderByProduct } from './types';

interface Props {
  open: boolean;
  product?: OrderByProduct;
  onCancel: () => void;
  onSave: (price: number) => Promise<boolean>;
}

/** Change the product price for this order (legacy "modifica prezzo"). */
const ModifyPriceDialog: React.FC<Props> = ({ open, product, onCancel, onSave }) => {
  const [price, setPrice] = useState<number | undefined>(undefined);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) return;
    setPrice(product?.prezzoKg);
    setSaving(false);
  }, [open, product]);

  const canSave = price !== undefined && price > 0;

  const save = useCallback(async () => {
    if (price === undefined) return;
    setSaving(true);
    const ok = await onSave(price);
    setSaving(false);
    if (ok) onCancel();
  }, [price, onSave, onCancel]);

  return (
    <FormDialog
      open={open}
      title="Modifica prezzo del prodotto"
      canSave={canSave}
      saving={saving}
      maxWidth="xs"
      onCancel={onCancel}
      onSave={save}
    >
      <Grid container spacing={2}>
        <Grid item xs={12}>
          <TextField fullWidth size="small" label="Prodotto" value={product?.nomeProdotto ?? ''} disabled />
        </Grid>
        <Grid item xs={12}>
          <NumberField label="Prezzo" value={price} decimals={2} min={0} endAdornment="€" onChange={setPrice} />
        </Grid>
        <Grid item xs={12}>
          <Typography variant="caption" color="textSecondary">
            Attenzione: il prezzo del prodotto per questo ordine verrà sostituito.
          </Typography>
        </Grid>
      </Grid>
    </FormDialog>
  );
};

export default ModifyPriceDialog;
