import React, { useCallback } from 'react';

function Switch(props) {
  
  const makeid = useCallback((length) => {
    var text = "";
    var possible = "abcdefghijklmnopqrstuvwxyz";

    for (var i = 0; i < length; i++)
      text += possible.charAt(Math.floor(Math.random() * possible.length));

    return text;
  }, []);

  let name = makeid(8);

  return (
    <div className="custom-control custom-switch">
      <input type="checkbox" className="custom-control-input" id={name} disabled={props.disabled} onChange={props.onChange} checked={props.checked}/>
      <label className="custom-control-label" htmlFor={name}>{props.label}</label>
    </div>
  );
}
  
export default Switch;