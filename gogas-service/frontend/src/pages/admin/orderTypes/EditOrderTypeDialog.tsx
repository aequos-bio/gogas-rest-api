import React, { useMemo, useState, useCallback, useEffect } from 'react';
import {
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Checkbox,
  FormControlLabel,
} from '@material-ui/core';
import { makeStyles } from '@material-ui/core/styles';
import { useOrderTypesAPI } from './useOrderTypesAPI';
import { OrderType } from './typed';

const useStyles = makeStyles(theme => ({
  content: {
    display: 'flex',
    flexDirection: 'column',
  },
  field: {
    marginBottom: theme.spacing(1),
    marginTop: theme.spacing(1),
  },
  radiogrp: {
    flexDirection: 'row',
  },
  icon: {
    color: theme.palette.grey[500],
  },
}));

interface Props {
  mode: false | 'edit' | 'new',
  onClose: (refresh: boolean) => void,
  orderTypeId: string,
}

const EditOrderTypeDialog: React.FC<Props> = ({
  mode,
  onClose,
  orderTypeId,
}) => {
  const classes = useStyles();
  const [description, setDescription] = useState('');
  const [groupFriends, setGroupFriends] = useState(false);
  const [calculateTotal, setCalculateTotal] = useState(false);
  const [externalOrder, setExternalOrder] = useState(false);
  const [needShifts, setNeedShifts] = useState(false);
  const [showCount, setShowCount] = useState(false);
  const [showCompleteness, setShowCompleteness] = useState(false);
  const [excelAllUsers, setExcelAllUsers] = useState(false);
  const [excelAllProducts, setExcelAllProducts] = useState(false);
  const [externalLink, setExternalLink] = useState('');
  const [accountingCode, setAccountingCode] = useState<string | undefined>();
  const [orderToAequos, setOrderToAequos] = useState(false);
  const [billedByAequos, setBilledByAequos] = useState(false);
  const { getOrderType, saveOrderType, updateOrderType } = useOrderTypesAPI();

  useEffect(() => {
    if (!mode) return;
    setDescription('');
    setGroupFriends(false);
    setCalculateTotal(false);
    setExternalOrder(false);
    setNeedShifts(false);
    setShowCount(false);
    setShowCompleteness(false);
    setExcelAllUsers(false);
    setExcelAllProducts(false);
    setExternalLink('');
    setAccountingCode('');
    setOrderToAequos(false);
    setBilledByAequos(false);

    if (orderTypeId) {
      getOrderType(orderTypeId).then(orderType => {
        if (orderType) {
          setDescription(orderType.descrizione || '');
          setGroupFriends(orderType.riepilogo || false);
          setCalculateTotal(orderType.totalecalcolato || false);
          setExternalOrder(orderType.external || false);
          setNeedShifts(orderType.turni || false);
          setShowCount(orderType.preventivo || false);
          setShowCompleteness(orderType.completamentocolli || false);
          setExcelAllUsers(orderType.excelAllUsers || false);
          setExcelAllProducts(orderType.excelAllProducts || false);
          setExternalLink(orderType.externalLink || '');
          setAccountingCode(orderType.accountingCode || '');
          setOrderToAequos(orderType.idordineaequos !== undefined);
          setBilledByAequos(orderType.billedByAequos);
        }
      })
    }
  }, [mode, orderTypeId]);

  const canSave = useMemo(() => {
    return description !== undefined && description !== '';
  }, [description]);

  const save = useCallback(() => {
    const params: OrderType = {
      descrizione: description,
      riepilogo: groupFriends,
      totalecalcolato: calculateTotal,
      external: externalOrder,
      turni: needShifts,
      preventivo: showCount,
      completamentocolli: showCompleteness,
      excelAllUsers,
      excelAllProducts,
      externalLink,
      accountingCode,
      billedByAequos,
      utilizzata: true
    };

    if (mode === 'new') {
      saveOrderType(params).then(() => {
        onClose(true);
      })
    } else {
      updateOrderType(orderTypeId, params).then(() => { onClose(true) });
    }
  }, [
    orderTypeId,
    description,
    groupFriends,
    calculateTotal,
    externalOrder,
    needShifts,
    showCount,
    showCompleteness,
    excelAllUsers,
    excelAllProducts,
    externalLink,
    accountingCode,
    mode,
    onClose,
  ]);

  return (
    <Dialog
      open={mode !== false}
      onClose={() => onClose(false)}
      maxWidth="xs"
      fullWidth
    >
      <DialogTitle>
        {mode === 'new' ? 'Nuovo' : 'Modifica'} tipo di ordine
      </DialogTitle>

      <DialogContent className={classes.content}>
        <TextField
          className={classes.field}
          label="Descrizione"
          value={description}
          variant="outlined"
          size="small"
          InputLabelProps={{
            shrink: true,
          }}
          onChange={evt => {
            setDescription(evt.target.value);
          }}
          fullWidth
        />

        <FormControlLabel
          control={<Checkbox checked={orderToAequos} disabled />}
          label="Ordine Aequos"
        />
        <FormControlLabel
          control={<Checkbox checked={billedByAequos} disabled />}
          label="Fatt. da Aequos"
        />

        <hr style={{ width: '100%' }} />

        <FormControlLabel
          control={
            <Checkbox
              checked={groupFriends}
              onChange={evt => setGroupFriends(evt.target.checked)}
            />
          }
          label="Raggruppamento amici"
        />

        <FormControlLabel
          control={
            <Checkbox
              checked={calculateTotal}
              onChange={evt => setCalculateTotal(evt.target.checked)}
            />
          }
          label="Totale calcolato"
        />

        <FormControlLabel
          control={
            <Checkbox
              checked={needShifts}
              onChange={evt => setNeedShifts(evt.target.checked)}
            />
          }
          label="Prevede turni"
        />

        <FormControlLabel
          control={
            <Checkbox
              checked={showCount}
              onChange={evt => setShowCount(evt.target.checked)}
            />
          }
          label="Mostra preventivo"
        />

        <FormControlLabel
          control={
            <Checkbox
              checked={showCompleteness}
              onChange={evt => setShowCompleteness(evt.target.checked)}
            />
          }
          label="Mostra completamento colli"
        />

        <hr style={{ width: '100%' }} />

        <strong>Generazione file excel degli ordini</strong>

        <FormControlLabel
          control={
            <Checkbox
              checked={excelAllUsers}
              onChange={evt => setExcelAllUsers(evt.target.checked)}
            />
          }
          label="Esporta tutti gli utenti"
        />

        <FormControlLabel
          control={
            <Checkbox
              checked={excelAllProducts}
              onChange={evt => setExcelAllProducts(evt.target.checked)}
            />
          }
          label="Esporta tutti i prodotti"
        />

        <hr style={{ width: '100%' }} />

        <FormControlLabel
          control={
            <Checkbox
              checked={externalOrder}
              onChange={evt => setExternalOrder(evt.target.checked)}
            />
          }
          label="Ordine esterno"
        />

        <TextField
          className={classes.field}
          label="Link esterno"
          value={externalLink}
          variant="outlined"
          size="small"
          InputLabelProps={{
            shrink: true,
          }}
          InputProps={{
            readOnly: externalOrder === false,
          }}
          onChange={evt => {
            setExternalLink(evt.target.value);
          }}
          fullWidth
        />
      </DialogContent>

      <DialogActions>
        <Button onClick={() => onClose(false)} autoFocus>
          Annulla
        </Button>
        <Button onClick={() => save()} disabled={!canSave}>
          Salva
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default EditOrderTypeDialog;
