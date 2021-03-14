import React, { useState, useEffect, useCallback } from 'react';
import { useSelector } from 'react-redux';
import { withSnackbar } from 'notistack';
import { Container } from '@material-ui/core';
import { apiGetJson, apiDelete } from '../../../utils/axios_utils';
import PageTitle from '../../../components/PageTitle';
import DataTable from '../../../components/DataTable';
import ActionDialog from '../../../components/ActionDialog';
import EditGasMovementDialog from './EditGasMovementDialog';

const GasMovements = ({ enqueueSnackbar }) => {
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState([]);
  const [deleteDlgOpen, setDeleteDlgOpen] = useState(false);
  const [selectedMovement, setSelectedMovement] = useState();
  const [showDlg, setShowDlg] = useState(false);
  const accounting = useSelector(state => state.accounting);

  const reload = useCallback(() => {
    setLoading(true);
    apiGetJson('/api/accounting/gas/entry/list', {
      dateFrom: `01/01/${accounting.currentYear}`,
      dateTo: `31/12/${accounting.currentYear}`,
    }).then(vv => {
      setLoading(false);
      if (vv.error) {
        enqueueSnackbar(vv.errorMessage, {
          variant: 'error',
        });
      } else {
        setRows(vv);
      }
    });
  }, [enqueueSnackbar, accounting]);

  useEffect(() => {
    reload();
  }, [reload]);

  const columns = [
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
      property: 'importo',
    },
  ];

  const addMovement = useCallback(() => {
    setSelectedMovement();
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
    apiDelete(`/api/accounting/gas/entry/${selectedMovement.id}`)
      .then(() => {
        setSelectedMovement();
        setDeleteDlgOpen(false);
        enqueueSnackbar('Movimento eliminato', { variant: 'success' });
        reload();
      })
      .catch(err => {
        enqueueSnackbar(`Impossibile eliminare il movimento: ${err}`, {
          variant: 'error',
        });
      });
  }, [enqueueSnackbar, reload, selectedMovement]);

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
          canEdit: true,
          canDelete: true,
          canAdd: true,
          pagination: false,
        }}
        columns={columns}
        rows={rows.map(v => ({ value: v }))}
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

export default withSnackbar(GasMovements);
