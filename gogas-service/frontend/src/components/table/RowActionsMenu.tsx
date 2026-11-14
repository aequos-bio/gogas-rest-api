import React, { useState, useCallback } from 'react';
import { IconButton, Menu, MenuItem, ListItemIcon, ListItemText } from '@material-ui/core';
import { MoreVert as MoreVertIcon } from '@material-ui/icons';

export interface RowAction {
  /** Stable key. */
  key: string;
  label: string;
  icon: React.ReactNode;
  onClick: () => void;
  disabled?: boolean;
}

interface Props {
  actions: RowAction[];
  /** Tooltip/aria label for the trigger button. */
  ariaLabel?: string;
}

/**
 * A compact three-dot (kebab) menu of row actions. Replaces inline icon
 * buttons to save horizontal space; each entry shows an icon + text label.
 * Stops click propagation so it works inside clickable table rows.
 */
const RowActionsMenu: React.FC<Props> = ({ actions, ariaLabel = 'Azioni' }) => {
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);
  const open = Boolean(anchorEl);

  const openMenu = useCallback((event: React.MouseEvent<HTMLElement>) => {
    event.stopPropagation();
    setAnchorEl(event.currentTarget);
  }, []);

  const closeMenu = useCallback(() => {
    setAnchorEl(null);
  }, []);

  const handle = useCallback(
    (action: RowAction) => (event: React.MouseEvent) => {
      event.stopPropagation();
      setAnchorEl(null);
      action.onClick();
    },
    [],
  );

  if (!actions.length) return null;

  return (
    <>
      <IconButton size="small" aria-label={ariaLabel} onClick={openMenu}>
        <MoreVertIcon fontSize="small" />
      </IconButton>
      <Menu anchorEl={anchorEl} open={open} onClose={closeMenu} keepMounted>
        {actions.map((action) => (
          <MenuItem key={action.key} onClick={handle(action)} disabled={action.disabled}>
            <ListItemIcon style={{ minWidth: 36 }}>{action.icon}</ListItemIcon>
            <ListItemText>{action.label}</ListItemText>
          </MenuItem>
        ))}
      </Menu>
    </>
  );
};

export default RowActionsMenu;
