import React from 'react';
import {
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions
} from '@material-ui/core';

const ExportTypeSelectionDialog = ({open, onCancel, onExport}) => {
  return (
    <Dialog open={open} onClose={onCancel}>
      <DialogTitle>
        Esportazione dati
      </DialogTitle>

      <DialogContent>
        <DialogContentText id="alert-dialog-description">
          Selezionare il tipo di esportazione
        </DialogContentText>
      </DialogContent>
        
      <DialogActions>
        <Button onClick={onCancel} autoFocus>
          Annulla
        </Button>
        <Button onClick={() => onExport('simple')} color='secondary'>
          Situaz. contabile
        </Button>
        <Button onClick={() => onExport('full')} color='secondary'>
          Situaz. contab. + dettaglio
        </Button>
      </DialogActions>
    </Dialog>
  );
}

export default ExportTypeSelectionDialog;