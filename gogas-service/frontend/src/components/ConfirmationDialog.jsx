import React from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button
} from '@material-ui/core';

const ConfirmationDialog = ({title, message, open, onCancel, onConfirm}) => {
  return (
    <Dialog open={open} onClose={onCancel} maxWidth='xs' fullWidth>
      <DialogTitle>{title}</DialogTitle>

      <DialogContent>{message}</DialogContent>

      <DialogActions>
      <Button onClick={onCancel} autoFocus>
          Annulla
        </Button>
        <Button onClick={onConfirm} color='secondary'>
          Ok
        </Button>

      </DialogActions>
    </Dialog>
  );
}

export default ConfirmationDialog