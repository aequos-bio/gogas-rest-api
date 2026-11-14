import React, { useEffect, useMemo, useState, useCallback } from 'react';
import { Grid, TextField } from '@material-ui/core';
import FormDialog from '../../../components/form/FormDialog';
import { Supplier } from './types';

interface Props {
  open: boolean;
  supplier?: Supplier;
  onCancel: () => void;
  onSave: (values: { ragionesociale: string; provincia: string }) => Promise<boolean>;
}

/**
 * Add/edit dialog for a supplier, built on the shared FormDialog. Replaces the
 * legacy jqxWindow form in admin/suppliers.html. Fields: Ragione Sociale and
 * Provincia (both required), matching the legacy validation.
 */
const SupplierEditDialog: React.FC<Props> = ({ open, supplier, onCancel, onSave }) => {
  const [ragionesociale, setRagionesociale] = useState('');
  const [provincia, setProvincia] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) return;
    setRagionesociale(supplier?.ragionesociale ?? '');
    setProvincia(supplier?.provincia ?? '');
    setSaving(false);
  }, [open, supplier]);

  const canSave = useMemo(
    () => ragionesociale.trim().length > 0 && provincia.trim().length > 0,
    [ragionesociale, provincia],
  );

  const save = useCallback(async () => {
    setSaving(true);
    const ok = await onSave({
      ragionesociale: ragionesociale.trim(),
      provincia: provincia.trim(),
    });
    setSaving(false);
    if (ok) onCancel();
  }, [onSave, onCancel, ragionesociale, provincia]);

  return (
    <FormDialog
      open={open}
      title={supplier ? 'Modifica produttore' : 'Nuovo produttore'}
      canSave={canSave}
      saving={saving}
      onCancel={onCancel}
      onSave={save}
    >
      <Grid container spacing={2}>
        <Grid item xs={12}>
          <TextField
            fullWidth
            size="small"
            label="Ragione Sociale"
            value={ragionesociale}
            onChange={(evt) => setRagionesociale(evt.target.value)}
          />
        </Grid>
        <Grid item xs={12}>
          <TextField
            fullWidth
            size="small"
            label="Provincia"
            value={provincia}
            onChange={(evt) => setProvincia(evt.target.value)}
          />
        </Grid>
      </Grid>
    </FormDialog>
  );
};

export default SupplierEditDialog;
