import React, { useState, useCallback } from 'react';
import {
  IconButton,
  TableRow,
  TableCell,
  Menu,
  MenuItem,
} from '@material-ui/core';
import { green } from '@material-ui/core/colors';
import {
  CheckSharp as CheckIcon,
  MoreVert as MoreVertIcon,
} from '@material-ui/icons';
import { makeStyles } from '@material-ui/core/styles';
import { OrderType } from './typed';

const useStyles = makeStyles(theme => ({
  tableCell: {
    paddingLeft: theme.spacing(1),
    paddingRight: theme.spacing(1),
  },
  label: {
    marginLeft: theme.spacing(2),
    padding: theme.spacing(0.25, 1),
    fontSize: '80%',
    borderRadius: '8px',
    backgroundColor: green[400],
    color: 'white',
  },
  tdFlag: {
    textAlign: 'center',
    maxWidth: '60px',
    width: '60px',
    color: theme.palette.grey[700],
  },
}));

interface Props {
  orderType: OrderType;
  onEdit: (id: string) => void;
  onDelete: (id: string) => void;
  onEditCategories: (id: string) => void;
  onEditManagers: (id: string) => void;
}

const OrderTypeItem: React.FC<Props> = ({
  orderType,
  onEdit,
  onDelete,
  onEditCategories,
  onEditManagers,
}) => {
  const classes = useStyles();
  const [anchorEl, setAnchorEl] = useState(null);
  const open = Boolean(anchorEl);

  const handleOpenMenu = useCallback(event => {
    setAnchorEl(event.currentTarget);
  }, []);

  const handleCloseMenu = useCallback(() => {
    setAnchorEl(null);
  }, []);

  return (
    <TableRow key={`ordertype-${orderType.id}`} hover>
      <TableCell className={classes.tableCell}>
        <span>{orderType.descrizione}</span>
        {orderType.idordineaequos !== undefined &&
          orderType.idordineaequos !== null ? (
          <span className={classes.label}>AEQUOS</span>
        ) : null}
      </TableCell>
      <TableCell className={`${classes.tableCell} ${classes.tdFlag}`}>
        {orderType.riepilogo ? <CheckIcon /> : null}
      </TableCell>
      <TableCell className={`${classes.tableCell} ${classes.tdFlag}`}>
        {orderType.totalecalcolato ? <CheckIcon /> : null}
      </TableCell>
      <TableCell className={`${classes.tableCell} ${classes.tdFlag}`}>
        {orderType.turni ? <CheckIcon /> : null}
      </TableCell>
      <TableCell className={`${classes.tableCell} ${classes.tdFlag}`}>
        {orderType.preventivo ? <CheckIcon /> : null}
      </TableCell>
      <TableCell className={`${classes.tableCell} ${classes.tdFlag}`}>
        {orderType.completamentocolli ? <CheckIcon /> : null}
      </TableCell>
      <TableCell className={`${classes.tableCell} ${classes.tdFlag}`}>
        {orderType.external ? <CheckIcon /> : null}
      </TableCell>
      <TableCell className={classes.tableCell}>
        <IconButton edge="end" onClick={handleOpenMenu}>
          <MoreVertIcon />
        </IconButton>
        <Menu
          anchorEl={anchorEl}
          keepMounted
          open={open}
          onClose={handleCloseMenu}
          PaperProps={{
            style: {
              width: 200,
            },
          }}
        >
          <MenuItem
            onClick={() => {
              handleCloseMenu();
              onEdit(orderType.id as string);
            }}
          >
            Modifica
          </MenuItem>

          <MenuItem
            onClick={() => {
              handleCloseMenu();
              onDelete(orderType.id as string);
            }}
          >
            Elimina
          </MenuItem>

          <MenuItem
            onClick={() => {
              handleCloseMenu();
              onEditCategories(orderType.id as string);
            }}
          >
            Categorie
          </MenuItem>

          <MenuItem
            onClick={() => {
              handleCloseMenu();
              onEditManagers(orderType.id as string);
            }}
          >
            Referenti
          </MenuItem>
        </Menu>
      </TableCell>
    </TableRow>
  );
};

export default OrderTypeItem;
