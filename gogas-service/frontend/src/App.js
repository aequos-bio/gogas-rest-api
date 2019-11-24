import React from "react";
import Main from "./pages/Main";
import { Provider } from "react-redux";
import { Store } from "./store/store";

function App() {
  return (
    <Provider store={Store}>
      <Main />
    </Provider>
  );
}

export default App;
