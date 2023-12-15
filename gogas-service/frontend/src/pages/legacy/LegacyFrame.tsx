import React from 'react';
import { useLocation } from 'react-router-dom';
import queryString from 'query-string';

export const LegacyOrderManagerPage: React.FC = () => {
  return render('orders-list');
};

export const LegacyProductsManagerPage: React.FC = () => {
  return render('products');
};

function render(page : string) {
  return (
    <div className="App" style={{display: "flex", height: "750px", width: "100%"}}>
      <iframe src={'/legacy/' + page} style={{flexGrow: "1", margin: "0", padding: "0", border: "none"}}></iframe>
    </div>
  );
};