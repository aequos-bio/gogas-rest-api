import React, { useEffect, useMemo, useState, useCallback } from 'react';
import { Grid, FormControlLabel, Checkbox, TextField } from '@material-ui/core';
import moment from 'moment-timezone';
import FormDialog from '../../../components/form/FormDialog';
import SelectField from '../../../components/form/SelectField';
import NumberField from '../../../components/form/NumberField';
import DatePickerField from '../../../components/form/DatePickerField';
import { ManagerOrder, OrderInput, OrderTypeSelectItem } from './types';

/** Prefill for a new order opened from an Aequos availability notification. */
export interface OrderPrefill {
  idtipoordine: string;
  /** Dates in ISO (YYYY-MM-DD) form. */
  dataapertura?: string;
  datachiusura?: string;
  dataconsegna?: string;
}

interface Props {
  open: boolean;
  order?: ManagerOrder;
  prefill?: OrderPrefill;
  orderTypes: OrderTypeSelectItem[];
  onCancel: () => void;
  onSave: (values: OrderInput) => Promise<boolean>;
}

const toIso = (itDate?: string) => (itDate ? moment(itDate, 'DD/MM/YYYY').format('YYYY-MM-DD') : undefined);
const toItalian = (iso?: string) => (iso ? moment(iso, 'YYYY-MM-DD').format('DD/MM/YYYY') : undefined);
const before = (a?: string, b?: string) => !a || !b || moment(a).isBefore(moment(b));

/**
 * Create/edit dialog for a manager order. Mirrors the legacy orders-list popup:
 *  - on edit the order type is locked;
 *  - external link is editable only for "external" order types;
 *  - "sincronizza listino alla creazione" is available only for Aequos types
 *    and only when creating a new order;
 *  - opening date must precede both due and delivery dates.
 */
