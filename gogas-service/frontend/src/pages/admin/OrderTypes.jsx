import React, { useEffect, useCallback, useState, useMemo } from "react";
import {
  Container,
  Fab,
  Button,
  IconButton,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody
} from "@material-ui/core";
import {
  AddSharp as PlusIcon,
  CheckSharp as CheckIcon,
  EditSharp as EditIcon,
  DeleteSharp as DeleteIcon,
  SyncSharp as SyncIcon
} from "@material-ui/icons";
import { withSnackbar } from "notistack";
import { connect } from "react-redux";
import { makeStyles } from "@material-ui/core/styles";
import { getJson, apiPut, calldelete } from "../../utils/axios_utils";
import PageTitle from "../../components/PageTitle";
import ActionDialog from "../../components/ActionDialog";
import LoadingRow from "../../components/LoadingRow";
import EditOrderTypeDialog from "./components/EditOrderTypeDialog";

const useStyles = makeStyles(theme => ({
  fab: {
    position: "fixed",
    bottom: theme.spacing(2),
    right: theme.spacing(2)
  },
  tableCell: {
    paddingLeft: theme.spacing(1),
    paddingRight: theme.spacing(1)
  },
  tdFlag: {
    textAlign: "center",
    maxWidth: "60px",
    width: "60px",
    color: theme.palette.grey[700]
  },
  tdButtons: {
    fontSize: "130%",
    textAlign: "center",
    minWidth: "88px",
    width: "88px"
  },
  cellHeader: {
    paddingLeft: theme.spacing(1),
    paddingRight: theme.spacing(1),
    verticalAlign: "bottom"
  },
  cellHeaderFlag: {
    maxWidth: "60px",
    width: "60px",
    textAlign: "center"
  }
}));

