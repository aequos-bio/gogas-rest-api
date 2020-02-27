import React from 'react';
import { ListItem, CircularProgress } from '@material-ui/core';

const LoadingListItem = () => {
  return (
    <ListItem disableGutters style={{ justifyContent: 'center' }}>
      <CircularProgress />
    </ListItem>
  );
};

export default LoadingListItem;
