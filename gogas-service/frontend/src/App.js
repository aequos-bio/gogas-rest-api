import React from "react";
import { Provider } from "react-redux";
import { SnackbarProvider } from "notistack";
import { ThemeProvider, createMuiTheme } from "@material-ui/core/styles";
import indigo from "@material-ui/core/colors/indigo";
import green from "@material-ui/core/colors/green";
// import lightBlue from '@material-ui/core/colors/lightBlue';
import Routes from "./pages/Routes";
import { Store } from "./store/store";
import "./style/app.scss";

const theme = createMuiTheme({
  palette: {
    primary: green,
    secondary: indigo
  },
  status: {
    danger: "yellow"
  }
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
