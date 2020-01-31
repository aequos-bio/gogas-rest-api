import React, { useCallback } from 'react';

const Switch = ({ disabled, checked, label, onChange }) => {
  
  const makeid = useCallback((length) => {
    let text = "";
    const possible = "abcdefghijklmnopqrstuvwxyz";

    for (let i = 0; i < length; i++)
      text += possible.charAt(Math.floor(Math.random() * possible.length));

    return text;
  }, []);

  const name = makeid(8);

  return (
    <div className="custom-control custom-switch">
      <input type="checkbox" className="custom-control-input" id={name} disabled={disabled} onChange={onChange} checked={checked}/>
      <label className="custom-control-label" htmlFor={name}>{label}</label>
    </div>
  );
}
  
export default Switch;