import React from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  CircularProgress,
} from '@material-ui/core';

interface Props {
  open: boolean;
  title: string;
  /** Disable the confirm button (e.g. when the form is invalid). */
  canSave?: boolean;
  /** Show a spinner and block actions while a save is in flight. */
  saving?: boolean;
  saveLabel?: string;
  cancelLabel?: string;
  maxWidth?: 'xs' | 'sm' | 'md' | 'lg' | 'xl';
  onCancel: () => void;
  onSave: () => void;
  children?: React.ReactNode;
}

/**
 * Shared add/edit dialog for the migration. Replaces the per-page jqxWindow
 * forms from the legacy UI: wraps a MUI Dialog with standard Annulla/Salva
 * actions, a disabled-until-valid confirm button, and an in-flight spinner.
 *
 * Field layout and validation live in the caller via `children` / `canSave`,
 * keeping this component form-agnostic.
 */
const FormDialog: React.FC<Props> = ({
  open,
  title,
  canSave = true,
  saving = false,
  saveLabel = 'Salva',
  cancelLabel = 'Annulla',
  maxWidth = 'xs',
  onCancel,
  onSave,
  children,
}) => {
  return (
    <Dialog
      open={open}
      onClose={saving ? undefined : onCancel}
      maxWidth={maxWidth}
      fullWidth
      disableBackdropClick={saving}
      disableEscapeKeyDown={saving}
    >
      <DialogTitle>{title}</DialogTitle>

      <DialogContent>{children}</DialogContent>

      <DialogActions>
        <Button onClick={onCancel} disabled={saving}>
          {cancelLabel}
        </Button>
        <Button
          onClick={onSave}
          color="primary"
          disabled={!canSave || saving}
          startIcon={saving ? <CircularProgress size={16} /> : undefined}
        >
          {saveLabel}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default FormDialog;
