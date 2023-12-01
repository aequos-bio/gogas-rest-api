import React from 'react';
import {
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions
} from '@material-ui/core';

interface Props {
  open: boolean;
  onCancel: () => void;
  onExport: (type: 'simple' | 'full') => void;
}

const ExportTypeSelectionDialog: React.FC<Props> = ({ open, onCancel, onExport }) => {
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
        <Button onClick={() => onExport('simple')}>
          Situaz. contabile
        </Button>
        <Button onClick={() => onExport('full')}>
          Situaz. contab. + dettaglio
        </Button>
      </DialogActions>
    </Dialog>
  );
}

export default ExportTypeSelectionDialog;