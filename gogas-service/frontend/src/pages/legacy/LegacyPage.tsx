import React from 'react';
import { Container } from '@material-ui/core';
import { makeStyles } from '@material-ui/core/styles';
import queryString from 'query-string';

const useStyles = makeStyles(() => ({
  appContainer: {
    display: "flex",
    height: "calc(100vh - 140px)",
    width: "100%"
  },
  appFrame: {
    flexGrow: 1,
    margin: "0",
    padding: "0",
    border: "none"
  }
}));

interface Props {
  page: string;
}

const LegacyPage: React.FC<Props> = ({ page }) => {
  const classes = useStyles();

  return (
    <Container maxWidth={false}>
      <div className={classes.appContainer}>
        <iframe className={classes.appFrame} src={'/legacy-ui/' + page}></iframe>
      </div>
    </Container>

  )
}

export const LegacyOrderManagerPage: React.FC = () => {
  return (<LegacyPage page='orders-list' />)
};

export const LegacyProductsManagerPage: React.FC = () => {
  return (<LegacyPage page='products' />)
};

export const LegacyOrdersReportPage: React.FC = () => {
  return (<LegacyPage page='orders-report' />)
};

export const LegacySuppliersManagerPage: React.FC = () => {
  return (<LegacyPage page='suppliers' />)
};

export const LegacyConfigurationPage: React.FC = () => {
  return (<LegacyPage page='configuration' />)
};

export const LegacyUserOrderPage: React.FC = () => {
  return (<LegacyPage page='user-orders-list' />)
};

export const LegacyUserOrderDetailsPage: React.FC = () => {
  const search = queryString.parse(location.search);
  return (<LegacyPage page={'user-order-details?orderId=' + search.orderId + '&userId=' + search.userId} />);
}

export const LegacyFriendOrderDetailsPage: React.FC = () => {
  const search = queryString.parse(location.search);
  return (<LegacyPage page={'friend-order-details?orderId=' + search.orderId + '&userId=' + search.userId} />);
}

export const LegacyManageFriendsPage: React.FC = () => {
  return (<LegacyPage page='manage-friends' />)
}