const OrderTypes = ({ enqueueSnackbar }) => {
  const classes = useStyles();
  const [loading, setLoading] = useState(false);
  const [orderTypes, setOrderTypes] = useState([]);
  const [dialogMode, setDialogMode] = useState(false);
  const [selectedId, setSelectedId] = useState();
  const [deleteDlgOpen, setDeleteDlgOpen] = useState(false);

  const reload = useCallback(() => {
    setLoading(true);
    getJson("/api/ordertype/list", {}).then(oo => {
      setLoading(false);
      if (oo.error) {
        enqueueSnackbar(oo.errorMessage, { variant: "error" });
      } else {
        setOrderTypes(oo);
      }
    });
  }, [enqueueSnackbar]);

  useEffect(() => {
    reload();
  }, [reload]);

  const newOrderType = useCallback(() => {
    setSelectedId();
    setDialogMode("new");
  }, []);

  const editOrderType = useCallback(id => {
    setSelectedId(id);
    setDialogMode("edit");
  }, []);

  const deleteOrderType = useCallback(id => {
    setSelectedId(id);
    setDeleteDlgOpen(true);
  }, []);

  const doDeleteOrderType = useCallback(() => {
    calldelete(`/api/ordertype/${selectedId}`)
      .then(() => {
        setDeleteDlgOpen(false);
        reload();
        enqueueSnackbar("Tipo ordine eliminato", { variant: "success" });
      })
      .catch(err => {
        enqueueSnackbar(
          err.response?.statusText ||
            "Errore nell'eliminazione del tipo ordine",
          { variant: "error" }
        );
      });
  }, [enqueueSnackbar, selectedId, reload]);

  const dialogClosed = useCallback(
    needrefresh => {
      setDialogMode(false);
      if (needrefresh) {
        reload();
      }
    },
    [reload]
  );

  const syncWithAequos = useCallback(() => {
    apiPut("/api/ordertype/aequos/sync")
      .then(() => {
        enqueueSnackbar("Sincronizzazione completata con successo", {
          variant: "success"
        });
        reload();
      })
      .catch(err => {
        enqueueSnackbar(`Errore nella sincronizzazione: ${err}`, {
          variant: "error"
        });
      });
  }, [reload, enqueueSnackbar]);

  const rows = useMemo(() => {
    return loading ? (
      <LoadingRow colSpan={8} />
    ) : (
      orderTypes.map(o => (
        <TableRow key={`ordertype-${o.id}`} hover>
          <TableCell className={classes.tableCell}>{o.descrizione}</TableCell>
          <TableCell className={`${classes.tableCell} ${classes.tdFlag}`}>
            {o.riepilogo ? <CheckIcon /> : null}
          </TableCell>
          <TableCell className={`${classes.tableCell} ${classes.tdFlag}`}>
            {o.totalecalcolato ? <CheckIcon /> : null}
          </TableCell>
          <TableCell className={`${classes.tableCell} ${classes.tdFlag}`}>
            {o.turni ? <CheckIcon /> : null}
          </TableCell>
          <TableCell className={`${classes.tableCell} ${classes.tdFlag}`}>
            {o.preventivo ? <CheckIcon /> : null}
          </TableCell>
          <TableCell className={`${classes.tableCell} ${classes.tdFlag}`}>
            {o.completamentocolli ? <CheckIcon /> : null}
          </TableCell>
          <TableCell className={`${classes.tableCell} ${classes.tdFlag}`}>
            {o.external ? <CheckIcon /> : null}
          </TableCell>
          <TableCell className={classes.tableCell}>
            <IconButton
              onClick={() => {
                editOrderType(o.id);
              }}
            >
              <EditIcon fontSize="small" />
            </IconButton>
            <IconButton
              onClick={() => {
                deleteOrderType(o.id);
              }}
            >
              <DeleteIcon fontSize="small" />
            </IconButton>
          </TableCell>
        </TableRow>
      ))
    );
  }, [orderTypes, classes, editOrderType, deleteOrderType, loading]);

  return (
    <Container maxWidth={false}>
      <PageTitle title="Tipi di ordine">
        <Button onClick={syncWithAequos} startIcon={<SyncIcon />}>
          Sincronizza
        </Button>
      </PageTitle>

      <Fab className={classes.fab} color="secondary" onClick={newOrderType}>
        <PlusIcon />
      </Fab>

      <TableContainer>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell className={classes.cellHeader}>Descrizione</TableCell>
              <TableCell
                className={`${classes.cellHeader} ${classes.cellHeaderFlag}`}
              >
                Raggruppam. amici
              </TableCell>
              <TableCell
                className={`${classes.cellHeader} ${classes.cellHeaderFlag}`}
              >
                Totale calcolato
              </TableCell>
              <TableCell
                className={`${classes.cellHeader} ${classes.cellHeaderFlag}`}
              >
                Prevede turni
              </TableCell>
              <TableCell
                className={`${classes.cellHeader} ${classes.cellHeaderFlag}`}
              >
                Mostra preventivo
              </TableCell>
              <TableCell
                className={`${classes.cellHeader} ${classes.cellHeaderFlag}`}
              >
                Mostra compl. colli
              </TableCell>
              <TableCell
                className={`${classes.cellHeader} ${classes.cellHeaderFlag}`}
              >
                Ordine esterno
              </TableCell>
              <TableCell className={classes.tdButtons} />
            </TableRow>
          </TableHead>

          <TableBody className={classes.tableBody}>{rows}</TableBody>
        </Table>
      </TableContainer>

      <EditOrderTypeDialog
        mode={dialogMode}
        onClose={dialogClosed}
        orderTypeId={selectedId}
      />

      <ActionDialog
        open={deleteDlgOpen}
        onCancel={() => setDeleteDlgOpen(false)}
        actions={["Ok"]}
        onAction={doDeleteOrderType}
        title="Conferma eliminazione"
        message="Sei sicuro di voler eliminare il tipo ordine selezionato?"
      />
    </Container>
  );
};

const mapStateToProps = state => {
  return {
    authentication: state.authentication,
    info: state.info
  };
};

const mapDispatchToProps = {};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(withSnackbar(OrderTypes));
