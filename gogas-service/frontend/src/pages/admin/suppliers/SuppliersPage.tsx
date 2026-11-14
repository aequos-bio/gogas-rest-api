import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Container, TextField, Button } from '@material-ui/core';
import { GetApp as ExportIcon } from '@material-ui/icons';
import { useSnackbar } from 'notistack';
import PageTitle from '../../../components/PageTitle';
import SortableTable, { Column } from '../../../components/table/SortableTable';
import { useTableFilter } from '../../../components/table/useTableFilter';
import ActionDialog from '../../../components/ActionDialog';
import { exportToExcel } from '../../../utils/download_utils';
import SupplierEditDialog from './SupplierEditDialog';
import { useSuppliersAPI } from './useSuppliersAPI';
import { Supplier } from './types';

const columns: Column[] = [
  { label: 'Ragione sociale', type: 'String', alignment: 'Left', property: 'ragionesociale' },
  { label: 'Provincia', type: 'String', alignment: 'Left', property: 'provincia' },
  { label: 'Codice esterno', type: 'String', alignment: 'Center', property: 'idesterno' },
];

/** Auto-imported suppliers (with an external id) cannot be edited or deleted. */
const isManaged = (s: Supplier) => s.idesterno === null || s.idesterno === undefined;

const SuppliersPage: React.FC = () => {
  const { suppliers, loading, reload, createSupplier, updateSupplier, deleteSupplier } =
    useSuppliersAPI();
  const { enqueueSnackbar } = useSnackbar();

  const [editOpen, setEditOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [selected, setSelected] = useState<Supplier | undefined>(undefined);

  useEffect(() => {
    reload();
  }, [reload]);

  const getValue = useCallback((row: Supplier, property: string) => (row as any)[property], []);
  const { filteredRows, term, setTerm } = useTableFilter<Supplier>(
    suppliers,
    getValue,
    ['ragionesociale', 'provincia'],
  );

  const rows = useMemo(() => filteredRows.map((value) => ({ value })), [filteredRows]);

  const onAdd = useCallback(() => {
    setSelected(undefined);
    setEditOpen(true);
  }, []);

  const onEdit = useCallback((supplier: Supplier) => {
    setSelected(supplier);
    setEditOpen(true);
  }, []);

  const onDelete = useCallback((supplier: Supplier) => {
    setSelected(supplier);
    setDeleteOpen(true);
  }, []);

  const save = useCallback(
    (values: { ragionesociale: string; provincia: string }) => {
      if (selected) {
        return updateSupplier(selected.id, { ...selected, ...values });
      }
      return createSupplier(values);
    },
    [selected, createSupplier, updateSupplier],
  );

  const doDelete = useCallback(() => {
    if (!selected) return;
    deleteSupplier(selected.id).then(() => {
      setDeleteOpen(false);
      setSelected(undefined);
    });
  }, [selected, deleteSupplier]);

  const onExport = useCallback(() => {
    if (!suppliers.length) {
      enqueueSnackbar('Nessun produttore da esportare', { variant: 'info' });
      return;
    }
    exportToExcel<Supplier>(
      suppliers,
      [
        { header: 'Ragione sociale', value: 'ragionesociale', width: 40 },
        { header: 'Provincia', value: 'provincia', width: 15 },
        { header: 'Codice esterno', value: (s) => s.idesterno ?? '', width: 20 },
      ],
      'produttori',
      'Produttori',
    );
  }, [suppliers, enqueueSnackbar]);

  return (
    <Container maxWidth={false}>
      <PageTitle title="Produttori">
        <TextField
          size="small"
          variant="outlined"
          placeholder="Cerca..."
          value={term}
          onChange={(evt) => setTerm(evt.target.value)}
        />
        <Button startIcon={<ExportIcon />} onClick={onExport}>
          Esporta
        </Button>
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
        canEditRow={isManaged}
        canDeleteRow={isManaged}
        onAdd={onAdd}
        onEdit={onEdit}
        onDelete={onDelete}
      />

      <SupplierEditDialog
        open={editOpen}
        supplier={selected}
        onCancel={() => setEditOpen(false)}
        onSave={save}
      />

      <ActionDialog
        open={deleteOpen}
        onCancel={() => setDeleteOpen(false)}
        actions={['Ok']}
        onAction={doDelete}
        title="Conferma eliminazione"
        message={`Il produttore ${selected?.ragionesociale ?? ''} verrà cancellato, continuare?`}
      />
    </Container>
  );
};

export default SuppliersPage;
