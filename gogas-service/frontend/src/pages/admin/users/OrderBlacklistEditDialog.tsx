import React, { useEffect, useCallback, useMemo, useState } from 'react';
import { useSnackbar } from 'notistack';
import {
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Button,
  Grid,
} from '@material-ui/core';
import { makeStyles } from '@material-ui/core/styles';
import { apiDelete, apiPut } from '../../../utils/axios_utils';
import { User } from './types';
import ManagedOrderType from '../managers/ManagedOrderType';
import { AxiosResponse } from 'axios';
import { useOrderTypesAPI } from '../orderTypes/useOrderTypesAPI';
import { useOrderTypesBlacklistAPI } from './useOrderTypesBlacklistAPI';

const useStyles = makeStyles(theme => ({
  column: {
    display: 'flex',
    flexDirection: 'column',
  },
}));

interface Props {
  open: boolean;
  onClose: (ok?: boolean) => void;
  user?: User;
}

const OrderBlacklistEditDialog: React.FC<Props> = ({ open, onClose, user }) => {
  const classes = useStyles();
  const { enqueueSnackbar } = useSnackbar();
  const { orderTypes, reload: reloadOrderTypes } = useOrderTypesAPI();
  const { reload: reloadBlacklist, updateBlacklist } = useOrderTypesBlacklistAPI();
  const [blacklistOrderTypes, setBlacklistOrderTypes] = useState<string[]>([]);

  useEffect(() => {
    reloadOrderTypes();
  }, []);

  useEffect(() => {
    if (!user) return;

    reloadBlacklist(user!.idUtente)
      .then((blacklist) => {
        setBlacklistOrderTypes(blacklist);
      });
  }, [user, open]);

  const sliceSize = useMemo(() => {
    return Math.max(7, orderTypes.length / 3 + 1);
  }, [orderTypes]);

  const toggleOrderType = useCallback(
    (id, value) => {
      const _blacklistOrderTypes = [...blacklistOrderTypes];

      const index = blacklistOrderTypes.indexOf(id);
      if (index >= 0) {
        _blacklistOrderTypes.splice(index, 1);
      }

      if (value) {
        _blacklistOrderTypes.push(id);
      }
      setBlacklistOrderTypes(_blacklistOrderTypes);
    },
    [blacklistOrderTypes]
  );

  const column1 = useMemo(() => {
    return orderTypes
      .slice(0, sliceSize)
      .map(orderType => (
        <ManagedOrderType
          key={`orderType-${orderType.id}`}
          orderType={orderType}
          checked={blacklistOrderTypes.includes(orderType.id as string)}
          onChange={toggleOrderType}
        />
      ));
  }, [blacklistOrderTypes, orderTypes, sliceSize, toggleOrderType]);

  const column2 = useMemo(() => {
    return orderTypes
      .slice(sliceSize, sliceSize * 2)
      .map(orderType => (
        <ManagedOrderType
          key={`orderType-${orderType.id}`}
          orderType={orderType}
          checked={blacklistOrderTypes.includes(orderType.id as string)}
          onChange={toggleOrderType}
        />
      ));
  }, [blacklistOrderTypes, orderTypes, sliceSize, toggleOrderType]);

  const column3 = useMemo(() => {
    return orderTypes
      .slice(sliceSize * 2, 1000)
      .map(orderType => (
        <ManagedOrderType
          key={`orderType-${orderType.id}`}
          orderType={orderType}
          checked={blacklistOrderTypes.includes(orderType.id as string)}
          onChange={toggleOrderType}
        />
      ));
  }, [blacklistOrderTypes, orderTypes, sliceSize, toggleOrderType]);

  const allSelected = useMemo(() => {
    let _allSelected = true;
    orderTypes.forEach(orderType => {
      if (!blacklistOrderTypes.includes(orderType.id as string)) _allSelected = false;
    });
    return _allSelected;
  }, [orderTypes, blacklistOrderTypes]);

  const save = useCallback(() => {
    updateBlacklist(user!, blacklistOrderTypes)
      .then(results => {
        onClose(true);
      })
      .catch(err => {
        enqueueSnackbar('Errore di salvataggio', { variant: 'error' });
        console.error(err);
      });
  }, [
    blacklistOrderTypes,
    user,
    onClose,
    enqueueSnackbar,
    updateBlacklist,
  ]);

  const cancel = useCallback(() => {
    onClose();
  }, [onClose]);

  const selectAll = useCallback(() => {
    const _blacklistOrderTypes = orderTypes.map(orderType => orderType.id as string);
    setBlacklistOrderTypes(_blacklistOrderTypes);
  }, [orderTypes]);

  const deselectAll = useCallback(() => {
    setBlacklistOrderTypes([]);
  }, []);

  return (
    <Dialog open={open} onClose={() => onClose()} maxWidth="md" fullWidth>
      <DialogTitle>
        {user?.nome} {user?.cognome}
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

export default OrderBlacklistEditDialog;
