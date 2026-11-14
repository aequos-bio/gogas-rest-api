import React, { useEffect, useMemo, useState, useCallback } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Grid,
  CircularProgress,
} from '@material-ui/core';
import SelectField from '../../../../components/form/SelectField';
import NumberField from '../../../../components/form/NumberField';
import { SelectItem } from './types';

interface Props {
  open: boolean;
  availableUsers: SelectItem[];
  onCancel: () => void;
  /** Save a movement. `keepOpen` means "conferma e continua". Returns success. */
  onSave: (userId: string, amount: number, keepOpen: boolean) => Promise<boolean>;
}

/**
 * Add a user movement (manual charge) to an external order. Offers both
 * "Conferma" and "Conferma e continua" (keeps the dialog open and resets the
 * fields for rapid entry), mirroring the legacy dialog.
 */
const AddMovementDialog: React.FC<Props> = ({ open, availableUsers, onCancel, onSave }) => {
  const [userId, setUserId] = useState<string | undefined>(undefined);
  const [amount, setAmount] = useState<number | undefined>(undefined);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) return;
    setUserId(undefined);
    setAmount(undefined);
    setSaving(false);
  }, [open]);

  const userOptions = useMemo(
    () => availableUsers.map((u) => ({ value: u.id, label: u.description })),
    [availableUsers],
  );

  const canSave = !!userId && amount !== undefined && amount > 0;

  const save = useCallback(
    async (keepOpen: boolean) => {
      if (!userId || amount === undefined) return;
      setSaving(true);
      const ok = await onSave(userId, amount, keepOpen);
      setSaving(false);
      if (ok) {
        if (keepOpen) {
          setUserId(undefined);
          setAmount(undefined);
        } else {
          onCancel();
        }
      }
    },
    [userId, amount, onSave, onCancel],
  );

  return (
    <Dialog open={open} onClose={saving ? undefined : onCancel} maxWidth="xs" fullWidth>
      <DialogTitle>Aggiungi movimento utente</DialogTitle>
      <DialogContent>
        <Grid container spacing={2}>
          <Grid item xs={12}>
            <SelectField label="Utente" options={userOptions} value={userId} onChange={setUserId} />
          </Grid>
          <Grid item xs={12}>
            <NumberField label="Importo" value={amount} decimals={2} min={0} endAdornment="€" onChange={setAmount} />
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions>
        <Button onClick={onCancel} disabled={saving}>
          Annulla
        </Button>
        <Button onClick={() => save(true)} disabled={!canSave || saving}>
          Conferma e continua
        </Button>
        <Button
          onClick={() => save(false)}
          color="primary"
          disabled={!canSave || saving}
          startIcon={saving ? <CircularProgress size={16} /> : undefined}
        >
          Conferma
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default AddMovementDialog;
