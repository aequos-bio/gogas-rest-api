import React, { useMemo, useState, useCallback } from 'react';
import {
  ListItem,
  ListItemAvatar,
  ListItemText,
  ListItemSecondaryAction,
  IconButton,
  Avatar,
  Typography,
  Divider,
  Menu,
  MenuItem,
} from '@material-ui/core';
import { MoreVert as MoreVertIcon } from '@material-ui/icons';
import { makeStyles } from '@material-ui/core/styles';

const useStyles = makeStyles(theme => ({
  field: {
    marginRight: theme.spacing(1),
  },
  fieldValue: {
    marginRight: theme.spacing(2),
  },
  active: {
    color: theme.palette.getContrastText(theme.palette.primary.main),
    backgroundColor: theme.palette.primary.main,
  },
  inactive: {
    color: theme.palette.getContrastText(theme.palette.error.main),
    backgroundColor: theme.palette.error.main,
  },
  inactiveLabel: {
    fontSize: '80%',
    marginLeft: theme.spacing(2),
    padding: theme.spacing(0, 0.5),
    borderRadius: '3px',
    color: theme.palette.getContrastText(theme.palette.error.main),
    backgroundColor: theme.palette.error.main,
  },
}));

const UserItem = ({
  user,
  friend,
  sort,
  onEdit,
  onDelete,
  onPasswordReset,
}) => {
  const classes = useStyles();
  const [anchorEl, setAnchorEl] = useState(null);
  const open = Boolean(anchorEl);

  const initials = useMemo(() => {
    const n = user.nome.substring(0, 1).toUpperCase();
    const c = user.cognome.substring(0, 1).toUpperCase();
    return sort === 'NC' ? `${n}${c}` : `${c}${n}`;
  }, [user, sort]);

  const handleClick = useCallback(event => {
    setAnchorEl(event.currentTarget);
  }, []);

  const handleEdit = useCallback(() => {
    setAnchorEl(null);
    onEdit(user.idUtente);
  }, [user, onEdit]);

  const handleDelete = useCallback(() => {
    setAnchorEl(null);
    onDelete(user.idUtente);
  }, [user, onDelete]);

  const handlePasswordReset = useCallback(() => {
    setAnchorEl(null);
    onPasswordReset(user.idUtente);
  }, [onPasswordReset, user.idUtente]);

  const handleClose = useCallback(() => {
    setAnchorEl(null);
  }, []);

  return (
    <>
      <ListItem alignItems="flex-start" disableGutters>
        <ListItemAvatar>
          <Avatar className={user.attivo ? classes.active : classes.inactive}>
            {initials}
          </Avatar>
        </ListItemAvatar>
        <ListItemText
          primary={
            <>
              <span>
                {sort === 'NC'
                  ? `${user.nome} ${user.cognome}`
                  : `${user.cognome} ${user.nome}`}
              </span>
              <span style={{ fontSize: '80%' }}>
                {friend ? ` (amico di ${friend})` : ''}
              </span>
              {user.attivo ? null : (
                <span className={classes.inactiveLabel}>INATTIVO</span>
              )}
            </>
          }
          secondary={
            <>
              <Typography
                className={classes.field}
                component="span"
                variant="body2"
              >
                Username:
              </Typography>
              <Typography
                className={classes.fieldValue}
                component="span"
                variant="body2"
                color="textPrimary"
              >
                {user.username}
              </Typography>

              <Typography
                className={classes.field}
                component="span"
                variant="body2"
              >
                email:
              </Typography>
              <Typography
                className={classes.fieldValue}
                component="span"
                variant="body2"
                color="textPrimary"
              >
                {user.email}
              </Typography>

              <Typography
                className={classes.field}
                component="span"
                variant="body2"
              >
                Ruolo
              </Typography>
              <Typography
                className={classes.fieldValue}
                component="span"
                variant="body2"
                color="textPrimary"
              >
                {user.ruololabel}
              </Typography>
            </>
          }
        />
        <ListItemSecondaryAction>
          <IconButton edge="end" onClick={handleClick}>
            <MoreVertIcon />
          </IconButton>
          <Menu
            anchorEl={anchorEl}
            keepMounted
            open={open}
            onClose={handleClose}
            PaperProps={{
              style: {
                width: 200,
              },
            }}
          >
            <MenuItem onClick={handleEdit}>Modifica</MenuItem>
            <MenuItem onClick={handleDelete}>Elimina</MenuItem>
            <MenuItem onClick={handlePasswordReset}>Reset password</MenuItem>
          </Menu>
        </ListItemSecondaryAction>
      </ListItem>

      <Divider variant="middle" component="li" />
    </>
  );
};

export default UserItem;