const OrderEditDialog: React.FC<Props> = ({ open, order, prefill, orderTypes, onCancel, onSave }) => {
  const isEdit = !!order;

  const [typeId, setTypeId] = useState<string | undefined>(undefined);
  const [openingDate, setOpeningDate] = useState<string | undefined>(undefined);
  const [dueDate, setDueDate] = useState<string | undefined>(undefined);
  const [dueHour, setDueHour] = useState<number | undefined>(0);
  const [deliveryDate, setDeliveryDate] = useState<string | undefined>(undefined);
  const [externalLink, setExternalLink] = useState('');
  const [updateProductList, setUpdateProductList] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) return;
    if (order) {
      // Editing an existing order.
      setTypeId(order.idtipoordine);
      setOpeningDate(toIso(order.dataapertura));
      setDueDate(toIso(order.datachiusura));
      setDueHour(order.orachiusura ?? 0);
      setDeliveryDate(toIso(order.dataconsegna));
      setExternalLink(order.externallink ?? '');
      setUpdateProductList(false);
    } else if (prefill) {
      // New order prefilled from an Aequos availability notification.
      setTypeId(prefill.idtipoordine);
      setOpeningDate(prefill.dataapertura);
      setDueDate(prefill.datachiusura);
      setDueHour(0);
      setDeliveryDate(prefill.dataconsegna);
      const t = orderTypes.find((o) => o.id.toLowerCase() === prefill.idtipoordine.toLowerCase());
      setExternalLink(t?.externalLink ?? '');
      setUpdateProductList(t?.idordineaequos != null);
    } else {
      // Blank new order.
      setTypeId(undefined);
      setOpeningDate(undefined);
      setDueDate(undefined);
      setDueHour(0);
      setDeliveryDate(undefined);
      setExternalLink('');
      setUpdateProductList(false);
    }
    setSaving(false);
  }, [open, order, prefill, orderTypes]);

  const selectedType = useMemo(
    () => orderTypes.find((t) => t.id.toLowerCase() === (typeId ?? '').toLowerCase()),
    [orderTypes, typeId],
  );
  const isExternal = !!selectedType?.external;
  const isAequos = selectedType?.idordineaequos != null;

  const typeOptions = useMemo(
    () => orderTypes.map((t) => ({ value: t.id, label: t.description })),
    [orderTypes],
  );

  const onTypeChange = useCallback(
    (value?: string) => {
      setTypeId(value);
      const t = orderTypes.find((o) => o.id === value);
      setExternalLink(t?.externalLink ?? '');
      setUpdateProductList(t?.idordineaequos != null);
    },
    [orderTypes],
  );

  const datesValid = useMemo(
    () => before(openingDate, dueDate) && before(openingDate, deliveryDate),
    [openingDate, dueDate, deliveryDate],
  );

  const canSave = useMemo(() => {
    return (
      !!typeId &&
      !!openingDate &&
      !!dueDate &&
      !!deliveryDate &&
      dueHour !== undefined &&
      datesValid
    );
  }, [typeId, openingDate, dueDate, deliveryDate, dueHour, datesValid]);

  const save = useCallback(async () => {
    if (!typeId || !openingDate || !dueDate || !deliveryDate || dueHour === undefined) return;
    setSaving(true);
    const values: OrderInput = {
      idtipoordine: typeId,
      dataapertura: toItalian(openingDate)!,
      datachiusura: toItalian(dueDate)!,
      orachiusura: dueHour,
      dataconsegna: toItalian(deliveryDate)!,
      externallink: isExternal ? externalLink.trim() || undefined : undefined,
      updateProductList: !isEdit && isAequos ? updateProductList : undefined,
    };
    const ok = await onSave(values);
    setSaving(false);
    if (ok) onCancel();
  }, [
    typeId, openingDate, dueDate, deliveryDate, dueHour, isExternal, externalLink,
    isEdit, isAequos, updateProductList, onSave, onCancel,
  ]);

  const dateError = !datesValid;

  return (
    <FormDialog
      open={open}
      title={isEdit ? 'Modifica ordine' : 'Nuovo ordine'}
      canSave={canSave}
      saving={saving}
      maxWidth="sm"
      onCancel={onCancel}
      onSave={save}
    >
      <Grid container spacing={2}>
        <Grid item xs={12}>
          <SelectField
            label="Tipologia ordine"
            options={typeOptions}
            value={typeId}
            isDisabled={isEdit}
            onChange={onTypeChange}
          />
        </Grid>
        <Grid item xs={12}>
          <DatePickerField
            label="Data apertura"
            value={openingDate}
            onChange={setOpeningDate}
            error={dateError}
            helperText={dateError ? 'Deve precedere chiusura e consegna' : undefined}
          />
        </Grid>
        <Grid item xs={8}>
          <DatePickerField label="Data chiusura" value={dueDate} onChange={setDueDate} error={dateError} />
        </Grid>
        <Grid item xs={4}>
          <NumberField label="Ora chiusura" value={dueHour} decimals={0} min={0} max={23} onChange={setDueHour} />
        </Grid>
        <Grid item xs={12}>
          <DatePickerField label="Data consegna" value={deliveryDate} onChange={setDeliveryDate} error={dateError} />
        </Grid>
        {isExternal ? (
          <Grid item xs={12} sm={6}>
            <TextField
              fullWidth
              size="small"
              label="Link esterno"
              value={externalLink}
              onChange={(e) => setExternalLink(e.target.value)}
            />
          </Grid>
        ) : null}
        {!isEdit && isAequos ? (
          <Grid item xs={12}>
            <FormControlLabel
              control={
                <Checkbox
                  checked={updateProductList}
                  color="primary"
                  onChange={(e) => setUpdateProductList(e.target.checked)}
                />
              }
              label="Sincronizza il listino alla creazione"
            />
          </Grid>
        ) : null}
      </Grid>
    </FormDialog>
  );
};

export default OrderEditDialog;
