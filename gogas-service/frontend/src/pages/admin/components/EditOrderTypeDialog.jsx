import React, { useMemo, useState, useCallback, useEffect } from "react";
import {
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Checkbox,
  FormControlLabel
} from "@material-ui/core";
import { withSnackbar } from "notistack";
import { makeStyles } from "@material-ui/core/styles";
import { getJson, postRawJson, apiPut } from "../../../utils/axios_utils";

const useStyles = makeStyles(theme => ({
  content: {
    display: "flex",
    flexDirection: "column"
  },
  field: {
    marginBottom: theme.spacing(1),
    marginTop: theme.spacing(1)
  },
  radiogrp: {
    flexDirection: "row"
  },
  icon: {
    color: theme.palette.grey[500]
  }
}));

const EditOrderTypeDialog = ({
  mode,
  onClose,
  orderTypeId,
  enqueueSnackbar
}) => {
  const classes = useStyles();
  const [description, setDescription] = useState("");
  const [groupFriends, setGroupFriends] = useState(false);
  const [calculateTotal, setCalculateTotal] = useState(false);
  const [externalOrder, setExternalOrder] = useState(false);
  const [needShifts, setNeedShifts] = useState(false);
  const [showCount, setShowCount] = useState(false);
  const [showCompleteness, setShowCompleteness] = useState(false);
  const [exportAllUsers, setExportAllUsers] = useState(false);
  const [exportAllProducts, setExportAllProducts] = useState(false);
  const [externalLink, setExternalLink] = useState("");

  useEffect(() => {
    setDescription("");
    setGroupFriends(false);
    setCalculateTotal(false);
    setExternalOrder(false);
    setNeedShifts(false);
    setShowCount(false);
    setShowCompleteness(false);
    setExportAllUsers(false);
    setExportAllProducts(false);
    setExternalLink("");

    if (orderTypeId) {
      getJson(`/api/ordertype/${orderTypeId}`, {})
        .then(ot => {
          if (ot.error) {
            enqueueSnackbar(ot.errorMessage, { variant: "error" });
          } else {
            setDescription(ot.descrizione || "");
            setGroupFriends(ot.riepilogo || false);
            setCalculateTotal(ot.totalecalcolato || false);
            setExternalOrder(ot.external || false);
            setNeedShifts(ot.turni || false);
            setShowCount(ot.preventivo || false);
            setShowCompleteness(ot.completamentocolli || false);
            setExportAllUsers(ot.exportAllUsers || false);
            setExportAllProducts(ot.exportAllProducts || false);
            setExternalLink(ot.externalLink || "");
          }
        })
        .catch(err => {
          enqueueSnackbar(
            err.response?.statusText ||
              "Errore nel caricamento del tipo di ordine",
            { variant: "error" }
          );
        });
    }
  }, [orderTypeId, enqueueSnackbar]);

  const canSave = useMemo(() => {
    return description !== undefined && description !== "";
  }, [description]);

  const save = useCallback(() => {
    const params = {
      descrizione: description,
      riepilogo: groupFriends,
      totalecalcolato: calculateTotal,
      external: externalOrder,
      turni: needShifts,
      preventivo: showCount,
      completamentocolli: showCompleteness,
      exportAllUsers,
      exportAllProducts,
      externalLink
    };

    const thenFn = () => {
      enqueueSnackbar(
        `Tipo ordine ${mode === "new" ? "salvato" : "modificato"}`,
        { variant: "success" }
      );
      onClose(true);
    };

    const catchFn = err => {
      enqueueSnackbar(
        err.response?.statusText || "Errore nel salvataggio della causale",
        { variant: "error" }
      );
    };

    if (mode === "new") {
      postRawJson("/api/ordertype", params)
        .then(thenFn)
        .catch(catchFn);
    } else {
      apiPut(`/api/ordertype/${orderTypeId}`, params)
        .then(thenFn)
        .catch(catchFn);
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
    exportAllUsers,
    exportAllProducts,
    externalLink,
    mode,
    enqueueSnackbar,
    onClose
  ]);

  return (
    <Dialog
      open={mode !== false}
      onClose={() => onClose()}
      maxWidth="xs"
      fullWidth
    >
      <DialogTitle>
        {mode === "new" ? "Nuovo" : "Modifica"} tipo di ordine
      </DialogTitle>

      <DialogContent className={classes.content}>
        <TextField
          className={classes.field}
          label="Descrizione"
          value={description}
          variant="outlined"
          size="small"
          InputLabelProps={{
            shrink: true
          }}
          onChange={evt => {
            setDescription(evt.target.value);
          }}
          fullWidth
        />

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

        <hr style={{ width: "100%" }} />

        <strong>Generazione file excel degli ordini</strong>

        <FormControlLabel
          control={
            <Checkbox
              checked={exportAllUsers}
              onChange={evt => setExportAllUsers(evt.target.checked)}
            />
          }
          label="Esporta tutti gli utenti"
        />

        <FormControlLabel
          control={
            <Checkbox
              checked={exportAllProducts}
              onChange={evt => setExportAllProducts(evt.target.checked)}
            />
          }
          label="Esporta tutti i prodotti"
        />

        <hr style={{ width: "100%" }} />

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
            shrink: true
          }}
          InputProps={{
            readOnly: externalOrder === false
          }}
          onChange={evt => {
            setExternalLink(evt.target.value);
          }}
          fullWidth
        />
      </DialogContent>

      <DialogActions>
        <Button onClick={() => onClose()} autoFocus>
          Annulla
        </Button>
        <Button onClick={() => save(false)} disabled={!canSave}>
          Salva
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default withSnackbar(EditOrderTypeDialog);
