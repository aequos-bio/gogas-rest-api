import React, { useCallback, useEffect, useState, useMemo } from 'react';
import {
  Container,
  Fab,
  IconButton,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
} from '@material-ui/core';
import {
  EditSharp as EditIcon,
  DeleteSharp as DeleteIcon,
  AddSharp as PlusIcon,
  RemoveSharp as RemoveIcon,
} from '@material-ui/icons';
import { makeStyles } from '@material-ui/core/styles';
import { apiDelete } from '../../../utils/axios_utils';
import PageTitle from '../../../components/PageTitle';
import EditReasonDialog from './EditReasonDialog';
import ActionDialog from '../../../components/ActionDialog';
import Loadingrow from '../../../components/LoadingRow';
import { useReasonsAPI } from './useReasonsAPI';

const useStyles = makeStyles((theme) => ({
  fab: {
    position: 'fixed',
    bottom: theme.spacing(2),
    right: theme.spacing(2),
  },
  tdIcon: {
    color: 'red',
    textAlign: 'center',
    width: '30px',
  },
  tdButtons: {
    fontSize: '130%',
    textAlign: 'center',
  },
}));

const Reasons: React.FC = () => {
  const classes = useStyles();
  const [dialogMode, setDialogMode] = useState<false | 'new' | 'edit'>(false);
  const [selectedCode, setSelectedCode] = useState<string | undefined>(undefined);
  const [deleteDlgOpen, setDeleteDlgOpen] = useState(false);
  const { reasons, reload, loading, deleteReason } = useReasonsAPI();

  useEffect(() => {
    reload();
  }, [reload]);

  const newReason = useCallback(() => {
    setSelectedCode(undefined);
    setDialogMode('new');
  }, []);

  const editReason = useCallback((reasonCode) => {
    setSelectedCode(reasonCode);
    setDialogMode('edit');
  }, []);

  const _deleteReason = useCallback((reasonCode) => {
    setSelectedCode(reasonCode);
    setDeleteDlgOpen(true);
  }, []);

  const rows = useMemo(() => {
    if (loading) {
      return <Loadingrow colSpan={5} />;
    }
    return reasons.map((r) => (
      <TableRow key={`reason-${r.reasonCode}`} hover>
        <TableCell>{r.reasonCode}</TableCell>
        <TableCell>{r.description}</TableCell>
        <TableCell>
          {r.sign === '+' ? (
            <PlusIcon fontSize='small' />
          ) : (
            <RemoveIcon fontSize='small' />
          )}
        </TableCell>
        <TableCell>{r.accountingCode}</TableCell>
        <TableCell className={classes.tdButtons}>
          <IconButton
            onClick={() => {
              editReason(r.reasonCode);
            }}
          >
            <EditIcon fontSize='small' />
          </IconButton>
          <IconButton
            onClick={() => {
              _deleteReason(r.reasonCode);
            }}
          >
            <DeleteIcon fontSize='small' />
          </IconButton>
        </TableCell>
      </TableRow>
    ));
  }, [editReason, _deleteReason, classes, reasons, loading]);

  const dialogClosed = useCallback(
    (refresh) => {
      setDialogMode(false);
      if (refresh) reload();
    },
    [reload],
  );

  const doDeleteReason = useCallback(() => {
    if (!selectedCode) return;
    deleteReason(selectedCode).then(() => {
      setDeleteDlgOpen(false);
    })
  }, [selectedCode, reload]);

  return (
    <Container maxWidth={false}>
      <PageTitle title='Causali' />

      <Fab className={classes.fab} color='secondary' onClick={newReason}>
        <PlusIcon />
      </Fab>

      <TableContainer>
        <Table size='small'>
          <TableHead>
            <TableRow>
              <TableCell>Codice</TableCell>
              <TableCell>Descrizione</TableCell>
              <TableCell>Segno</TableCell>
              <TableCell>Cod. contabile</TableCell>
              <TableCell />
            </TableRow>
          </TableHead>

          <TableBody>{rows}</TableBody>
        </Table>
      </TableContainer>

      <EditReasonDialog
        mode={dialogMode}
        onClose={dialogClosed}
        reasonCode={selectedCode}
      />
      <ActionDialog
        open={deleteDlgOpen}
        onCancel={() => setDeleteDlgOpen(false)}
        actions={['Ok']}
        onAction={doDeleteReason}
        title='Conferma eliminazione'
        message='Sei sicuro di voler eliminare la causale selezionata?'
      />
    </Container>
  );
};

export default Reasons;
