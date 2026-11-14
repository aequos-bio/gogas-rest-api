import React, { useEffect, useState, useCallback } from 'react';
import { Grid, TextField, FormControlLabel, Checkbox } from '@material-ui/core';
import moment from 'moment-timezone';
import FormDialog from '../../../../components/form/FormDialog';
import NumberField from '../../../../components/form/NumberField';
import DatePickerField from '../../../../components/form/DatePickerField';
import { OrderDetails } from './types';

interface Props {
  open: boolean;
  details?: OrderDetails;
  /** Aequos orders lock the invoice number/amount/date. */
  aequosLocked?: boolean;
  onCancel: () => void;
  onSave: (data: {
    numeroFattura?: string;
    importoFattura?: number;
    dataFattura?: string;
    pagato: boolean;
    dataPagamento?: string;
  }) => Promise<boolean>;
}

const toIso = (it?: string) => (it ? moment(it, 'DD/MM/YYYY').format('YYYY-MM-DD') : undefined);
const toItalian = (iso?: string) => (iso ? moment(iso, 'YYYY-MM-DD').format('DD/MM/YYYY') : undefined);

/** Edit the accounting / invoice data of an order (legacy "dati contabili"). */
const InvoiceDataDialog: React.FC<Props> = ({ open, details, aequosLocked = false, onCancel, onSave }) => {
  const [numero, setNumero] = useState('');
  const [importo, setImporto] = useState<number | undefined>(undefined);
  const [dataFattura, setDataFattura] = useState<string | undefined>(undefined);
  const [pagato, setPagato] = useState(false);
  const [dataPagamento, setDataPagamento] = useState<string | undefined>(undefined);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) return;
    setNumero(details?.numerofattura ?? '');
    setImporto(details?.totalefattura);
    setDataFattura(toIso(details?.datafattura));
    setPagato(details?.evaso ?? false);
    setDataPagamento(toIso(details?.datapagamento));
    setSaving(false);
  }, [open, details]);

  const save = useCallback(async () => {
    setSaving(true);
    const ok = await onSave({
      numeroFattura: numero.trim() || undefined,
      importoFattura: importo,
      dataFattura: toItalian(dataFattura),
      pagato,
      dataPagamento: toItalian(dataPagamento),
    });
    setSaving(false);
    if (ok) onCancel();
  }, [numero, importo, dataFattura, pagato, dataPagamento, onSave, onCancel]);

  return (
    <FormDialog
      open={open}
      title="Dati contabili"
      saving={saving}
      saveLabel="Conferma"
      maxWidth="xs"
      onCancel={onCancel}
      onSave={save}
    >
      <Grid container spacing={2}>
        <Grid item xs={12}>
          <TextField
            fullWidth size="small" label="Numero fattura" value={numero}
            disabled={aequosLocked} onChange={(e) => setNumero(e.target.value)}
          />
        </Grid>
        <Grid item xs={12}>
          <NumberField
            label="Importo fattura" value={importo} decimals={2} min={0} endAdornment="€"
            disabled={aequosLocked} onChange={setImporto}
          />
        </Grid>
        <Grid item xs={12}>
          <DatePickerField
            label="Data fattura" value={dataFattura} disabled={aequosLocked} onChange={setDataFattura}
          />
        </Grid>
        <Grid item xs={12}>
          <FormControlLabel
            control={<Checkbox checked={pagato} color="primary" onChange={(e) => setPagato(e.target.checked)} />}
            label="Pagato"
          />
        </Grid>
        <Grid item xs={12}>
          <DatePickerField label="Data pagamento" value={dataPagamento} onChange={setDataPagamento} />
        </Grid>
      </Grid>
    </FormDialog>
  );
};

export default InvoiceDataDialog;
