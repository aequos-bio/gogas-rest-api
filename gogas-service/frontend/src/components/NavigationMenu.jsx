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
  ListItemText,
} from '@material-ui/core';
import {
  HomeSharp as HomeIcon,
  EventSharp as EventIcon,
  EuroSharp as EuroIcon,
  GroupSharp as GroupIcon,
  Settings as SettingsIcon,
  ViewListSharp as ListIcon,
  ExploreSharp as ExploreIcon,
  CodeSharp as CodeIcon,
  AccountBalanceSharp as AccountBalanceIcon,
  ReceiptSharp as BillIcon,
} from '@material-ui/icons';
import { useHistory } from 'react-router-dom';
import { useSelector } from 'react-redux';
import Jwt from 'jsonwebtoken';
import Logo from '../logo_aequos.png';

const drawerWidth = '280px';
const useStyles = makeStyles(theme => ({
  drawer: {},
  drawerHeader: {
    display: 'flex',
    alignItems: 'center',
    padding: theme.spacing(0, 1),
    ...theme.mixins.toolbar,
    justifyContent: 'flex-end',
  },
  menu: {
    flexGrow: 1,
  },
  menuContainer: {
    width: drawerWidth,
    padding: theme.spacing(1, 0),
    display: 'flex',
    flexDirection: 'column',
  },
  menuChapter: {
    paddingLeft: theme.spacing(2),
  },
  menuItemList: {
    padding: 0,
  },
  menuItem: {
    padding: theme.spacing(0.5, 2),
  },
  menuItemIcon: {
    minWidth: theme.spacing(6),
  },
  link: {
    display: 'flex',
    alignItems: 'center',
    margin: theme.spacing(1, 0),
    '& svg': {
      marginRight: theme.spacing(2),
    },
  },
  credits: {
    display: 'flex',
    flexDirection: 'column',
    padding: theme.spacing(1.5, 0),
  },
  logo: {
    display: 'flex',
    justifyContent: 'center',
    width: drawerWidth,
  },
  copyright: {
    width: drawerWidth,
    textAlign: 'center',
  },
}));

const icons = [
  <HomeIcon />,
  <EventIcon />,
  <EuroIcon />,
  <GroupIcon />,
  <SettingsIcon />,
  <ListIcon />,
  <ExploreIcon />,
  <CodeIcon />,
  <AccountBalanceIcon />,
  <BillIcon />,
];

const menuItems = [
  {
    items: [{ label: 'Home', url: '/', icon: 0 }],
  },
  {
    label: 'Contabilità [year]',
    items: [
      { label: 'Anni contabili', url: '/years', restrictions: ['A'], icon: 1 },
      { label: 'Causali', url: '/reasons', restrictions: ['A'], icon: 4 },
      {
        label: 'Codici contabili',
        url: '/accountingcodes',
        restrictions: ['A'],
        icon: 7,
      },
      {
        label: 'Movimenti del gas',
        url: '/gasmovements',
        restrictions: ['A'],
        icon: 2,
      },
      {
        label: 'Situazione utenti',
        url: '/useraccounting',
        restrictions: ['A'],
        icon: 2,
      },
      {
        label: 'Fatture',
        url: '/invoices',
        restrictions: ['A'],
        icon: 9,
      },
      {
        label: 'Contabilità del GAS',
        url: '/gasaccounting',
        restrictions: ['A'],
        icon: 8,
      },
    ],
  },
  {
    label: 'Gestione',
    items: [
      { label: 'Utenti', url: '/users', restrictions: ['A'], icon: 3 },
      {
        label: 'Tipi ordine',
        url: '/ordertypes',
        restrictions: ['A'],
        icon: 5,
      },
      { label: 'Referenti', url: '/managers', restrictions: ['A'], icon: 6 },
    ],
  },
  {
    label: 'Utente',
    items: [
      {
        label: 'Situazione contabile',
        url: `/useraccountingdetails?userId=:userId`,
        icon: 2,
      },
    ],
  },
];

const NavigationMenu = ({ open, onClose }) => {
  const classes = useStyles();
  const history = useHistory();
  const { authentication, accounting } = useSelector(state => state);

  const jwt = useMemo(() => {
    if (authentication.jwtToken) {
      const j = Jwt.decode(authentication.jwtToken);
      return j;
    }
    return null;
  }, [authentication]);

  const menuClick = useCallback(
    menu => {
      const url = menu.url.replace(':userId', jwt.id);
      history.push(url);
      onClose();
    },
    [history, jwt, onClose]
  );

  const menu = useMemo(() => {
    const mm = [];
    if (!jwt) return null;
    menuItems.forEach((menuChapter, i) => {
      const menus = menuChapter.items.filter(m => {
        if (!m.restrictions) {
          return true;
        }
        const matching = m.restrictions.filter(r => r === jwt.role);
        if (matching.length > 0) {
          return true;
        }
        return false;
      });
      if (menus && menus.length) {
        mm.push(
          <div key={`menuchapter-${i}`} className={classes.menuContainer}>
            {menuChapter.label ? (
              <Typography
                variant="overline"
                display="block"
                gutterBottom
                color="textSecondary"
                className={classes.menuChapter}
              >
                {menuChapter.label.replace('[year]', accounting.currentYear)}
              </Typography>
            ) : null}
            <List className={classes.menuItemList}>
              {menus.map((m, j) => (
                <ListItem
                  className={classes.menuItem}
                  button
                  key={`menu-${i}-${j}`}
                  onClick={() => menuClick(m)}
                >
                  <ListItemIcon className={classes.menuItemIcon}>
                    {icons[m.icon]}
                  </ListItemIcon>
                  <ListItemText>{m.label}</ListItemText>
                </ListItem>
              ))}
            </List>
          </div>,
          <Divider key={`divider-${i}`} />
        );
      }
    });
    return mm;
  }, [classes, jwt, menuClick, accounting]);

  return (
    <Drawer className={classes.drawer} open={open} onClose={onClose}>
      <div className={classes.menu}>{menu}</div>
      <div className={classes.credits}>
        <div className={classes.logo}>
          <Avatar src={Logo} />
        </div>
        <Typography
          className={classes.copyright}
          variant="overline"
          display="block"
          gutterBottom
          color="textSecondary"
        >
          Copyright 2019-2020 AEQUOS.BIO
        </Typography>
      </div>
    </Drawer>
  );
};

export default NavigationMenu;
