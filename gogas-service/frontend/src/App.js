import React from "react";
import { Provider } from "react-redux";
import Routes from "./pages/Routes";
import { Store } from "./store/store";

function App() {
  return (
    <Provider store={Store}>
      <Routes />
    </Provider>
  );
}

export default App;
