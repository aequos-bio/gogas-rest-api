import React from 'react';
import { Provider } from 'react-redux';
import { SnackbarProvider } from 'notistack';
import { ThemeProvider, createTheme } from '@material-ui/core/styles';
import { green as primary, amber as secondary } from '@material-ui/core/colors';
import Routes from './pages/Routes';
import { Store } from './store/store';
import './style/app.css';

const theme = createTheme({
  palette: {
    primary,
    secondary,
  },
  status: {
    danger: 'yellow',
  },
});

function App() {
  return (
    <ThemeProvider theme={theme}>
      <Provider store={Store}>
        <SnackbarProvider>
          <Routes />
        </SnackbarProvider>
      </Provider>
    </ThemeProvider>
  );
}

export default App;
