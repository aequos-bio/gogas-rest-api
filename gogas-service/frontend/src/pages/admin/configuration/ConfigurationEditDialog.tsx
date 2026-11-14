import React, { useEffect, useMemo, useState, useCallback } from 'react';
import { Grid, TextField } from '@material-ui/core';
import FormDialog from '../../../components/form/FormDialog';
import { ConfigurationItem } from './types';

interface Props {
  open: boolean;
  item?: ConfigurationItem;
  onCancel: () => void;
  onSave: (item: ConfigurationItem) => Promise<boolean>;
}

/**
 * Edit dialog for a single configuration item. Key and description are shown
 * read-only (as in the legacy grid, where only the value cell was editable),
 * and the value is required to be non-empty.
 */
const ConfigurationEditDialog: React.FC<Props> = ({ open, item, onCancel, onSave }) => {
  const [value, setValue] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) return;
    setValue(item?.value ?? '');
    setSaving(false);
  }, [open, item]);

  const canSave = useMemo(() => value.trim().length > 0, [value]);

  const save = useCallback(async () => {
    if (!item) return;
    setSaving(true);
    const ok = await onSave({ ...item, value: value.trim() });
    setSaving(false);
    if (ok) onCancel();
  }, [item, value, onSave, onCancel]);

  return (
    <FormDialog
      open={open}
      title="Modifica configurazione"
      canSave={canSave}
      saving={saving}
      maxWidth="sm"
      onCancel={onCancel}
      onSave={save}
    >
      <Grid container spacing={2}>
        <Grid item xs={12}>
          <TextField fullWidth size="small" label="Chiave" value={item?.key ?? ''} disabled />
        </Grid>
        <Grid item xs={12}>
          <TextField
            fullWidth
            size="small"
            label="Descrizione"
            value={item?.description ?? ''}
            disabled
          />
        </Grid>
        <Grid item xs={12}>
          <TextField
            fullWidth
            size="small"
            label="Valore"
            value={value}
            error={!canSave}
            helperText={!canSave ? 'Il campo non può essere vuoto' : ''}
            onChange={(evt) => setValue(evt.target.value)}
          />
        </Grid>
      </Grid>
    </FormDialog>
  );
};

export default ConfigurationEditDialog;
