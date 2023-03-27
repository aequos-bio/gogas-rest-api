import React, { useState, useEffect, useCallback } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  List,
  ListItem,
  ListItemText,
  ListItemSecondaryAction,
  IconButton,
  TextField,
} from '@material-ui/core';
import {
  AddSharp as AddIcon,
  DeleteSharp as DeleteIcon,
  EditSharp as EditIcon,
  CheckSharp as OkIcon,
  ClearSharp as CancelIcon,
} from '@material-ui/icons';
import { withSnackbar } from 'notistack';
import { orderBy } from 'lodash';
import { makeStyles } from '@material-ui/core/styles';
import {
  apiGetJson,
  apiPost,
  apiPut,
  apiDelete,
} from '../../../utils/axios_utils';

const useStyles = makeStyles((theme) => ({
  item: {
    borderBottom: '1px solid #c0c0c0',
  },
  content: {
    textAlign: 'right',
  },
  textedit: {
    marginLeft: theme.spacing(-2),
  },
  descriptiondelete: {
    color: 'red',
  },
}));

const CategoriesDialog = ({ open, orderTypeId, onClose, enqueueSnackbar }) => {
  const classes = useStyles();
  const [categories, setCategories] = useState([]);
  const [selectedId, setSelectedId] = useState();
  const [selectedDescription, setSelectedDescription] = useState('');
  const [mode, setMode] = useState();

  const reload = useCallback(() => {
    if (orderTypeId) {
      apiGetJson(`/api/category/list/${orderTypeId}`, {}).then((list) => {
        setCategories(orderBy(list, 'description'));
      });
    }
  }, [orderTypeId]);

  useEffect(() => {
    if (open) {
      setMode();
      setSelectedId();
      setSelectedDescription('');
      reload();
    }
  }, [open, reload]);

  const addCategory = useCallback(() => {
    setMode('new');
    setSelectedId('0');
    setSelectedDescription('');
  }, []);

  const doAddCategory = useCallback(() => {
    apiPost(`/api/category/${orderTypeId}`, selectedDescription)
      .then(() => {
        setMode();
        setSelectedId();
        enqueueSnackbar('Nuova categoria creata', { variant: 'success' });
        reload();
      })
      .catch((err) => {
        enqueueSnackbar(
          err.response?.statusText || 'Errore nella creazione della categoria',
          { variant: 'error' },
        );
      });
  }, [enqueueSnackbar, selectedDescription, reload, orderTypeId]);

  const editCategory = useCallback((category) => {
    setSelectedId(category.id);
    setSelectedDescription(category.description);
    setMode('edit');
  }, []);

  const doEditCategory = useCallback(
    (category) => {
      apiPut(`/api/category/${orderTypeId}`, {
        id: category.id,
        description: selectedDescription,
      })
        .then(() => {
          setMode();
          setSelectedId();
          enqueueSnackbar('Categoria modificata', { variant: 'success' });
          reload();
        })
        .catch((err) => {
          enqueueSnackbar(
            err.response?.statusText || 'Errore nella modifica della categoria',
            { variant: 'error' },
          );
        });
    },
    [enqueueSnackbar, orderTypeId, selectedDescription, reload],
  );

  const deleteCategory = useCallback((category) => {
    setSelectedId(category.id);
    setMode('delete');
  }, []);

  const doDeleteCategory = useCallback(
    (category) => {
      apiDelete(`/api/category/${category.id}`)
        .then(() => {
          reload();
          setMode();
          setSelectedId();
          enqueueSnackbar('Categora eliminata', { variant: 'success' });
        })
        .catch((err) => {
          enqueueSnackbar(
            err.response?.statusText ||
              "Errore nell'eliminazione della categoria",
            { variant: 'error' },
          );
        });
    },
    [enqueueSnackbar, reload],
  );

  const reset = useCallback(() => {
    setMode();
    setSelectedId();
    setSelectedDescription('');
  }, []);

  const descriptionField = useCallback(
    (category) => {
      if ((mode === 'edit' || mode === 'new') && category.id === selectedId) {
        return (
          <TextField
            label={mode === 'new' ? 'Nuova categoria' : 'Modifica categoria'}
            className={classes.textedit}
            value={selectedDescription}
            variant='outlined'
            size='small'
            InputLabelProps={{
              shrink: true,
            }}
            onChange={(evt) => {
              setSelectedDescription(evt.target.value);
            }}
            autoFocus
          />
        );
      }

      if (mode === 'delete' && category.id === selectedId) {
        return (
          <span className={classes.descriptiondelete}>
            {category.description}
          </span>
        );
      }

      return <span>{category.description}</span>;
    },
    [mode, classes, selectedDescription, selectedId],
  );

  const buttons = useCallback(
    (category) => {
      if (mode === 'new') {
        if (category.id === selectedId) {
          return (
            <>
              <IconButton onClick={() => doAddCategory()}>
                <OkIcon fontSize='small' />
              </IconButton>
              <IconButton onClick={reset}>
                <CancelIcon fontSize='small' />
              </IconButton>
            </>
          );
        }
        return null;
      }

      if (mode === 'edit') {
        if (category.id === selectedId) {
          return (
            <>
              <IconButton onClick={() => doEditCategory(category)}>
                <OkIcon fontSize='small' />
              </IconButton>
              <IconButton onClick={reset}>
                <CancelIcon fontSize='small' />
              </IconButton>
            </>
          );
        }
        return null;
      }

      if (mode === 'delete') {
        if (category.id === selectedId) {
          return (
            <>
              <IconButton onClick={() => doDeleteCategory(category)}>
                <OkIcon fontSize='small' />
              </IconButton>
              <IconButton onClick={reset}>
                <CancelIcon fontSize='small' />
              </IconButton>
            </>
          );
        }
        return null;
      }

      return (
        <>
          <IconButton onClick={() => editCategory(category)}>
            <EditIcon fontSize='small' />
          </IconButton>
          <IconButton onClick={() => deleteCategory(category)}>
            <DeleteIcon fontSize='small' />
          </IconButton>
        </>
      );
    },
    [
      mode,
      selectedId,
      editCategory,
      deleteCategory,
      doAddCategory,
      doEditCategory,
      doDeleteCategory,
      reset,
    ],
  );

  const items = categories.map((c) => (
    <ListItem className={classes.item} key={`category-${c.id}`}>
      <ListItemText>{descriptionField(c)}</ListItemText>
      <ListItemSecondaryAction>{buttons(c)}</ListItemSecondaryAction>
    </ListItem>
  ));

  const newitem =
    mode === 'new' ? (
      <ListItem className={classes.item}>
        <ListItemText>
          {descriptionField({
            id: selectedId,
            description: selectedDescription,
          })}
        </ListItemText>

        <ListItemSecondaryAction>
          {buttons({ id: selectedId, description: selectedDescription })}
        </ListItemSecondaryAction>
      </ListItem>
    ) : null;

  return (
    <Dialog open={open} onClose={() => onClose()} maxWidth='xs' fullWidth>
      <DialogTitle>Categorie</DialogTitle>

      <DialogContent className={classes.content}>
        {!mode ? (
          <Button
            className={classes.button}
            startIcon={<AddIcon />}
            onClick={addCategory}
          >
            Nuova categoria
          </Button>
        ) : null}

        <List dense>
          {newitem}
          {items}
        </List>
      </DialogContent>

      <DialogActions>
        <Button onClick={() => onClose()}>Chiudi</Button>
      </DialogActions>
    </Dialog>
  );
};

export default withSnackbar(CategoriesDialog);
