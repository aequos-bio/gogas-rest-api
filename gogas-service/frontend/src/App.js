import React from "react";
import { Provider } from "react-redux";
import Main from "./pages/Main";
import { Store } from "./store/store";

function App() {
  return (
    <Provider store={Store}>
      <Main />
    </Provider>
  );
}

export default App;
