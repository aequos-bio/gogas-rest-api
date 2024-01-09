import React, { useCallback, useState } from 'react';
import DefaultLogo from '../assets/logo_aequos.png';

interface Props {
  height: string;
}

const Logo: React.FC<Props> = ({ height }) => {

  const onError = (e: React.SyntheticEvent<HTMLImageElement, Event>) => {
    if (!event || !event.currentTarget) {
      return;
    }

    let target = (event.target as HTMLImageElement);
    target.src = DefaultLogo;
  };

  return (
    <img src='/info/logo' onError={onError} height={height} />
  );
};

export default Logo;
