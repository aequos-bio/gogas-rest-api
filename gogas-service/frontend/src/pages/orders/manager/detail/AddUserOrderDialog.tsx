import React, { useEffect, useMemo, useState, useCallback } from 'react';
import { Grid, TextField } from '@material-ui/core';
import FormDialog from '../../../../components/form/FormDialog';
import SelectField from '../../../../components/form/SelectField';
import NumberField from '../../../../components/form/NumberField';
import { OrderByProduct, SelectItem } from './types';

interface Props {
  open: boolean;
  product?: OrderByProduct;
  availableUsers: SelectItem[];
  onCancel: () => void;
  onSave: (userId: string, qta: number) => Promise<boolean>;
}

/** Add a user's order for the selected product (legacy "nuovo ordine"). */
const AddUserOrderDialog: React.FC<Props> = ({ open, product, availableUsers, onCancel, onSave }) => {
  const [userId, setUserId] = useState<string | undefined>(undefined);
  const [qta, setQta] = useState<number | undefined>(undefined);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) return;
    setUserId(undefined);
    setQta(undefined);
    setSaving(false);
  }, [open]);

  const userOptions = useMemo(
    () => availableUsers.map((u) => ({ value: u.id, label: u.description })),
    [availableUsers],
  );

  const canSave = !!userId && qta !== undefined && qta > 0;

  const save = useCallback(async () => {
    if (!userId || qta === undefined) return;
    setSaving(true);
    const ok = await onSave(userId, qta);
    setSaving(false);
    if (ok) onCancel();
  }, [userId, qta, onSave, onCancel]);

  return (
    <FormDialog
      open={open}
      title="Aggiungi ordine utente"
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
          <SelectField label="Utente" options={userOptions} value={userId} onChange={setUserId} />
        </Grid>
        <Grid item xs={12}>
          <NumberField
            label="Quantità"
            value={qta}
            decimals={2}
            min={0}
            endAdornment={product?.umProdotto}
            onChange={setQta}
          />
        </Grid>
      </Grid>
    </FormDialog>
  );
};

export default AddUserOrderDialog;
