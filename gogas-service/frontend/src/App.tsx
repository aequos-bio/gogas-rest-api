import React from 'react';
import { Provider } from 'react-redux';
import { SnackbarProvider } from 'notistack';
import { ThemeProvider, createTheme } from '@material-ui/core/styles';
import { green as primary, amber as secondary } from '@material-ui/core/colors';
import './style/app.css';
import { Routes } from './pages/Routes';
import { store } from './store/store';

const theme = createTheme({
  palette: {
    primary,
    secondary,
  },
});

function App() {
  return (
    <ThemeProvider theme={theme}>
      <Provider store={store}>
        <SnackbarProvider>
          <Routes />
        </SnackbarProvider>
      </Provider>
    </ThemeProvider>
  );
}

export default App;
