/* eslint-disable jsx-a11y/no-static-element-interactions */
/* eslint-disable jsx-a11y/click-events-have-key-events */
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
import { connect } from 'react-redux';
import Jwt from 'jsonwebtoken';
import { withSnackbar } from 'notistack';
import moment from 'moment-timezone';
import { apiGetJson } from '../utils/axios_utils';
import { logout } from '../store/actions';
import NavigationMenu from './NavigationMenu';

const useStyles = makeStyles(theme => ({
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

const NavBar = ({
  authentication,
  info,
  history,
  enqueueSnackbar,
  ...props
}) => {
  const classes = useStyles();
  const [anchorEl, setAnchorEl] = useState(null);
  const [menuOpen, setMenuOpen] = useState(false);
  const open = Boolean(anchorEl);
  const [balance, setBalance] = useState(0);

  const handleMenu = event => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const jwt = useMemo(() => {
    if (authentication.jwtToken) {
      const j = Jwt.decode(authentication.jwtToken);
      if (moment(j.exp * 1000).isBefore(moment())) {
        j.expired = true;
      }
      return j;
    }
    return null;
  }, [authentication]);

  useEffect(() => {
    if (!jwt || !jwt.id || jwt.expired) return;
    apiGetJson(`/api/accounting/user/${jwt.id}/balance`)
      .then(b => {
        if (b.error) {
          enqueueSnackbar(`Caricamento del saldo: ${b.errorMessage}`, {
            variant: 'error',
          });
        } else {
          setBalance(b);
        }
      })
      .catch(err => {
        enqueueSnackbar(
          `Caricamento del saldo: ${err.debugMessage || err.errorMessage}`,
          { variant: 'error' }
        );
      });
  }, [jwt, enqueueSnackbar]);

  const disconnect = useCallback(() => {
    props.logout();
    history.push('/login?disconnect');
  }, [props, history]);

  const openBalanceDetail = useCallback(() => {
    history.push(`/userAccountingDetails?userId=${jwt.id}`);
  }, [history, jwt]);

  return (
    <>
      <AppBar className={classes.appbar} position="fixed">
        <Toolbar>
          <IconButton
            edge="start"
            className={classes.menuButton}
            color="inherit"
            aria-label="menu"
            onClick={() => setMenuOpen(true)}
          >
            <MenuIcon />
          </IconButton>
          <Typography variant="h5" className={classes.title}>
            {info['gas.nome'] ? info['gas.nome'].value : 'GoGas'}
          </Typography>
          {authentication && (
            <div className={classes.userbutton}>
              <IconButton
                aria-label="account of current user"
                aria-controls="menu-appbar"
                aria-haspopup="true"
                onClick={handleMenu}
                color="inherit"
              >
                <AccountCircleIcon />
              </IconButton>
              <div className={classes.username}>
                <span>{jwt ? `${jwt.firstname} ${jwt.lastname}` : ''}</span>
                <span
                  className={`${classes.balance} ${
                    balance && balance < 0 ? classes.balanceRed : null
                  }`}
                  onClick={openBalanceDetail}
                >
                  Saldo {balance || '0.00'} €
                </span>
              </div>

              <Menu
                id="menu-appbar"
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

const mapStateToProps = state => {
  return {
    info: state.info,
    authentication: state.authentication,
  };
};

const mapDispatchToProps = {
  logout,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(withSnackbar(NavBar));
