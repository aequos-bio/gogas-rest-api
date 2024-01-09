import React, { useCallback } from 'react';
import { makeStyles } from '@material-ui/core/styles';
import {
  Typography,
  Drawer,
  Avatar,
} from '@material-ui/core';
import { useHistory } from 'react-router-dom';
import Logo from '../../components/Logo';
import LogoAequos from '../../assets/logo_aequos.png';
import useJwt from '../../hooks/JwtHooks';
import { menuItems } from './menuConfiguration';
import MenuChapter from './MenuChapter';

const drawerWidth = '280px';
const useStyles = makeStyles((theme) => ({
  drawer: {},
  logoHeader: {
     margin: '10px',
  },
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
    padding: '10px',
  },
  copyright: {
    width: drawerWidth,
    textAlign: 'center',
  },
}));

interface Props {
  open: boolean;
  onClose: () => void;
}

const NavigationMenu: React.FC<Props> = ({ open, onClose }) => {
  const classes = useStyles();
  const history = useHistory();
  const jwt = useJwt();

  const menuClick = useCallback((menu) => {
    if (!jwt) return;
    const url = menu.url.replace(':userId', jwt.id);
    if (menu.newWindow) {
      window.open(url, '_blank');
    } else {
      history.push(url);
    }
    onClose();
  }, [history, jwt, onClose],
  );

  return (
    <Drawer className={classes.drawer} open={open} onClose={onClose}>
      <div className={classes.logo}><Logo height="80px" /></div>
      <Divider />
      <div className={classes.menu}>
        {jwt ? (
          menuItems.map((menuChapter, i) => (
            <div key={`manuchapter-${i}`} >
              <MenuChapter chapter={menuChapter} onMenuClick={menuClick} />
            </div>
          ))
        ) : <></>
        }
      </div>
      <div className={classes.credits}>
        <div className={classes.logo}>
          <Avatar src={LogoAequos} />
        </div>
        <Typography
          className={classes.copyright}
          variant='overline'
          display='block'
          gutterBottom
          color='textSecondary'
        >
          Copyright 2019-2024 AEQUOS.BIO
        </Typography>
      </div>
    </Drawer>
  );
};

export default NavigationMenu;
