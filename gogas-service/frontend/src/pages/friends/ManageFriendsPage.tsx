import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Container, TextField } from '@material-ui/core';
import PageTitle from '../../components/PageTitle';
import SortableTable, { Column } from '../../components/table/SortableTable';
import { useTableFilter } from '../../components/table/useTableFilter';
import ActionDialog from '../../components/ActionDialog';
import FriendEditDialog from './FriendEditDialog';
import { useFriendsAPI } from './useFriendsAPI';
import useJwt from '../../hooks/JwtHooks';
import { Friend, FriendInput } from './types';

const columns: Column[] = [
  { label: 'Nome', type: 'String', alignment: 'Left', property: 'nome' },
  { label: 'Cognome', type: 'String', alignment: 'Left', property: 'cognome' },
  { label: 'Username', type: 'String', alignment: 'Left', property: 'username' },
  { label: 'E-mail', type: 'String', alignment: 'Left', property: 'email' },
  { label: 'Attivo', type: 'Boolean', alignment: 'Center', property: 'attivo' },
];

const ManageFriendsPage: React.FC = () => {
  const jwt = useJwt();
  const { friends, loading, reload, usernameExists, createFriend, updateFriend, deleteFriend } =
    useFriendsAPI();

  const [editOpen, setEditOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [selected, setSelected] = useState<Friend | undefined>(undefined);

  useEffect(() => {
    reload();
  }, [reload]);

  const getValue = useCallback((row: Friend, property: string) => (row as any)[property], []);
  const { filteredRows, term, setTerm } = useTableFilter<Friend>(
    friends,
    getValue,
    ['nome', 'cognome', 'username', 'email'],
  );

  const rows = useMemo(() => filteredRows.map((value) => ({ value })), [filteredRows]);

  const onAdd = useCallback(() => {
    setSelected(undefined);
    setEditOpen(true);
  }, []);

  const onEdit = useCallback((friend: Friend) => {
    setSelected(friend);
    setEditOpen(true);
  }, []);

  const onDelete = useCallback((friend: Friend) => {
    setSelected(friend);
    setDeleteOpen(true);
  }, []);

  const save = useCallback(
    (values: FriendInput) => {
      if (selected) {
        return updateFriend(selected.idUtente, values);
      }
      return createFriend(values);
    },
    [selected, createFriend, updateFriend],
  );

  const doDelete = useCallback(() => {
    if (!selected) return;
    deleteFriend(selected.idUtente).then(() => {
      setDeleteOpen(false);
      setSelected(undefined);
    });
  }, [selected, deleteFriend]);

  return (
    <Container maxWidth={false}>
      <PageTitle title="Gestione amici">
        <TextField
          size="small"
          variant="outlined"
          placeholder="Cerca..."
          value={term}
          onChange={(evt) => setTerm(evt.target.value)}
        />
      </PageTitle>

      <SortableTable
        columns={columns}
        rows={rows}
        loading={loading}
        settings={{
          canAdd: true,
          canEdit: true,
          canDelete: true,
          showHeader: true,
          showFooter: false,
          pagination: true,
          rowsPerPage: 25,
        }}
        onAdd={onAdd}
        onEdit={onEdit}
        onDelete={onDelete}
      />

      <FriendEditDialog
        open={editOpen}
        friend={selected}
        referralId={jwt?.id}
        onCancel={() => setEditOpen(false)}
        onSave={save}
        usernameExists={usernameExists}
      />

      <ActionDialog
        open={deleteOpen}
        onCancel={() => setDeleteOpen(false)}
        actions={['Ok']}
        onAction={doDelete}
        title="Conferma eliminazione"
        message={`L'utente ${selected?.nome ?? ''} ${selected?.cognome ?? ''} verrà cancellato, continuare?`}
      />
    </Container>
  );
};

export default ManageFriendsPage;
