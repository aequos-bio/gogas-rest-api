import React, { useState, useMemo, useCallback, useEffect } from 'react';
import { makeStyles } from '@material-ui/core/styles';
import {
  AppBar,
  Toolbar,
  Typography,
  IconButton,
  Menu,
  MenuItem,
} from '@material-ui/core';
import {
  MenuSharp as MenuIcon,
  AccountCircleSharp as AccountCircleIcon,
} from '@material-ui/icons';
import NavigationMenu from '../navigationmenu/NavigationMenu';
import useJwt from '../../hooks/JwtHooks';
import { logout } from '../../store/features/authentication.slice';
import { useHistory } from 'react-router-dom';
import { useUserBalanceAPI } from './useUserBalanceAPI';
import { useAppDispatch, useAppSelector } from '../../store/store';

const useStyles = makeStyles((theme) => ({
  appbar: {
    color: 'rgba(255,255,255,.87)',
  },
  gutter: {
    marginBottom: theme.spacing(2),
  },
  root: {
    flexGrow: 1,
  },
  menuButton: {
    marginRight: theme.spacing(2),
  },
  title: {
    flexGrow: 1,
  },
  userbutton: {
    display: 'flex',
    flexDirection: 'row',
  },
  username: {
    cursor: 'pointer',
    display: 'flex',
    flexDirection: 'column',
    justifyContent: 'center',
  },
  balance: {
    fontSize: '80%',
    color: theme.palette.secondary.main,
  },
  balanceRed: {
    color: theme.palette.warning.main,
  },
}));

const NavBar: React.FC = () => {
  const classes = useStyles();
  const history = useHistory();
  const [anchorEl, setAnchorEl] = useState(null);
  const [menuOpen, setMenuOpen] = useState(false);
  const open = Boolean(anchorEl);
  const { balance } = useUserBalanceAPI();
  const info = useAppSelector((state) => state.info);
  const authentication = useAppSelector((state) => state.authentication);
  const dispatch = useAppDispatch();
  const jwt = useJwt();

  const handleMenu = (event: any) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };


  const disconnect = useCallback(() => {
    dispatch(logout());
    history.push('/login?disconnect');
  }, [history, dispatch]);

  const openBalanceDetail = useCallback(() => {
    if (!jwt) return;
    history.push(`/userAccountingDetails?userId=${jwt.id}`);
  }, [history, jwt]);

  return (
    <>
      <AppBar className={classes.appbar} position='fixed'>
        <Toolbar>
          <IconButton
            edge='start'
            className={classes.menuButton}
            color='inherit'
            aria-label='menu'
            onClick={() => setMenuOpen(true)}
          >
            <MenuIcon />
          </IconButton>
          <Typography variant='h5' className={classes.title}>
            {info['gas.nome'] || 'GoGas'}
          </Typography>
          {authentication && (
            <div className={classes.userbutton}>
              <IconButton
                aria-label='account of current user'
                aria-controls='menu-appbar'
                aria-haspopup='true'
                onClick={handleMenu}
                color='inherit'
              >
                <AccountCircleIcon />
              </IconButton>
              <div className={classes.username}>
                <span>{jwt ? `${jwt.firstname} ${jwt.lastname}` : ''}</span>
                <span
                  className={`${classes.balance} ${balance && balance < 0 ? classes.balanceRed : null
                    }`}
                  onClick={openBalanceDetail}
                >
                  Saldo {balance || '0.00'} €
                </span>
              </div>

              <Menu
                id='menu-appbar'
                anchorEl={anchorEl}
                anchorOrigin={{
                  vertical: 'top',
                  horizontal: 'right',
                }}
                keepMounted
                transformOrigin={{
                  vertical: 'top',
                  horizontal: 'right',
                }}
                open={open}
                onClose={handleClose}
              >
                <MenuItem onClick={disconnect}>Disconnetti</MenuItem>
              </Menu>
            </div>
          )}
        </Toolbar>

        <NavigationMenu open={menuOpen} onClose={() => setMenuOpen(false)} />
      </AppBar>
      <Toolbar className={classes.gutter} />
    </>
  );
};

export default NavBar;
