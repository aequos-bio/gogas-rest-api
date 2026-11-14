import React, { useEffect, useMemo, useState, useCallback } from 'react';
import { Grid, TextField } from '@material-ui/core';
import FormDialog from '../../components/form/FormDialog';
import { Friend, FriendInput } from './types';

interface Props {
  open: boolean;
  friend?: Friend;
  referralId?: string;
  onCancel: () => void;
  onSave: (values: FriendInput) => Promise<boolean>;
  usernameExists: (username: string) => Promise<boolean>;
}

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/**
 * Add/edit dialog for a friend (role "S"). Mirrors the legacy manage-friends
 * form: on edit the username is locked and the password is optional; on create
 * all fields are editable, username must be unique and the password required.
 */
const FriendEditDialog: React.FC<Props> = ({
  open,
  friend,
  referralId,
  onCancel,
  onSave,
  usernameExists,
}) => {
  const isEdit = !!friend;

  const [nome, setNome] = useState('');
  const [cognome, setCognome] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [email, setEmail] = useState('');
  const [usernameError, setUsernameError] = useState<string | undefined>(undefined);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) return;
    setNome(friend?.nome ?? '');
    setCognome(friend?.cognome ?? '');
    setUsername(friend?.username ?? '');
    setPassword('');
    setEmail(friend?.email ?? '');
    setUsernameError(undefined);
    setSaving(false);
  }, [open, friend]);

  const emailError = useMemo(
    () => (email.length > 0 && !EMAIL_RE.test(email) ? "L'indirizzo e-mail non è valido" : undefined),
    [email],
  );

  const canSave = useMemo(() => {
    return (
      nome.trim().length > 0 &&
      cognome.trim().length > 0 &&
      username.trim().length > 0 &&
      (isEdit || password.length > 0) &&
      !emailError
    );
  }, [nome, cognome, username, password, isEdit, emailError]);

  const save = useCallback(async () => {
    setSaving(true);

    // Username uniqueness is only checked on create (legacy behaviour).
    if (!isEdit) {
      const exists = await usernameExists(username.trim());
      if (exists) {
        setUsernameError('Username già esistente');
        setSaving(false);
        return;
      }
    }

    const values: FriendInput = {
      username: username.trim(),
      nome: nome.trim(),
      cognome: cognome.trim(),
      email: email.trim() || undefined,
      ruolo: 'S',
      idReferente: friend?.idReferente ?? referralId,
      attivo: friend ? friend.attivo : true,
    };
    if (!isEdit || password.length > 0) {
      values.password = password;
    }

    const ok = await onSave(values);
    setSaving(false);
    if (ok) onCancel();
  }, [isEdit, username, nome, cognome, email, password, friend, referralId, usernameExists, onSave, onCancel]);

  return (
    <FormDialog
      open={open}
      title={isEdit ? 'Modifica amico' : 'Nuovo amico'}
      canSave={canSave}
      saving={saving}
      maxWidth="xs"
      onCancel={onCancel}
      onSave={save}
    >
      <Grid container spacing={2}>
        <Grid item xs={12}>
          <TextField
            fullWidth
            size="small"
            label="Nome utente"
            value={username}
            disabled={isEdit}
            error={!!usernameError}
            helperText={usernameError}
            onChange={(evt) => {
              setUsername(evt.target.value);
              setUsernameError(undefined);
            }}
          />
        </Grid>
        <Grid item xs={12}>
          <TextField
            fullWidth
            size="small"
            type="password"
            autoComplete="new-password"
            label={isEdit ? 'Password (lascia vuoto per non modificare)' : 'Password'}
            value={password}
            onChange={(evt) => setPassword(evt.target.value)}
          />
        </Grid>
        <Grid item xs={12}>
          <TextField
            fullWidth
            size="small"
            label="Nome"
            value={nome}
            onChange={(evt) => setNome(evt.target.value)}
          />
        </Grid>
        <Grid item xs={12}>
          <TextField
            fullWidth
            size="small"
            label="Cognome"
            value={cognome}
            onChange={(evt) => setCognome(evt.target.value)}
          />
        </Grid>
        <Grid item xs={12}>
          <TextField
            fullWidth
            size="small"
            label="E-mail"
            value={email}
            error={!!emailError}
            helperText={emailError}
            onChange={(evt) => setEmail(evt.target.value)}
          />
        </Grid>
      </Grid>
    </FormDialog>
  );
};

export default FriendEditDialog;
