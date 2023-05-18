/* eslint-disable react/no-array-index-key */
import React from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button
} from '@material-ui/core';

interface Props {
  title: string;
  message: string;
  open: boolean;
  onCancel: () => void;
  onAction: (actionIndex: number) => void;
  actions: string[];
}

const ActionDialog: React.FC<Props> = ({ title, message, open, onCancel, onAction, actions }) => {
  return (
    <Dialog open={open} onClose={onCancel} maxWidth='xs' fullWidth>
      <DialogTitle>{title}</DialogTitle>

      <DialogContent>{message}</DialogContent>

      <DialogActions>
        <Button onClick={onCancel} autoFocus>
          Annulla
        </Button>
        {actions.map((action, i) => (
          <Button key={`action-${i}`} onClick={() => onAction(i)}>
            {action}
          </Button>
        ))}

      </DialogActions>
    </Dialog>
  );
}

export default ActionDialog;