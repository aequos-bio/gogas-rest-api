import React from 'react';
import { useLocation } from 'react-router-dom';
import queryString from 'query-string';
import { Container } from '@material-ui/core';

export const LegacyOrderManagerPage: React.FC = () => {
  return render('orders-list');
};

export const LegacyProductsManagerPage: React.FC = () => {
  return render('products');
};

export const LegacySuppliersManagerPage: React.FC = () => {
  return render('suppliers');
};

export const LegacyConfigurationPage: React.FC = () => {
  return render('configuration');
};

function render(page : string) {
  return (
    <Container maxWidth={false}>
        <div className="App" style={{display: "flex", height: "750px", width: "100%"}}>
          <iframe src={'/legacy-ui/' + page} style={{flexGrow: "1", margin: "0", padding: "0", border: "none"}}></iframe>
        </div>
    </Container>
  );
};