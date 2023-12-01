import { Box, Button, Card, CardContent, CardHeader, Checkbox, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle, Divider, Grid, List, ListItem, ListItemIcon, ListItemSecondaryAction, ListItemText } from "@material-ui/core";
import { OrderType, _GenericManager } from "./typed";
import { useOrderTypeManagersAPI } from "./useOrderTypeManagersAPI";
import { useCallback, useEffect, useState } from "react";
import { CallMissedSharp } from "@material-ui/icons";
import { makeStyles } from "@material-ui/styles";

const useStyles = makeStyles(theme => ({
  list: {
    fontSize: '.875rem'
  },
  columnTitle: {
    fontSize: '1rem',
    fontWeight: 'bold',
    lineHeight: 2.5,
    display: 'flex',
    flexDirection: 'row',
    '& > :first-child': {
      flex: '1 1'
    }
  }
}));

interface Props {
  open: boolean;
  orderType?: OrderType;
  onClose: () => void;
}

const ManagersDialog: React.FC<Props> = ({ open, orderType, onClose }) => {
  const classes = useStyles();
  const {
    managers,
    availableManagers,
    loadingManagers,
    loadingAvailableManagers,
    reload,
    addManager,
    removeManager,
    addManagers,
    removeManagers
  } = useOrderTypeManagersAPI(orderType?.id);

  useEffect(() => {
    if (!orderType || !open) return;
    reload();
  }, [orderType, open])

  const removeAll = useCallback(() => {
    const ids = managers.map(manager => manager.id);
    removeManagers(ids);
  }, [managers]);

  const addAll = useCallback(() => {
    const ids = availableManagers.map(user => user.id);
    addManagers(ids);
  }, [availableManagers]);

  return (
    <Dialog open={open} onClose={() => onClose()} maxWidth='md' fullWidth>
      <DialogTitle>{orderType?.descrizione} - Referenti</DialogTitle>

      <DialogContent>
        <Grid container spacing={2}>
          <Grid item xs={6} className={classes.list}>
            <Box className={classes.columnTitle}>
              <div>
                Referenti
              </div>
              <Button onClick={removeAll}>Rimuovi tutti</Button>
            </Box>
            <Divider />
            {loadingManagers ?
              <CircularProgress />
              :
              <TransferList items={managers} onRemove={(item) => { removeManager(item) }} />
            }
          </Grid>

          <Grid item xs={6} className={classes.list}>
            <Box className={classes.columnTitle}>
              <div>
                Disponibili
              </div>
              <Button onClick={addAll}>Aggiungi tutti</Button>
            </Box>
            <Divider />
            {loadingAvailableManagers ?
              <CircularProgress />
              :
              <TransferList items={availableManagers} onAdd={(item) => { addManager(item) }} />
            }
          </Grid>
        </Grid>
      </DialogContent>

      <DialogActions>
        <Button onClick={() => onClose()}>Chiudi</Button>
      </DialogActions>
    </Dialog>

  )
}

interface TransferListProps {
  items: _GenericManager[];
  onAdd?: (item: _GenericManager) => void;
  onRemove?: (item: _GenericManager) => void;
}

const TransferList: React.FC<TransferListProps> = ({ items, onAdd, onRemove }) => {
  return (
    <List>
      {items.map(item => (
        <TransferListItem key={item.id} item={item} onAdd={onAdd ? () => onAdd(item) : undefined} onRemove={onRemove ? () => onRemove(item) : undefined} />
      ))}
    </List>

  )
}

interface TransferListItemProps {
  item: _GenericManager;
  onAdd?: () => void;
  onRemove?: () => void;
}
const TransferListItem: React.FC<TransferListItemProps> = ({ item, onAdd, onRemove }) => {
  const [hover, setHover] = useState(false);

  return (
    <ListItem button onMouseEnter={() => setHover(true)} onMouseLeave={() => setHover(false)}>
      <ListItemText primary={item.description} />
      <ListItemSecondaryAction>
        {onAdd && hover ? <Button onMouseEnter={() => setHover(true)} onMouseLeave={() => setHover(false)} onClick={() => onAdd()}>Aggiungi</Button> : null}
        {onRemove && hover ? <Button onMouseEnter={() => setHover(true)} onMouseLeave={() => setHover(false)} onClick={() => onRemove()}>Rimuovi</Button> : null}
      </ListItemSecondaryAction>
    </ListItem>

  )
}

export default ManagersDialog;
