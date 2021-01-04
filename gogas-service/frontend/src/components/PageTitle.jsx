import React from 'react';
import { makeStyles } from '@material-ui/core/styles';
import { Typography } from '@material-ui/core';

const useStyles = makeStyles(theme => ({
  header: {
    display: 'flex',
    alignItems: 'center',
    marginBottom: theme.spacing(2),
  },
  title: {
    flex: '1 1',
  },
  buttons: {
    '&>*': {
      marginLeft: '5px',
    },
  },
}));

const PageTitle = ({ title, children }) => {
  const classes = useStyles();

  return (
    <div className={classes.header}>
      <Typography className={classes.title} component="h4" variant="h4">
        {title}
      </Typography>
      <div className={classes.buttons}>{children}</div>
    </div>
  );
};

export default PageTitle;
