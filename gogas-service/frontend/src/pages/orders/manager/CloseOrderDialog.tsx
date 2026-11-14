import React, { useEffect, useState, useCallback } from 'react';
import { Grid, Typography } from '@material-ui/core';
import FormDialog from '../../../components/form/FormDialog';
import SelectField from '../../../components/form/SelectField';

interface Props {
  open: boolean;
  saving?: boolean;
  onCancel: () => void;
  onConfirm: (roundType: number) => void;
}

const roundOptions = [
  { value: '0', label: 'Secondo soglia' },
  { value: '1', label: 'Per eccesso' },
  { value: '2', label: 'Per difetto' },
];

/**
 * Confirmation dialog for the "chiudi" (close) action. Lets the manager pick
 * the rounding mode used to build the supplier order (as in the legacy
 * closeOrderDialog): 0 = by threshold, 1 = round up, 2 = round down.
 */
const CloseOrderDialog: React.FC<Props> = ({ open, saving = false, onCancel, onConfirm }) => {
  const [roundType, setRoundType] = useState('0');

  useEffect(() => {
    if (open) setRoundType('0');
  }, [open]);

  const confirm = useCallback(() => {
    onConfirm(parseInt(roundType, 10));
  }, [roundType, onConfirm]);

  return (
    <FormDialog
      open={open}
      title="Chiusura ordine"
      saving={saving}
      saveLabel="Conferma"
      maxWidth="xs"
      onCancel={onCancel}
      onSave={confirm}
    >
      <Grid container spacing={2}>
        <Grid item xs={12}>
          <Typography variant="body2">
            Scegli l'arrotondamento da operare per creare automaticamente l'ordine produttore:
          </Typography>
        </Grid>
        <Grid item xs={12}>
          <SelectField
            label="Arrotondamento"
            options={roundOptions}
            value={roundType}
            onChange={(v) => setRoundType(v ?? '0')}
          />
        </Grid>
      </Grid>
    </FormDialog>
  );
};

export default CloseOrderDialog;
