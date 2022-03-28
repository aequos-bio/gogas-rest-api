import React, { useEffect, useCallback, useMemo, useState } from 'react';
import { useSnackbar } from 'notistack';
import {
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Button,
  Grid,
  FormControlLabel,
  Checkbox,
} from '@material-ui/core';
import { makeStyles } from '@material-ui/core/styles';
import { apiDelete, apiGetJson, apiPut } from '../../../utils/axios_utils';

const useStyles = makeStyles(theme => ({
  column: {
    display: 'flex',
    flexDirection: 'column',
  },
}));

const ManagedOrderType = ({ orderType, checked, onChange }) => {
  return (
    <FormControlLabel
      control={
        <Checkbox
          checked={checked}
          onChange={evt => {
            onChange(orderType.id, evt.target.checked);
          }}
          name={`check-orderType-${orderType.id}`}
        />
      }
      label={orderType.descrizione}
    />
  );
};

const ManagerEditDialog = ({ open, onClose, manager }) => {
  const classes = useStyles();
  const { enqueueSnackbar } = useSnackbar();
  const [orderTypes, setOrderTypes] = useState([]);
  const [
    originallyManagedOrderTypes,
    setOriginallyManagedOrderTypes,
  ] = useState([]);
  const [managedOrderTypes, setManagedOrderTypes] = useState([]);

  useEffect(() => {
    if (open) return;
    setOrderTypes([]);
    setManagedOrderTypes([]);
    setOriginallyManagedOrderTypes([]);
  }, [open]);

  useEffect(() => {
    if (!open) return;
    if (!manager) return;

    apiGetJson('/api/ordertype/list', {})
      .then(orderTypeList => {
        setOrderTypes(orderTypeList);
      })
      .catch(error => {
        enqueueSnackbar('Errore nel caricamento dei dati degli ordini', {
          variant: 'error',
        });
      });

    apiGetJson('/api/ordertype/manager/list', { userId: manager.idUtente })
      .then(managers => {
        setOriginallyManagedOrderTypes(managers);
        setManagedOrderTypes(managers.map(manager => manager.orderTypeId));
      })
      .catch(err => {
        enqueueSnackbar('Errore nel caricamento dei dati', {
          variant: 'error',
        });
      });
  }, [open, manager, enqueueSnackbar]);

  const sliceSize = useMemo(() => {
    return Math.max(7, Number.parseInt(orderTypes.length / 3, 10) + 1);
  }, [orderTypes]);

  const toggleOrderType = useCallback(
    (id, value) => {
      const _managedOrderTypes = [...managedOrderTypes];

      const index = _managedOrderTypes.indexOf(id);
      if (index >= 0) {
        _managedOrderTypes.splice(index, 1);
      }

      if (value) {
        _managedOrderTypes.push(id);
      }
      setManagedOrderTypes(_managedOrderTypes);
    },
    [managedOrderTypes]
  );

  const column1 = useMemo(() => {
    return orderTypes
      .slice(0, sliceSize)
      .map(orderType => (
        <ManagedOrderType
          key={`orderType-${orderType.id}`}
          orderType={orderType}
          checked={managedOrderTypes.includes(orderType.id)}
          onChange={toggleOrderType}
        />
      ));
  }, [managedOrderTypes, orderTypes, sliceSize, toggleOrderType]);

  const column2 = useMemo(() => {
    return orderTypes
      .slice(sliceSize, sliceSize * 2)
      .map(orderType => (
        <ManagedOrderType
          key={`orderType-${orderType.id}`}
          orderType={orderType}
          checked={managedOrderTypes.includes(orderType.id)}
          onChange={toggleOrderType}
        />
      ));
  }, [managedOrderTypes, orderTypes, sliceSize, toggleOrderType]);

  const column3 = useMemo(() => {
    return orderTypes
      .slice(sliceSize * 2, 1000)
      .map(orderType => (
        <ManagedOrderType
          key={`orderType-${orderType.id}`}
          orderType={orderType}
          checked={managedOrderTypes.includes(orderType.id)}
          onChange={toggleOrderType}
        />
      ));
  }, [managedOrderTypes, orderTypes, sliceSize, toggleOrderType]);

  const allSelected = useMemo(() => {
    let _allSelected = true;
    orderTypes.forEach(orderType => {
      if (!managedOrderTypes.includes(orderType.id)) _allSelected = false;
    });
    return _allSelected;
  }, [orderTypes, managedOrderTypes]);

  const save = useCallback(() => {
    const toAdd = [];
    const toRemove = [];
    managedOrderTypes.forEach(orderType => {
      if (
        !originallyManagedOrderTypes.find(
          managedOrderType => managedOrderType.orderTypeId === orderType
        )
      )
        toAdd.push(orderType);
    });
    originallyManagedOrderTypes.forEach(managedOrderType => {
      if (!managedOrderTypes.includes(managedOrderType.orderTypeId))
        toRemove.push(managedOrderType.id);
    });

    const promises = [];
    toAdd.forEach(orderTypeId => {
      promises.push(
        apiPut(`/api/ordertype/${orderTypeId}/manager/${manager.idUtente}`)
      );
    });
    toRemove.forEach(managedOrderTypeId => {
      promises.push(apiDelete(`/api/ordertype/manager/${managedOrderTypeId}`));
    });
    Promise.all(promises)
      .then(results => {
        onClose(true);
      })
      .catch(err => {
        enqueueSnackbar('Errore di salvataggio', { variant: 'error' });
        console.error(err);
      });
  }, [
    managedOrderTypes,
    originallyManagedOrderTypes,
    manager,
    onClose,
    enqueueSnackbar,
  ]);

  const cancel = useCallback(() => {
    onClose();
  }, [onClose]);

  const selectAll = useCallback(() => {
    const _managedOrderTypes = orderTypes.map(orderType => orderType.id);
    setManagedOrderTypes(_managedOrderTypes);
  }, [orderTypes]);

  const deselectAll = useCallback(() => {
    setManagedOrderTypes([]);
  }, []);

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>
        {manager?.nome} {manager?.cognome}
      </DialogTitle>

      <DialogContent>
        <Grid container spacing={2}>
          <Grid item xs={4} className={classes.column}>
            {column1}
          </Grid>
          <Grid item xs={4} className={classes.column}>
            {column2}
          </Grid>
          <Grid item xs={4} className={classes.column}>
            {column3}
          </Grid>
        </Grid>
      </DialogContent>

      <DialogActions>
        {allSelected ? null : (
          <Button onClick={selectAll}>Seleziona tutto</Button>
        )}
        {allSelected ? (
          <Button onClick={deselectAll}>Deseleziona tutto</Button>
        ) : null}
        <div style={{ flex: '1 1' }} />
        <Button onClick={cancel}>Annulla</Button>

        <Button onClick={save}>Salva</Button>
      </DialogActions>
    </Dialog>
  );
};

export default ManagerEditDialog;
