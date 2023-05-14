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
import { makeStyles } from '@material-ui/core/styles';
import { useCategoriesAPI } from './useCategoriesAPI';
import { Category } from './typed';

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

interface Props {
  open: boolean;
  orderTypeId?: string;
  onClose: () => void;
}

const CategoriesDialog: React.FC<Props> = ({ open, orderTypeId, onClose }) => {
  const classes = useStyles();
  const { categories, reload, createCategory, updateCategory, deleteCategory: deleteCat } = useCategoriesAPI(orderTypeId);
  const [selectedId, setSelectedId] = useState<string | undefined>(undefined);
  const [selectedDescription, setSelectedDescription] = useState('');
  const [mode, setMode] = useState<string | undefined>(undefined);

  useEffect(() => {
    if (open) {
      setMode(undefined);
      setSelectedId(undefined);
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
    createCategory(selectedDescription).then(() => {
      setMode(undefined);
      setSelectedId(undefined);
    })
  }, [selectedDescription]);

  const editCategory = useCallback((category) => {
    setSelectedId(category.id);
    setSelectedDescription(category.description);
    setMode('edit');
  }, []);

  const doEditCategory = useCallback(
    (category) => {
      updateCategory(category.id, selectedDescription).then(() => {
        setMode(undefined);
        setSelectedId(undefined);
      })
    }, [selectedDescription],
  );

  const deleteCategory = useCallback((category) => {
    setSelectedId(category.id);
    setMode('delete');
  }, []);

  const doDeleteCategory = useCallback(
    (category: Category) => {
      deleteCat(category).then(() => {
        setMode(undefined);
        setSelectedId(undefined);
      })
    }, []
  );

  const reset = useCallback(() => {
    setMode(undefined);
    setSelectedId(undefined);
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

export default CategoriesDialog;
