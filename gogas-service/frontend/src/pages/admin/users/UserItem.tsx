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
  Chip,
} from '@material-ui/core';
import { MoreVert as MoreVertIcon } from '@material-ui/icons';
import { makeStyles } from '@material-ui/core/styles';
import { Friend, User } from './types';

const useStyles = makeStyles(theme => ({
  fieldName: {
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
  secondLine: {
    display: 'flex',
    flexWrap: 'wrap',
    overflowX: 'hidden',
  },
}));

interface Props {
  user: User;
  friend?: Friend;
  sort: string;
  onEdit: (user: User) => void;
  onDelete: (user: User) => void;
  onPasswordReset: (user: User) => void;
  onEnable: (user: User) => void;
  onDisable: (user: User) => void;
}

const UserItem: React.FC<Props> = ({
  user,
  friend,
  sort,
  onEdit,
  onDelete,
  onPasswordReset,
  onEnable,
  onDisable,
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
    onEdit(user);
  }, [user, onEdit]);

  const handleDelete = useCallback(() => {
    setAnchorEl(null);
    onDelete(user);
  }, [user, onDelete]);

  const handleEnable = useCallback(() => {
    setAnchorEl(null);
    onEnable(user);
  }, [user, onEnable]);

  const handleDisable = useCallback(() => {
    setAnchorEl(null);
    onDisable(user);
  }, [user, onDisable]);

  const handlePasswordReset = useCallback(() => {
    setAnchorEl(null);
    onPasswordReset(user);
  }, [onPasswordReset, user]);

  const handleClose = useCallback(() => {
    setAnchorEl(null);
  }, []);

  const getField = (label: string, value: string) => {
    return (
      <div className={classes.fieldName}>
        <Typography
          className={classes.fieldName}
          component="span"
          variant="body2"
        >
          {label}:
        </Typography>
        <Typography
          className={classes.fieldValue}
          component="span"
          variant="body2"
          color="textPrimary"
        >
          {value}
        </Typography>
      </div>
    );
  };

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
              {getField('Username', user.username)}
              {getField('Email', user.email)}
              {user.ruolo === 'U' ? null : (
                <Chip size="small" label={user.ruololabel} color="secondary" />
              )}
            </>
          }
          secondaryTypographyProps={{
            component: 'div',
            className: classes.secondLine,
          }}
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
            {user.attivo ? null : (
              <MenuItem onClick={handleEnable}>Riattiva</MenuItem>
            )}
            {user.attivo ? (
              <MenuItem onClick={handleDisable}>Disattiva</MenuItem>
            ) : null}
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
