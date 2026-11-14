import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Container, TextField } from '@material-ui/core';
import PageTitle from '../../../components/PageTitle';
import SortableTable, { Column } from '../../../components/table/SortableTable';
import { useTableFilter } from '../../../components/table/useTableFilter';
import ConfigurationEditDialog from './ConfigurationEditDialog';
import { useConfigurationAPI } from './useConfigurationAPI';
import { ConfigurationItem } from './types';

const columns: Column[] = [
  { label: 'Chiave', type: 'String', alignment: 'Left', property: 'key' },
  { label: 'Descrizione', type: 'String', alignment: 'Left', property: 'description' },
  { label: 'Valore', type: 'String', alignment: 'Left', property: 'value' },
];

/**
 * Configuration (Configurazione generale) page — native replacement for the
 * legacy admin/configuration.html jqxGrid. The list is read-only except for
 * each item's value, edited through a dialog (no add/delete, matching legacy).
 */
const ConfigurationPage: React.FC = () => {
  const { items, loading, reload, updateItem } = useConfigurationAPI();

  const [editOpen, setEditOpen] = useState(false);
  const [selected, setSelected] = useState<ConfigurationItem | undefined>(undefined);

  useEffect(() => {
    reload();
  }, [reload]);

  const getValue = useCallback((row: ConfigurationItem, property: string) => (row as any)[property], []);
  const { filteredRows, term, setTerm } = useTableFilter<ConfigurationItem>(
    items,
    getValue,
    ['key', 'description', 'value'],
  );

  const rows = useMemo(() => filteredRows.map((value) => ({ value })), [filteredRows]);

  const onEdit = useCallback((item: ConfigurationItem) => {
    setSelected(item);
    setEditOpen(true);
  }, []);

  const save = useCallback(
    (item: ConfigurationItem) => updateItem(item),
    [updateItem],
  );

  return (
    <Container maxWidth={false}>
      <PageTitle title="Configurazione generale">
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
          canAdd: false,
          canEdit: true,
          canDelete: false,
          showHeader: true,
          showFooter: false,
          pagination: true,
          rowsPerPage: 25,
        }}
        onEdit={onEdit}
      />

      <ConfigurationEditDialog
        open={editOpen}
        item={selected}
        onCancel={() => setEditOpen(false)}
        onSave={save}
      />
    </Container>
  );
};

export default ConfigurationPage;
