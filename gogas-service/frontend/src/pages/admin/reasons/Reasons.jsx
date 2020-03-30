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
import _ from 'lodash';
import { makeStyles } from '@material-ui/core/styles';
import { withSnackbar } from 'notistack';
import { apiGetJson, apiDelete } from '../../../utils/axios_utils';
import PageTitle from '../../../components/PageTitle';
import EditReasonDialog from './EditReasonDialog';
import ActionDialog from '../../../components/ActionDialog';
import Loadingrow from '../../../components/LoadingRow';

const useStyles = makeStyles(theme => ({
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

const Reasons = ({ enqueueSnackbar }) => {
  const classes = useStyles();
  const [reasons, setReasons] = useState([]);
  const [dialogMode, setDialogMode] = useState(false);
  const [selectedCode, setSelectedCode] = useState();
  const [deleteDlgOpen, setDeleteDlgOpen] = useState(false);
  const [loading, setLoading] = useState(false);

  const reload = useCallback(() => {
    setLoading(true);
    apiGetJson('/api/accounting/reason/list', {})
      .then(rr => {
        setLoading(false);
        if (rr.error) {
          enqueueSnackbar(rr.errorMessage, { variant: 'error' });
        } else {
          setReasons(_.orderBy(rr, ['reasonCode'], ['asc']));
        }
      })
      .catch(err => {
        enqueueSnackbar(
          err.response?.statusText || 'Errore nel caricamento delle causali',
          { variant: 'error' }
        );
      });
  }, [enqueueSnackbar]);

  useEffect(() => {
    reload();
  }, [reload]);

  const newReason = useCallback(() => {
    setSelectedCode();
    setDialogMode('new');
  }, []);

  const editReason = useCallback(reasonCode => {
    setSelectedCode(reasonCode);
    setDialogMode('edit');
  }, []);

  const deleteReason = useCallback(reasonCode => {
    setSelectedCode(reasonCode);
    setDeleteDlgOpen(true);
  }, []);

  const rows = useMemo(() => {
    if (loading) {
      return <Loadingrow colSpan={5} />;
    }
    return reasons.map(r => (
      <TableRow key={`reason-${r.reasonCode}`} hover>
        <TableCell>{r.reasonCode}</TableCell>
        <TableCell>{r.description}</TableCell>
        <TableCell>
          {r.sign === '+' ? (
            <PlusIcon size="small" />
          ) : (
            <RemoveIcon size="small" />
          )}
        </TableCell>
        <TableCell>{r.accountingCode}</TableCell>
        <TableCell className={classes.tdButtons}>
          <IconButton
            onClick={() => {
              editReason(r.reasonCode);
            }}
          >
            <EditIcon fontSize="small" />
          </IconButton>
          <IconButton
            onClick={() => {
              deleteReason(r.reasonCode);
            }}
          >
            <DeleteIcon fontSize="small" />
          </IconButton>
        </TableCell>
      </TableRow>
    ));
  }, [editReason, deleteReason, classes, reasons, loading]);

  const dialogClosed = useCallback(
    refresh => {
      setDialogMode(false);
      if (refresh) reload();
    },
    [reload]
  );

  const doDeleteReason = useCallback(() => {
    apiDelete(`/api/accounting/reason/${selectedCode}`)
      .then(() => {
        setDeleteDlgOpen(false);
        reload();
        enqueueSnackbar('Causale eliminata', { variant: 'success' });
      })
      .catch(err => {
        enqueueSnackbar(
          err.response?.statusText || "Errore nell'eliminazione della causale",
          { variant: 'error' }
        );
      });
  }, [enqueueSnackbar, selectedCode, reload]);

  return (
    <Container maxWidth={false}>
      <PageTitle title="Causali" />

      <Fab className={classes.fab} color="secondary" onClick={newReason}>
        <PlusIcon />
      </Fab>

      <TableContainer>
        <Table size="small">
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
        title="Conferma eliminazione"
        message="Sei sicuro di voler eliminare la causale selezionata?"
      />
    </Container>
  );
};

export default withSnackbar(Reasons);
