import React, { useState, useEffect, useCallback } from 'react';
import { Container } from '@material-ui/core';
import PageTitle from '../../../components/PageTitle';
import DataTable, { Column } from '../../../components/DataTable';
import ActionDialog from '../../../components/ActionDialog';
import EditGasMovementDialog from './EditGasMovementDialog';
import { useAppSelector } from '../../../store/store';
import { useGasMovementsAPI } from './useGasMovementsAPI';
import { GasMovementView } from './types';

const GasMovements: React.FC = () => {
  const [deleteDlgOpen, setDeleteDlgOpen] = useState(false);
  const [selectedMovement, setSelectedMovement] = useState<GasMovementView | undefined>();
  const [showDlg, setShowDlg] = useState(false);
  const { gasMovements, loading, reload, deleteGasMovement } = useGasMovementsAPI();
  const accounting = useAppSelector(state => state.accounting);

  useEffect(() => {
    reload();
  }, [reload]);

  const columns: Column[] = [
    { label: 'Data', type: 'Date', alignment: 'Left', property: 'data' },
    {
      label: 'Causale',
      type: 'String',
      alignment: 'Left',
      property: 'nomecausale',
    },
    {
      label: 'Descrizione',
      type: 'String',
      alignment: 'Left',
      property: 'descrizione',
    },
    {
      label: 'Importo',
      type: 'Amount',
      alignment: 'Right',
      property: 'importo',
    },
  ];

  const addMovement = useCallback(() => {
    setSelectedMovement(undefined);
    setShowDlg(true);
  }, []);

  const editMovement = useCallback(mov => {
    setSelectedMovement(mov);
    setShowDlg(true);
  }, []);

  const deleteMovement = useCallback(mov => {
    setSelectedMovement(mov);
    setDeleteDlgOpen(true);
  }, []);

  const doDeleteMovement = useCallback(() => {
    if (!selectedMovement || !selectedMovement.id) return undefined;
    deleteGasMovement(selectedMovement.id).then(() => {
      setSelectedMovement(undefined);
      setDeleteDlgOpen(false);
    });
  }, [reload, selectedMovement]);

  const editDialogClosed = useCallback(
    refreshdata => {
      setShowDlg(false);
      if (refreshdata) reload();
    },
    [reload]
  );

  return (
    <Container maxWidth={false}>
      <PageTitle title={`Movimenti del GAS - ${accounting.currentYear}`} />

      <DataTable
        settings={{
          showEdit: true,
          showDelete: true,
          showAdd: true,
          pagination: false,
          showHeader: true,
          showFooter: false,
        }}
        columns={columns}
        rows={gasMovements.map(movement => ({ value: movement }))}
        loading={loading}
        onAdd={addMovement}
        onEdit={editMovement}
        onDelete={deleteMovement}
      />

      <ActionDialog
        open={deleteDlgOpen}
        onCancel={() => setDeleteDlgOpen(false)}
        actions={['Ok']}
        onAction={doDeleteMovement}
        title="Conferma eliminazione"
        message="Sei sicuro di voler eliminare il movimento selezionato?"
      />
      <EditGasMovementDialog
        open={showDlg}
        onClose={editDialogClosed}
        movement={selectedMovement}
      />
    </Container>
  );
};

export default GasMovements;
