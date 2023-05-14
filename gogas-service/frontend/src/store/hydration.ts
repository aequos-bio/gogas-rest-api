import { RootState } from "./store";

export function loadState(): any {
  try {
    const serializedState = localStorage.getItem('state');
    if (!serializedState) return undefined;
    return JSON.parse(serializedState);
  } catch (err) {
    console.error('Error while trying to idrate the state', err);
    return undefined;
  }
};

export const saveState = (state: RootState) => {
  try {
    const serializedState = JSON.stringify(state);
    localStorage.setItem('state', serializedState);
  } catch (err) {
    console.error(err);
    // ignore write errors
  }
};
