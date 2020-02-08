/* eslint-disable react/no-array-index-key */
import React, { useMemo, useCallback } from 'react';
import { makeStyles } from '@material-ui/core/styles';
import {
  Typography,
  Drawer,
  Divider,
  Avatar,
  List,
  ListItem,
  ListItemIcon,
  ListItemText
} from '@material-ui/core';
import {
  HomeSharp as HomeIcon,
  EventSharp as EventIcon,
  EuroSharp as EuroIcon,
  GroupSharp as GroupIcon
} from '@material-ui/icons';
import { useHistory} from 'react-router-dom';
import { connect } from 'react-redux';
import Jwt from 'jsonwebtoken';
import Logo from '../logo_aequos.png';

const drawerWidth = '280px';
const useStyles = makeStyles(theme => ({
  drawer: {
  },
  drawerHeader: {
    display: 'flex',
    alignItems: 'center',
    padding: theme.spacing(0, 1),
    ...theme.mixins.toolbar,
    justifyContent: 'flex-end',
  },
  menuContainer: {
    width: drawerWidth,
    padding: theme.spacing(2,0),
    display: 'flex',
    flexDirection: 'column'
  },
  menuChapter: {
    paddingLeft: theme.spacing(2),
  },
  link: {
    display: 'flex',
    alignItems: 'center',
    margin: theme.spacing(1,0),
    '& svg': {
      marginRight: theme.spacing(2)
    }
  },
  logo: {
    display: 'flex',
    justifyContent: 'center',
    position: 'fixed',
    bottom: theme.spacing(6),
    width: drawerWidth,
  },
  copyright: {
    position: 'fixed',
    bottom: theme.spacing(1),
    width: drawerWidth,
    textAlign: 'center'
  }
}));

const menuItems = [
  {
    items: [
      {label: 'Home', url:'/', icon:0}
    ]
  },
  {
    label: 'Contabilità',
    items: [
      {label: 'Anni contabili', url:'/years', restrictions:['A'], icon:1},
      {label: 'Situazione utenti', url:'/useraccounting', restrictions:['A'], icon:2},
    ]
  },
  {
    label: 'Gestione',
    items: [
      {label:'Utenti', url:'/users', restrictions:['A'], icon:3}
    ]
  },
  {
    label: 'Utente',
    items: [
      {label:'Situazione contabile', url:`/useraccountingdetails?userId=:userId`, icon:2}
    ]
  }
]

const icons = [
  <HomeIcon/>,
  <EventIcon/>,
  <EuroIcon/>,
  <GroupIcon/>
]

const NavigationMenu = ({authentication, open, onClose}) => {
  const classes = useStyles();
  const history = useHistory();

  const jwt = useMemo(() => {
    if (authentication.jwtToken) {
      const j = Jwt.decode(authentication.jwtToken);
      return j;
    }
    return null;
  }, [authentication]);

  const menuClick = useCallback((menu) => {
    const url = menu.url.replace(':userId', jwt.id);
    history.push(url);
    onClose();
  }, [history, jwt, onClose]);

  const menu = useMemo(() => {
    const mm = [];
    if (!jwt) return null;
    menuItems.forEach((menuChapter,i) => {
      const menus = menuChapter.items.filter(m => {
        if (!m.restrictions) {
          return true;
        }
        const matching = m.restrictions.filter(r => r === jwt.role)
        if (matching.length>0) {
          return true;
        }
        return false;
      });
      if (menus && menus.length) {
        mm.push(
          <div key={`menuchapter-${i}`} className={classes.menuContainer}>
            {menuChapter.label ? 
              <Typography variant="overline" display="block" gutterBottom color='textSecondary' className={classes.menuChapter}>
                {menuChapter.label}
              </Typography>
            : null}
            <List>
              {menus.map((m,j)=> (
                <ListItem button key={`menu-${i}-${j}`} onClick={() => menuClick(m)}> 
                  <ListItemIcon>{icons[m.icon]}</ListItemIcon>
                  <ListItemText>{m.label}</ListItemText>
                </ListItem>
              ))}
            </List>
          </div>,  
          <Divider key={`divider-${i}`}/>
      );
      }
    });
    return mm;
  }, [classes, jwt, menuClick]);

  return (
    <Drawer className={classes.drawer} open={open} onClose={onClose}>
      {menu}

      <div className={classes.logo}>
        <Avatar src={Logo} />  
      </div>
      <Typography className={classes.copyright} variant="overline" display="block" gutterBottom color='textSecondary'>
        Copyright 2019-2020 AEQUOS.BIO
      </Typography>
  </Drawer>

  );
}

const mapStateToProps = state => {
  return {
    info: state.info,
    authentication: state.authentication,
  };
};

const mapDispatchToProps = {
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(NavigationMenu);
