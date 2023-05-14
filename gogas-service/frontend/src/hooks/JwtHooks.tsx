import { useMemo } from 'react';
import { useSelector } from 'react-redux';
import jwt_decode from 'jwt-decode';
import moment from 'moment-timezone';
import { RootState } from '../store/store';
import { JwtToken } from '../store/types';

const useJwt = () => {
  const authentication = useSelector((state: RootState) => state.authentication);

  const decodedJwt = useMemo(() => {
    if (authentication.jwtToken) {
      const j = jwt_decode<JwtToken>(authentication.jwtToken);
      if (moment(j.exp * 1000).isBefore(moment())) { // expired
        return null;
      }
      return j;
    }
    return null;
  }, [authentication]);

  return decodedJwt;
};

export default useJwt;
