/* eslint-disable jsx-a11y/no-static-element-interactions */
/* eslint-disable jsx-a11y/click-events-have-key-events */
import React, { useState, useMemo, useCallback } from 'react';
import { makeStyles } from '@material-ui/core/styles';
import {
  AppBar, 
  Toolbar, 
  Typography,
  IconButton,
  Menu,
  MenuItem
} from '@material-ui/core';
import {
  MenuSharp as MenuIcon,
  AccountCircleSharp as AccountCircleIcon,
} from '@material-ui/icons';
import { connect } from 'react-redux';
import Jwt from 'jsonwebtoken';
import { logout } from '../store/actions';
import NavigationMenu from './NavigationMenu';

const useStyles = makeStyles(theme => ({
  appbar: {
    color: 'rgba(255,255,255,.87)'
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
    cursor: 'pointer',
  },
  username: {
    cursor: 'pointer'
  },
}));

const NavBar = ({authentication, info, history, ...props}) => {
  const classes = useStyles();
  const [anchorEl, setAnchorEl] = useState(null);
  const [menuOpen, setMenuOpen] = useState(false);
  const open = Boolean(anchorEl);
  
  const handleMenu = event => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };
  
  const jwt = useMemo(() => {
    if (authentication.jwtToken) {
      const j = Jwt.decode(authentication.jwtToken);
      return j;
    }
    return null;
  }, [authentication]);

  const disconnect = useCallback(() => {
    props.logout(); 
    history.push('/login?disconnect');
  }, [props, history])

  return (
    <>
    <AppBar className={classes.appbar} position="fixed">
      <Toolbar>
        <IconButton edge="start" className={classes.menuButton} color="inherit" aria-label="menu" onClick={() => setMenuOpen(true)}>
          <MenuIcon />
        </IconButton>
        <Typography variant="h5" className={classes.title} onClick={() => {history.push('/')}}>
          {info['gas.nome'] ? info['gas.nome'].value : 'GoGas'}
        </Typography>
        {authentication && (
          <div>
            <IconButton
              aria-label="account of current user"
              aria-controls="menu-appbar"
              aria-haspopup="true"
              onClick={handleMenu}
              color="inherit"
            >
              <AccountCircleIcon />
            </IconButton>
            <span className={classes.username} onClick={handleMenu}>
              {jwt ? `${jwt.firstname} ${jwt.lastname}` : ''}
            </span>
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
    <Toolbar className={classes.gutter}/>  
    </>
  );
}

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
)(NavBar);
